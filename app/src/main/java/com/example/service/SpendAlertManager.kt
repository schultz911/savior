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
        createNotificationChannels(context)
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

        createNotificationChannels(context)
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

    fun getNotificationLargeIcon(context: Context): Bitmap {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_savio_logo)
        if (drawable != null) {
            val size = (context.resources.displayMetrics.density * 64).toInt().coerceAtLeast(96)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
        return BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
    }

    private fun formatCurrency(amount: Double, currency: String): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$currency${formatter.format(amount)}"
    }
}
