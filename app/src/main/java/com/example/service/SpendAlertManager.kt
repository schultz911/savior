package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.text.NumberFormat
import java.util.Locale

object SpendAlertManager {
    const val CHANNEL_ALERTS = "savior_budget_alerts_v3"
    private const val NOTIF_ID_UNRECOGNIZED_BASE = 2000
    private const val NOTIF_ID_CATEGORY_LIMIT_BASE = 3000

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            try {
                manager.deleteNotificationChannel("savio_alerts_channel")
                manager.deleteNotificationChannel("spend_alerts_channel")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val channel = NotificationChannel(
                CHANNEL_ALERTS,
                "SAVIO Budget & Category Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for unrecognized transactions, 80% category budget warning, and overshoots"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Push notification asking the user to assign a category when a spend cannot be recognized.
     * Features in-line direct category classification actions for 1-tap categorization without opening app.
     */
    fun notifyUnrecognizedSpend(
        context: Context,
        expenseId: Long,
        merchant: String,
        amount: Double,
        currency: String
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = (NOTIF_ID_UNRECOGNIZED_BASE + (expenseId % 1000)).toInt()

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_ASSIGN_CATEGORY_EXPENSE_ID, expenseId)
            putExtra(MainActivity.EXTRA_NAVIGATE_TAB, MainActivity.TAB_DASHBOARD)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun createCategoryActionPendingIntent(category: String, actionCode: Int): PendingIntent {
            val intent = Intent(context, com.example.receiver.CategoryActionReceiver::class.java).apply {
                action = com.example.receiver.CategoryActionReceiver.ACTION_ASSIGN_CATEGORY
                putExtra(com.example.receiver.CategoryActionReceiver.EXTRA_EXPENSE_ID, expenseId)
                putExtra(com.example.receiver.CategoryActionReceiver.EXTRA_CATEGORY, category)
                putExtra(com.example.receiver.CategoryActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
            }
            return PendingIntent.getBroadcast(
                context,
                notifId * 10 + actionCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val formattedAmount = formatCurrency(amount, currency)
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_rupee)
            .setLargeIcon(getNotificationLargeIcon(context))
            .setColor(0xFF059669.toInt())
            .setContentTitle("Categorize Spend: $formattedAmount")
            .setContentText("Transaction at '$merchant' needs a category.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("We couldn't recognize '$merchant' ($formattedAmount). Tap a quick category below or open the app for more.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                0,
                "Dining",
                createCategoryActionPendingIntent("Food & Dining", 1)
            )
            .addAction(
                0,
                "Groceries",
                createCategoryActionPendingIntent("Groceries", 2)
            )
            .addAction(
                0,
                "Shopping",
                createCategoryActionPendingIntent("Shopping", 3)
            )
            .build()

        manager.notify(notifId, notification)
    }

    /**
     * Notify user when they reach 80% of their category limit or overshoot 100%.
     */
    fun checkAndNotifyCategoryLimit(
        context: Context,
        category: String,
        currentCategoryTotal: Double,
        categoryLimit: Double,
        currency: String
    ) {
        if (categoryLimit <= 0.0) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val ratio = currentCategoryTotal / categoryLimit
        val notifId = NOTIF_ID_CATEGORY_LIMIT_BASE + Math.abs(category.hashCode()) % 1000

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_NAVIGATE_TAB, MainActivity.TAB_DASHBOARD)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val currentFmt = formatCurrency(currentCategoryTotal, currency)
        val limitFmt = formatCurrency(categoryLimit, currency)

        if (ratio >= 1.0) {
            // Overshot limit!
            val overBy = formatCurrency(currentCategoryTotal - categoryLimit, currency)
            val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_stat_rupee)
                .setLargeIcon(getNotificationLargeIcon(context))
                .setColor(0xFF059669.toInt())
                .setContentTitle("⚠️ Category Budget Overshot: $category")
                .setContentText("Spent $currentFmt of $limitFmt limit (+${overBy} over)!")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("You have overshot your monthly $category spend limit of $limitFmt. Total spent is now $currentFmt (exceeded by $overBy).")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            manager.notify(notifId, notification)
        } else if (ratio >= 0.80) {
            // Reached 80% of limit
            val pct = (ratio * 100).toInt()
            val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_stat_rupee)
                .setLargeIcon(getNotificationLargeIcon(context))
                .setColor(0xFF059669.toInt())
                .setContentTitle("⚡ 80% Budget Alert: $category")
                .setContentText("You've reached $pct% of your $limitFmt limit ($currentFmt spent).")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Heads up: You have used $pct% of your $limitFmt budget for $category ($currentFmt spent). Consider monitoring upcoming spends.")
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            manager.notify(notifId, notification)
        }
    }

