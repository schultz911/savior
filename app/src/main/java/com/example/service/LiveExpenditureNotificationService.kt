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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.example.ui.PacingStatus
import com.example.engine.RecurringDetectionEngine

class LiveExpenditureNotificationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
        val initialNotification = buildNotification(
            monthTitle = "Spend Tracker",
            contentText = "Monitoring SMS for debits, transfers & spends",
            pacingStatus = PacingStatus.ON_TRACK,
            progress = 0
        )

        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, initialNotification)
            } catch (ne: Exception) {
                ne.printStackTrace()
            }
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
                val monthName = try {
                    val parser = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
                    val formatter = java.text.SimpleDateFormat("MMMM", java.util.Locale.US)
                    val date = parser.parse(currentMonthKey)
                    if (date != null) formatter.format(date) else currentMonthKey
                } catch (e: Exception) {
                    "Month"
                }
                val currentMonthExpenses = dao.getExpensesForMonthSync(currentMonthKey)

                val currency = prefs.currency
                val budget = prefs.monthlyBudget
                val blacklisted = prefs.getBlacklistedMerchants()

                var totalSpend = 0.0
                var transfersSum = 0.0
                var spendsSum = 0.0
                var creditCardsSum = 0.0
                var selfSum = 0.0

                for (exp in currentMonthExpenses) {
                    if (exp.isExcluded) {
                        continue // Excluded transactions are omitted from total spend calculations
                    }
                    val normMerchant = exp.merchantOrRecipient.trim().lowercase()
                    if (blacklisted.any { normMerchant.contains(it.lowercase()) || it.lowercase().contains(normMerchant) }) {
                        continue // Blacklisted merchants are simply ignored and not considered
                    }

                    val isSelf = exp.type == ExpenseType.SELF || exp.category.equals("Self", ignoreCase = true)
                    val isCreditCard = exp.type == ExpenseType.CREDIT_CARD || exp.category.equals("Credit Card Bill", ignoreCase = true)

                    // Net spend calculation: debit amount minus refunded / reversed amount
                    val netAmount = (exp.amount - exp.refundedAmount).coerceAtLeast(0.0)

                    if (isSelf) {
                        selfSum += netAmount
                    } else if (isCreditCard) {
                        creditCardsSum += netAmount
                    } else if (exp.isRefundOrReversal) {
                        spendsSum = (spendsSum - exp.amount).coerceAtLeast(0.0)
                        totalSpend = (totalSpend - exp.amount).coerceAtLeast(0.0)
                    } else if (exp.type == ExpenseType.P2P) {
                        transfersSum += netAmount
                        totalSpend += netAmount
                    } else {
                        spendsSum += netAmount
                        totalSpend += netAmount
                    }
                }

                val totalFormatted = formatCurrency(totalSpend, currency)
                val title = "$monthName:  $totalFormatted"

                val cal = java.util.Calendar.getInstance()
                val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                val currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
                val daysRemaining = (daysInMonth - currentDay + 1).coerceAtLeast(1)

                val allExpensesSync = dao.getAllExpensesSync()
                val ignoredMerchants = prefs.getIgnoredRecurringMerchants()
                val recurringCommitments = RecurringDetectionEngine.detectRecurringBills(allExpensesSync, currentMonthKey, ignoredMerchants)
                val upcomingRecurring = recurringCommitments.filter { !it.isPaidThisMonth }.sumOf { it.expectedAmount }
                val remainingDiscretionary = (budget - totalSpend - upcomingRecurring).coerceAtLeast(0.0)
                val safeDaily = if (budget > 0) remainingDiscretionary / daysRemaining else 0.0

                // Proactively alert user if any recurring bill is due in the next 48 hours
                SpendAlertManager.checkAndNotifyUpcomingBills(this@LiveExpenditureNotificationService, recurringCommitments, currency)

                val progress = if (budget > 0) {
                    ((totalSpend / budget) * 100).toInt().coerceIn(0, 100)
                } else 0

                val pacingStatus = when {
                    budget <= 0.0 -> PacingStatus.ON_TRACK
                    totalSpend > budget -> PacingStatus.OVER_PACED
                    safeDaily <= 0 || totalSpend > (budget / daysInMonth) * currentDay * 1.15 -> PacingStatus.CAUTION
                    else -> PacingStatus.ON_TRACK
                }

                val burnRateStatus = when (pacingStatus) {
                    PacingStatus.ON_TRACK -> "Safe"
                    PacingStatus.CAUTION -> "Over-paced"
                    PacingStatus.OVER_PACED -> "Excessive"
                }

                val safeDailyFormatted = formatCurrency(safeDaily, currency)
                val contentText = if (budget > 0) {
                    val budgetFormatted = formatCurrency(budget, currency)
                    "$progress% of $budgetFormatted • $burnRateStatus burn rate • Safe daily spend pace: $safeDailyFormatted/day"
                } else {
                    "No budget set • $burnRateStatus burn rate • Safe daily spend pace: $safeDailyFormatted/day"
                }

                val notification = buildNotification(
                    monthTitle = title,
                    contentText = contentText,
                    pacingStatus = pacingStatus,
                    progress = progress,
                    currency = currency
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
        contentText: String,
        pacingStatus: PacingStatus,
        progress: Int,
        currency: String = "₹"
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

        val addExpenseIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_ADD_SPEND
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_SHOW_MANUAL_ADD, true)
        }
        val pendingAddExpenseIntent = PendingIntent.getActivity(
            this,
            2,
            addExpenseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pacedLargeIcon = getPacedNotificationLargeIcon(this, pacingStatus, currency)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_rupee)
            .setLargeIcon(pacedLargeIcon)
            .setColor(0xFF059669.toInt())
            .setContentTitle(monthTitle)
            .setContentText(contentText)
            .setSubText("Live Spend")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(monthTitle)
                    .bigText(contentText)
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingOpenIntent)
            .setProgress(100, progress, false)
            .addAction(
                android.R.drawable.ic_input_add,
                "Add Spend",
                pendingAddExpenseIntent
            )
            .addAction(
                android.R.drawable.ic_menu_view,
                "Open App",
                pendingOpenIntent
            )

        return builder.build()
    }

    private fun getPacedNotificationLargeIcon(context: Context, status: PacingStatus, currency: String = "₹"): Bitmap {
        val size = (context.resources.displayMetrics.density * 64).toInt().coerceAtLeast(96)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val (bgColor, ringColor, accentColor) = when (status) {
            PacingStatus.ON_TRACK -> Triple(0xFFECFDF5.toInt(), 0xFF10B981.toInt(), 0xFF059669.toInt()) // Emerald Green Safe
            PacingStatus.CAUTION -> Triple(0xFFFFFBEB.toInt(), 0xFFF59E0B.toInt(), 0xFFD97706.toInt()) // Amber Caution / Over-Paced
            PacingStatus.OVER_PACED -> Triple(0xFFFFF1F2.toInt(), 0xFFF43F5E.toInt(), 0xFFE11D48.toInt()) // Rose Exceeded Budget
        }

        val radius = size / 2f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(radius, radius, radius - 2f, bgPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ringColor
            style = Paint.Style.STROKE
            strokeWidth = size * 0.065f
        }
        canvas.drawCircle(radius, radius, radius - size * 0.04f, ringPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = size * 0.48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val yPos = radius - ((textPaint.descent() + textPaint.ascent()) / 2)
        val symbol = if (currency.isNotBlank()) currency else "₹"
        canvas.drawText(symbol, radius, yPos, textPaint)

        return bitmap
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
        const val CHANNEL_ID = "live_expenditure_channel_v5"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.action.START_TRACKER"
        const val ACTION_SYNC = "com.example.action.SYNC_EXPENDITURE"
        const val ACTION_STOP = "com.example.action.STOP_TRACKER"
        const val ACTION_ADD_SPEND = "com.example.savior.ACTION_ADD_SPEND"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                // Delete older channels to flush icon and notification cache
                try {
                    notificationManager.deleteNotificationChannel("live_expenditure_channel")
                    notificationManager.deleteNotificationChannel("live_expenditure_channel_v2")
                    notificationManager.deleteNotificationChannel("live_expenditure_channel_v3")
                    notificationManager.deleteNotificationChannel("live_expenditure_channel_v4")
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val name = "Live Monthly Expenditure"
                val descriptionText = "Persistent status bar tracker for debits, transfers, and spends"
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    setShowBadge(false)
                }
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
