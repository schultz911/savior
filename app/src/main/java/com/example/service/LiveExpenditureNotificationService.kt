package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.SpendTrackerApplication
import com.example.data.ExpenseEntity
import com.example.data.ExpenseType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class LiveExpenditureNotificationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
        val initialNotification = buildNotification(
            monthTitle = "Spend Tracker",
            totalSpendText = "Tracking live expenditures...",
            breakdownText = "Monitoring SMS for debits, transfers & spends",
            progress = 0,
            showProgress = false
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        refreshExpenditure()
        return START_STICKY
    }

    private fun refreshExpenditure() {
        serviceScope.launch {
            try {
                val app = application as? SpendTrackerApplication ?: return@launch
                val dao = app.database.expenseDao()
                val prefs = app.preferences

                if (!prefs.isPersistentNotificationEnabled) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }

                val currentMonthKey = ExpenseEntity.formatMonthKey(System.currentTimeMillis())
                val monthDisplay = ExpenseEntity.formatMonthDisplay(currentMonthKey)
                val currentMonthExpenses = dao.getExpensesForMonthSync(currentMonthKey)

                val currency = prefs.currency
                val budget = prefs.monthlyBudget

                var totalSpend = 0.0
                var debitsSum = 0.0
                var transfersSum = 0.0
                var cardSpendsSum = 0.0

                for (exp in currentMonthExpenses) {
                    totalSpend += exp.amount
                    when (exp.type) {
                        ExpenseType.DEBIT -> debitsSum += exp.amount
                        ExpenseType.TRANSFER -> transfersSum += exp.amount
                        ExpenseType.SPEND -> cardSpendsSum += exp.amount
                    }
                }

                val totalFormatted = formatCurrency(totalSpend, currency)
                val debitsFormatted = formatCurrency(debitsSum, currency)
                val transfersFormatted = formatCurrency(transfersSum, currency)
                val spendsFormatted = formatCurrency(cardSpendsSum, currency)

                val title = "$monthDisplay Spend: $totalFormatted"
                val breakdown = "Debits: $debitsFormatted • Spends: $spendsFormatted • Transfers: $transfersFormatted"

                val progress = if (budget > 0) {
                    ((totalSpend / budget) * 100).toInt().coerceIn(0, 100)
                } else 0

                val budgetSubtext = if (budget > 0) {
                    val budgetFormatted = formatCurrency(budget, currency)
                    "$progress% of $budgetFormatted budget"
                } else null

                val notification = buildNotification(
                    monthTitle = title,
                    totalSpendText = breakdown,
                    breakdownText = budgetSubtext ?: "${currentMonthExpenses.size} transactions this month",
                    progress = progress,
                    showProgress = budget > 0
                )

                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun buildNotification(
        monthTitle: String,
        totalSpendText: String,
        breakdownText: String,
        progress: Int,
        showProgress: Boolean
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val syncIntent = Intent(this, LiveExpenditureNotificationService::class.java).apply {
            action = ACTION_SYNC
        }
        val pendingSyncIntent = PendingIntent.getService(
            this,
            1,
            syncIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val addExpenseIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_SHOW_MANUAL_ADD, true)
        }
        val pendingAddExpenseIntent = PendingIntent.getActivity(
            this,
            2,
            addExpenseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(monthTitle)
            .setContentText(totalSpendText)
            .setSubText("Live Spend")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(monthTitle)
                    .bigText("$totalSpendText\n$breakdownText")
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingOpenIntent)
            .addAction(
                android.R.drawable.ic_input_add,
                "+ Add Spend",
                pendingAddExpenseIntent
            )
            .addAction(
                android.R.drawable.ic_menu_rotate,
                "Refresh",
                pendingSyncIntent
            )
            .addAction(
                android.R.drawable.ic_menu_view,
                "Open App",
                pendingOpenIntent
            )

        if (showProgress) {
            builder.setProgress(100, progress, false)
        }

        return builder.build()
    }

    private fun formatCurrency(amount: Double, currency: String): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$currency${formatter.format(amount)}"
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "live_expenditure_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.action.START_TRACKER"
        const val ACTION_SYNC = "com.example.action.SYNC_EXPENDITURE"
        const val ACTION_STOP = "com.example.action.STOP_TRACKER"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Live Monthly Expenditure"
                val descriptionText = "Persistent status bar tracker for debits, transfers, and spends"
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    setShowBadge(false)
                }
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun updateLiveExpenditure(context: Context) {
            val intent = Intent(context, LiveExpenditureNotificationService::class.java).apply {
                action = ACTION_SYNC
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stopNotification(context: Context) {
            val intent = Intent(context, LiveExpenditureNotificationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
