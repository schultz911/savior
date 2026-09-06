package com.example.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.SpendTrackerApplication
import com.example.data.ExpenseType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Scheduled Sunday Evening Spend Digest (Zero-Overhead Local Worker).
 * Runs periodically every Sunday at 8:00 PM to deliver a proactive local summary
 * of the user's trailing 7-day spending, top category outflow, and month pacing.
 */
class WeeklySpendDigestWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "WeeklySpendDigestWorker running Sunday spend digest...")
        try {
            val app = applicationContext as? SpendTrackerApplication ?: return Result.success()

            // 1. Check if user enabled weekly digest
            if (!app.preferences.isWeeklyDigestEnabled) {
                Log.d(TAG, "Weekly spend digest disabled in user preferences, skipping.")
                return Result.success()
            }

            // 2. Check if notifications are enabled
            if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
                Log.d(TAG, "Notifications disabled at system level, skipping.")
                return Result.success()
            }

            // 3. Query trailing 7-day transactions
            val now = System.currentTimeMillis()
            val sevenDaysAgo = now - TimeUnit.DAYS.toMillis(7)
            val recentExpenses = app.repository.getExpensesSince(sevenDaysAgo)

            val blacklisted = app.preferences.getBlacklistedMerchants().map { it.trim().lowercase(Locale.US) }
            val validExpenses = recentExpenses.filter {
                !it.isExcluded &&
                it.type != ExpenseType.SELF &&
                !it.category.equals("Self", ignoreCase = true) &&
                !it.category.equals("Credit Card Bill", ignoreCase = true) &&
                !blacklisted.any { b -> it.merchantOrRecipient.trim().lowercase(Locale.US).contains(b) }
            }

            var totalWeeklySpend = 0.0
            for (exp in validExpenses) {
                if (exp.isRefundOrReversal) {
                    totalWeeklySpend = (totalWeeklySpend - exp.amount).coerceAtLeast(0.0)
                } else {
                    totalWeeklySpend += (exp.amount - exp.refundedAmount).coerceAtLeast(0.0)
                }
            }

            val currency = app.preferences.currency
            val numberFormatter = NumberFormat.getNumberInstance(Locale.US).apply {
                minimumFractionDigits = 0
                maximumFractionDigits = 0
            }

            // Group by category to find top spending category (excluding refunds)
            val topCategoryGroup = validExpenses.filter { !it.isRefundOrReversal }.groupBy { it.category }
                .maxByOrNull { entry -> entry.value.sumOf { (it.amount - it.refundedAmount).coerceAtLeast(0.0) } }

            val topCategoryName = topCategoryGroup?.key ?: "General"
            val topCategoryAmount = topCategoryGroup?.value?.sumOf { (it.amount - it.refundedAmount).coerceAtLeast(0.0) } ?: 0.0

            val cal = Calendar.getInstance()
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val currentDay = cal.get(Calendar.DAY_OF_MONTH)
            val daysRemaining = (daysInMonth - currentDay + 1).coerceAtLeast(1)
            val monthFormat = SimpleDateFormat("MMMM", Locale.US).format(cal.time)

            // Formulate notification text
            val formattedTotal = "${currency}${numberFormatter.format(totalWeeklySpend)}"
            val formattedTop = "${currency}${numberFormatter.format(topCategoryAmount)}"

            val title = "Weekly Spend Digest • $formattedTotal"
            val shortText = if (totalWeeklySpend > 0) {
                "Top: $topCategoryName ($formattedTop) • $daysRemaining days left in $monthFormat"
            } else {
                "Zero spend recorded this week • $daysRemaining days left in $monthFormat"
            }

            val bigText = if (totalWeeklySpend > 0) {
                "In the past 7 days, you spent a net total of $formattedTotal across ${validExpenses.size} transaction(s).\n" +
                "• Top Category: $topCategoryName ($formattedTop)\n" +
                "• Month Progress: $daysRemaining day(s) remaining in $monthFormat.\n" +
                "Tap to view your complete financial analytics."
            } else {
                "You recorded ₹0 in spending over the past 7 days.\n" +
                "• Month Progress: $daysRemaining day(s) remaining in $monthFormat.\n" +
                "Tap to review your monthly targets."
            }

            // Tap intent deep-links directly into Analytics Tab (tab index 1)
            val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("extra_open_tab", 1)
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                6001,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            SpendAlertManager.createNotificationChannels(applicationContext)

            val notif = NotificationCompat.Builder(applicationContext, SpendAlertManager.CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_stat_rupee)
                .setContentTitle(title)
                .setContentText(shortText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIF_ID_WEEKLY_DIGEST, notif)
            Log.d(TAG, "Weekly spend digest notification dispatched successfully.")

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "WeeklySpendDigestWorker failed: ${e.message}", e)
            return Result.success()
        }
    }

    companion object {
        private const val TAG = "WeeklySpendDigestWorker"
        const val WORK_NAME = "savior_weekly_spend_digest_worker"
        const val NOTIF_ID_WEEKLY_DIGEST = 6001

        fun calculateInitialDelayToSundayEvening(): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 20)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
        }

        fun schedule(context: Context) {
            try {
                val initialDelay = calculateInitialDelayToSundayEvening()
                val workRequest = PeriodicWorkRequestBuilder<WeeklySpendDigestWorker>(
                    7, TimeUnit.DAYS,
                    2, TimeUnit.HOURS
                )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
                Log.d(TAG, "WeeklySpendDigestWorker scheduled (initial delay: ${initialDelay / 1000 / 60} mins).")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule WeeklySpendDigestWorker: ${e.message}", e)
            }
        }

        fun cancel(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                Log.d(TAG, "WeeklySpendDigestWorker cancelled.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cancel WeeklySpendDigestWorker: ${e.message}", e)
            }
        }
    }
}