    @Volatile
    private var cachedLargeIcon: Bitmap? = null

    fun getNotificationLargeIcon(context: Context): Bitmap {
        cachedLargeIcon?.let { return it }
        synchronized(this) {
            cachedLargeIcon?.let { return it }
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_savio_logo)
            val bitmap = if (drawable != null) {
                val size = (context.resources.displayMetrics.density * 64).toInt().coerceAtLeast(96)
                val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp
            } else {
                BitmapFactory.decodeResource(context.resources, R.mipmap.ic_savio_launcher)
            }
            cachedLargeIcon = bitmap
            return bitmap
        }
    }

    private val currencyFormatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private fun formatCurrency(amount: Double, currency: String): String = synchronized(currencyFormatter) {
        "$currency${currencyFormatter.format(amount)}"
    }

    /**
     * Proactive reminder when an unpaid recurring bill or subscription is due within 48 hours.
     */
    fun checkAndNotifyUpcomingBills(
        context: Context,
        bills: List<com.example.engine.PredictedRecurringBill>,
        currency: String
    ) {
        if (bills.isEmpty()) return
        val cal = java.util.Calendar.getInstance()
        val currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val currentMonthKey = com.example.data.ExpenseEntity.formatMonthKey(System.currentTimeMillis())

        val alertPrefs = context.getSharedPreferences("spend_tracker_recurring_alerts", Context.MODE_PRIVATE)

        for (bill in bills) {
            if (bill.isPaidThisMonth) continue
            val diffDays = bill.typicalDayOfMonth - currentDay
            // Alert if due within next 2 days or today (0, 1, or 2 days away)
            if (diffDays in 0..2) {
                val alertKey = "${currentMonthKey}_${bill.merchant}_${bill.typicalDayOfMonth}"
                if (alertPrefs.getBoolean(alertKey, false)) {
                    // Already notified this month
                    continue
                }

                createNotificationChannels(context)
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notifId = 4000 + Math.abs(bill.merchant.hashCode()) % 1000

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(MainActivity.EXTRA_NAVIGATE_TAB, MainActivity.TAB_DASHBOARD)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    notifId,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val dueText = when (diffDays) {
                    0 -> "due today"
                    1 -> "due tomorrow"
                    else -> "due in 2 days"
                }
                val formattedAmt = formatCurrency(bill.expectedAmount, currency)

                val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
                    .setSmallIcon(R.drawable.ic_stat_rupee)
                    .setLargeIcon(getNotificationLargeIcon(context))
                    .setColor(0xFF059669.toInt())
                    .setContentTitle("⚡ Upcoming Bill: $formattedAmt for ${bill.merchant}")
                    .setContentText("Subscription/bill is $dueText (~${bill.typicalDayOfMonth}th). Safe daily spend is protected.")
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText("Heads up: Your recurring commitment for ${bill.merchant} of $formattedAmt is $dueText. Your daily pacing has reserved this amount.")
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                manager.notify(notifId, notification)
                alertPrefs.edit().putBoolean(alertKey, true).apply()
            }
        }
    }

    const val NOTIF_ID_VELOCITY = 5001
    private const val NOTIF_ID_ANOMALY_BASE = 6000

    /**
     * Proactive velocity pacing alert when spend burn rate is >30% ahead of expected day-of-month budget pacing.
     */
    fun checkAndNotifySpendVelocity(
        context: Context,
        currentSpent: Double,
        monthlyBudget: Double,
        currency: String
    ) {
        if (monthlyBudget <= 0.0) return

        val cal = java.util.Calendar.getInstance()
        val currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

        // Only evaluate past Day 3 to prevent false alerts on early fixed bills
        if (currentDay < 4) return

        val expectedSpend = (currentDay.toDouble() / daysInMonth.toDouble()) * monthlyBudget
        if (currentSpent <= 1.30 * expectedSpend) return

        val currentMonthKey = com.example.data.ExpenseEntity.formatMonthKey(System.currentTimeMillis())
        val alertPrefs = context.getSharedPreferences("spend_tracker_velocity_alerts", Context.MODE_PRIVATE)
        val lastAlertDay = alertPrefs.getInt("last_alert_day_${currentMonthKey}", 0)

        // Rate-limit: alert at most once every 4 days per month
        if (currentDay - lastAlertDay < 4) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_NAVIGATE_TAB, MainActivity.TAB_DASHBOARD)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIF_ID_VELOCITY,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val overrunPct = (((currentSpent - expectedSpend) / expectedSpend) * 100).toInt()
        val remainingDays = (daysInMonth - currentDay + 1).coerceAtLeast(1)
        val remainingBudget = (monthlyBudget - currentSpent).coerceAtLeast(0.0)
        val recommendedDailyCap = remainingBudget / remainingDays

        val currentSpentFmt = formatCurrency(currentSpent, currency)
        val budgetFmt = formatCurrency(monthlyBudget, currency)
        val capFmt = formatCurrency(recommendedDailyCap, currency)

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_rupee)
            .setLargeIcon(getNotificationLargeIcon(context))
            .setColor(0xFF059669.toInt())
            .setContentTitle("⚡ Spend Velocity Alert: Pacing Fast")
            .setContentText("Spent $currentSpentFmt on Day $currentDay ($overrunPct% ahead of monthly pace).")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Heads up: You've spent $currentSpentFmt by Day $currentDay, pacing $overrunPct% ahead of your $budgetFmt budget. Recommended cap: ~$capFmt/day for the remaining $remainingDays days to stay on track.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(NOTIF_ID_VELOCITY, notification)
        alertPrefs.edit().putInt("last_alert_day_${currentMonthKey}", currentDay).apply()
    }

    /**
     * Anomaly detection for unusually large single transactions (>4x trailing median and >= ₹1,500).
     */
    fun checkAndNotifyHighValueAnomaly(
        context: Context,
        expense: com.example.data.ExpenseEntity,
        medianAmount: Double,
        currency: String
    ) {
        val effectiveMedian = if (medianAmount > 0.0) medianAmount else 500.0
        val isAnomaly = expense.amount >= 4.0 * effectiveMedian && expense.amount >= 1500.0
        if (!isAnomaly) return

        val alertPrefs = context.getSharedPreferences("spend_tracker_anomaly_alerts", Context.MODE_PRIVATE)
        val alertKey = "anomaly_${expense.id}"
        if (alertPrefs.getBoolean(alertKey, false)) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = (NOTIF_ID_ANOMALY_BASE + (expense.id % 1000)).toInt()

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_ASSIGN_CATEGORY_EXPENSE_ID, expense.id)
            putExtra(MainActivity.EXTRA_NAVIGATE_TAB, MainActivity.TAB_DASHBOARD)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val amountFmt = formatCurrency(expense.amount, currency)
        val medianFmt = formatCurrency(effectiveMedian, currency)
        val multiplier = String.format(Locale.US, "%.1f", expense.amount / effectiveMedian)

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_rupee)
            .setLargeIcon(getNotificationLargeIcon(context))
            .setColor(0xFF059669.toInt())
            .setContentTitle("🔍 High-Value Spend: $amountFmt at ${expense.merchantOrRecipient}")
            .setContentText("Transaction is ${multiplier}x higher than your typical median spend ($medianFmt).")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Spike Alert: A debit of $amountFmt was recorded at '${expense.merchantOrRecipient}'. This is ${multiplier}x your typical transaction median ($medianFmt). Tap to inspect or re-categorize.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(notifId, notification)
        alertPrefs.edit().putBoolean(alertKey, true).apply()
    }
}
