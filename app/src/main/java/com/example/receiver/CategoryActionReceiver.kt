package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.SpendTrackerApplication
import com.example.data.ExpenseType
import com.example.data.MerchantRuleEntity
import com.example.service.ExpenseProcessingHelper
import com.example.service.LiveExpenditureNotificationService
import com.example.service.SpendAlertManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ASSIGN_CATEGORY) return

        val expenseId = intent.getLongExtra(EXTRA_EXPENSE_ID, -1L)
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        if (expenseId <= 0L) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? SpendTrackerApplication ?: return@launch
                val dao = app.database.expenseDao()
                val ruleDao = app.database.merchantRuleDao()
                val prefs = app.preferences

                val expense = dao.getExpenseById(expenseId) ?: return@launch

                val newType = when {
                    category.equals("Self", ignoreCase = true) -> ExpenseType.SELF
                    category.equals("Credit Card Bill", ignoreCase = true) -> ExpenseType.CREDIT_CARD
                    category.equals("Transfers", ignoreCase = true) -> ExpenseType.P2P
                    else -> ExpenseType.MERCHANT
                }

                // 1. Update this transaction in DB
                dao.updateCategoryAndType(expenseId, category, newType)

                // 2. If merchant is identified, save persistent rule and update other past transactions
                val merchant = expense.merchantOrRecipient.trim()
                if (merchant.isNotBlank() && !merchant.equals("Unknown", ignoreCase = true) && !merchant.equals("Merchant / Payee", ignoreCase = true)) {
                    prefs.saveMerchantCategory(merchant, category)
                    ruleDao.insertRule(
                        MerchantRuleEntity(
                            merchantPattern = merchant,
                            assignedCategory = category,
                            normalizedAlias = merchant,
                            isRegex = false,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    dao.updateCategoryAndTypeForMerchant(merchant, category, newType)
                }

                // 3. Check for category budget limit alert
                val updatedExpense = dao.getExpenseById(expenseId)
                if (updatedExpense != null) {
                    ExpenseProcessingHelper.checkCategoryLimitAlert(context, updatedExpense)
                }

                // 4. Update status bar live notification
                if (prefs.isPersistentNotificationEnabled) {
                    LiveExpenditureNotificationService.updateLiveExpenditure(context)
                }

                // 5. Update or dismiss notification with feedback
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notifId > 0) {
                    val feedbackNotification = NotificationCompat.Builder(context, SpendAlertManager.CHANNEL_ALERTS)
                        .setSmallIcon(R.drawable.ic_stat_rupee)
                        .setColor(0xFF059669.toInt())
                        .setContentTitle("Spend Categorized")
                        .setContentText("Tagged '$merchant' as $category ✓")
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setAutoCancel(true)
                        .setTimeoutAfter(3000)
                        .build()
                    manager.notify(notifId, feedbackNotification)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ASSIGN_CATEGORY = "com.example.savior.ACTION_ASSIGN_CATEGORY"
        const val EXTRA_EXPENSE_ID = "extra_expense_id"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
