package com.example.data

import android.content.Context
import com.example.service.LiveExpenditureNotificationService
import com.example.sms.SampleSmsData
import com.example.sms.SmsReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ExpenseRepository(
    private val context: Context,
    private val expenseDao: ExpenseDao,
    private val preferences: ExpensePreferences,
    private val merchantRuleDao: MerchantRuleDao? = null
) {

    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val allMonthKeys: Flow<List<String>> = expenseDao.getAllMonthKeys()
    val recurringExpenses: Flow<List<ExpenseEntity>> = expenseDao.getRecurringExpenses()
    val allMerchantRules: Flow<List<MerchantRuleEntity>> =
        merchantRuleDao?.getAllRules() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertMerchantRule(rule: MerchantRuleEntity): Long = withContext(Dispatchers.IO) {
        merchantRuleDao?.insertRule(rule) ?: -1L
    }

    suspend fun deleteMerchantRule(id: Long) = withContext(Dispatchers.IO) {
        merchantRuleDao?.deleteRule(id)
    }

    suspend fun getAllMerchantRulesSync(): List<MerchantRuleEntity> = withContext(Dispatchers.IO) {
        merchantRuleDao?.getAllRulesSync() ?: emptyList()
    }

    suspend fun updateMerchantName(
        id: Long,
        oldMerchant: String,
        newMerchant: String,
        category: String
    ) = withContext(Dispatchers.IO) {
        val cleanNew = newMerchant.trim()
        val cleanOld = oldMerchant.trim()
        if (cleanNew.isBlank()) return@withContext

        // 1. Update Room records
        expenseDao.updateMerchantName(id, cleanNew)
        if (cleanOld.isNotBlank() && !cleanOld.equals(cleanNew, ignoreCase = true)) {
            expenseDao.updateMerchantNameForMatching(cleanOld, cleanNew)

            // 2. Autosave deterministic rule and alias in merchant_rules table
            merchantRuleDao?.insertRule(
                MerchantRuleEntity(
                    merchantPattern = cleanOld,
                    assignedCategory = category,
                    normalizedAlias = cleanNew,
                    isRegex = false,
                    createdAt = System.currentTimeMillis()
                )
            )
            // 3. Save to merchant preferences
            preferences.saveMerchantCategory(cleanNew, category)
        }

        if (preferences.isPersistentNotificationEnabled) {
            LiveExpenditureNotificationService.updateLiveExpenditure(context)
        }
    }

    suspend fun applyRefund(id: Long, refundAmount: Double) = withContext(Dispatchers.IO) {
        expenseDao.applyRefund(id, refundAmount)
        if (preferences.isPersistentNotificationEnabled) {
            LiveExpenditureNotificationService.updateLiveExpenditure(context)
        }
    }

    suspend fun setRefundedAmount(id: Long, refundAmount: Double) = withContext(Dispatchers.IO) {
        expenseDao.setRefundedAmount(id, refundAmount)
        if (preferences.isPersistentNotificationEnabled) {
            LiveExpenditureNotificationService.updateLiveExpenditure(context)
        }
    }

    suspend fun getExpensesSince(sinceTimestamp: Long): List<ExpenseEntity> = withContext(Dispatchers.IO) {
        expenseDao.getExpensesSinceSync(sinceTimestamp)
    }

    fun getExpensesForMerchant(merchant: String): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesForMerchant(merchant)

    suspend fun updateIsRecurring(id: Long, isRecurring: Boolean) = withContext(Dispatchers.IO) {
        expenseDao.updateIsRecurring(id, isRecurring)
    }

    suspend fun updateIsExcluded(id: Long, isExcluded: Boolean) = withContext(Dispatchers.IO) {
        expenseDao.updateIsExcluded(id, isExcluded)
        if (preferences.isPersistentNotificationEnabled) {
            LiveExpenditureNotificationService.updateLiveExpenditure(context)
        }
    }

    suspend fun updateIsRecurringForMerchant(merchant: String, isRecurring: Boolean) = withContext(Dispatchers.IO) {
        expenseDao.updateIsRecurringForMerchant(merchant, isRecurring)
    }

    fun getExpensesForMonth(monthKey: String): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesForMonth(monthKey)

    suspend fun insertExpense(expense: ExpenseEntity): Long = withContext(Dispatchers.IO) {
        val id = expenseDao.insertExpense(expense)
        if (id > 0 && preferences.isPersistentNotificationEnabled) {
            LiveExpenditureNotificationService.updateLiveExpenditure(context)
        }
        id
    }

    suspend fun deleteExpense(id: Long) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpenseById(id)
        if (preferences.isPersistentNotificationEnabled) {
            LiveExpenditureNotificationService.updateLiveExpenditure(context)
        }
    }

    suspend fun deleteExpensesForMonth(monthKey: String): Int = withContext(Dispatchers.IO) {
        val deletedCount = expenseDao.deleteExpensesForMonth(monthKey)
        if (preferences.isPersistentNotificationEnabled) {
            LiveExpenditureNotificationService.updateLiveExpenditure(context)
        }
        deletedCount
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        expenseDao.clearAll()
        if (preferences.isPersistentNotificationEnabled) {
            LiveExpenditureNotificationService.updateLiveExpenditure(context)
        }
    }

    suspend fun parseAndAddMessage(rawBody: String, sender: String = "TestSMS"): Boolean =
        withContext(Dispatchers.IO) {
            val inserted = com.example.service.ExpenseProcessingHelper.processRawSms(
                context = context,
                rawText = rawBody,
                sender = sender,
                timestamp = System.currentTimeMillis()
            )
            inserted != null
        }

    suspend fun syncInbox(onProgress: ((current: Int, total: Int) -> Unit)? = null): Int = withContext(Dispatchers.IO) {
        if (!SmsReader.hasReadSmsPermission(context)) {
            return@withContext 0
        }

        val lastSync = preferences.lastSyncTimestamp
        val candidateMessages = SmsReader.readCandidateSmsMessages(context, lastSync, limit = 50)
        var insertedCount = 0

        for ((index, msg) in candidateMessages.withIndex()) {
            onProgress?.invoke(index + 1, candidateMessages.size)

            // Validate with AI (gemini-3.5-flash-lite) to confirm if actually spend/transfer and intelligently enhance
            val inserted = com.example.service.ExpenseProcessingHelper.processRawSms(
                context = context,
                rawText = msg.body,
                sender = msg.sender,
                timestamp = msg.timestamp,
                isBatchSync = true
            )
            if (inserted != null) {
                insertedCount++
            }
        }

        preferences.lastSyncTimestamp = System.currentTimeMillis()
        if (insertedCount > 0 && preferences.isPersistentNotificationEnabled) {
            LiveExpenditureNotificationService.updateLiveExpenditure(context)
        }
        insertedCount
    }

    suspend fun importInitialSampleDataIfNeeded() = withContext(Dispatchers.IO) {
        if (!preferences.hasImportedInitialSamples) {
            val samples = SampleSmsData.createInitialSampleExpenses(preferences.currency)
            for (item in samples) {
                val exists = expenseDao.existsByContent(item.sender, item.timestamp, item.amount)
                if (!exists) {
                    expenseDao.insertExpense(item)
                }
            }
            preferences.hasImportedInitialSamples = true
            if (preferences.isPersistentNotificationEnabled) {
                LiveExpenditureNotificationService.updateLiveExpenditure(context)
            }
        }
    }
}
