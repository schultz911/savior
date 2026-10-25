package com.example.data

import android.content.Context
import com.example.service.LiveExpenditureNotificationService
import com.example.sms.SampleSmsData
import com.example.sms.SmsReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

class ExpenseRepository(
    private val context: Context,
    private val expenseDao: ExpenseDao,
    private val preferences: ExpensePreferences,
    private val merchantRuleDao: MerchantRuleDao? = null,
    private val instrumentRuleDao: InstrumentRuleDao? = null
) {
    private val syncMutex = Mutex()

    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val allMonthKeys: Flow<List<String>> = expenseDao.getAllMonthKeys()
    val allMerchantRules: Flow<List<MerchantRuleEntity>> =
        merchantRuleDao?.getAllRules() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val allInstrumentRules: Flow<List<InstrumentRuleEntity>> =
        instrumentRuleDao?.getAllInstruments() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertMerchantRule(rule: MerchantRuleEntity): Long = withContext(Dispatchers.IO) {
        merchantRuleDao?.insertRule(rule) ?: -1L
    }

    suspend fun deleteMerchantRule(id: Long) = withContext(Dispatchers.IO) {
        merchantRuleDao?.deleteRule(id)
    }

    suspend fun getAllMerchantRulesSync(): List<MerchantRuleEntity> = withContext(Dispatchers.IO) {
        merchantRuleDao?.getAllRulesSync() ?: emptyList()
    }

    suspend fun getAllInstrumentRulesSync(): List<InstrumentRuleEntity> = withContext(Dispatchers.IO) {
        instrumentRuleDao?.getAllInstrumentsSync() ?: emptyList()
    }

    /**
     * Saves a user-defined instrument rule.
     * If two instruments have the same last 4 digits, they are automatically clubbed
     * into a single instrument by appending the markers and consolidating details.
     */
    suspend fun saveAndClubInstrumentRule(rule: InstrumentRuleEntity): InstrumentRuleEntity = withContext(Dispatchers.IO) {
        val dao = instrumentRuleDao ?: return@withContext rule
        val cleanLast4 = rule.last4.trim().uppercase(java.util.Locale.US)

        if (cleanLast4.isNotBlank()) {
            val existingMatches = dao.getInstrumentsByLast4(cleanLast4)
                .filter { it.id != rule.id }

            if (existingMatches.isNotEmpty()) {
                val mergedMarkers = (existingMatches.flatMap { it.getMarkerList() } + rule.getMarkerList()).distinct()
                val mergedBank = if (rule.bankName.isNotBlank()) rule.bankName.trim() else existingMatches.map { it.bankName.trim() }.firstOrNull { it.isNotBlank() } ?: ""
                val mergedCard = if (rule.cardOrAccountName.isNotBlank()) rule.cardOrAccountName.trim() else existingMatches.map { it.cardOrAccountName.trim() }.firstOrNull { it.isNotBlank() } ?: ""

                val primaryId = if (rule.id != 0L) rule.id else existingMatches.first().id
                val consolidated = InstrumentRuleEntity(
                    id = primaryId,
                    bankName = mergedBank,
                    cardOrAccountName = mergedCard,
                    last4 = cleanLast4,
                    markers = InstrumentRuleEntity.markersToString(mergedMarkers),
                    createdAt = rule.createdAt
                )

                // Delete any other redundant records with same last 4
                for (match in existingMatches) {
                    if (match.id != primaryId) {
                        dao.deleteInstrumentById(match.id)
                    }
                }

                dao.updateInstrument(consolidated)
                return@withContext consolidated
            }
        }

        val cleanRule = rule.copy(
            bankName = rule.bankName.trim(),
            cardOrAccountName = rule.cardOrAccountName.trim(),
            last4 = cleanLast4,
            markers = InstrumentRuleEntity.markersToString(rule.getMarkerList())
        )
        if (cleanRule.id == 0L) {
            val newId = dao.insertInstrument(cleanRule)
            cleanRule.copy(id = newId)
        } else {
            dao.updateInstrument(cleanRule)
            cleanRule
        }
    }

    suspend fun deleteInstrumentRule(id: Long) = withContext(Dispatchers.IO) {
        instrumentRuleDao?.deleteInstrumentById(id)
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

    suspend fun setRefundedAmount(id: Long, refundAmount: Double) = withContext(Dispatchers.IO) {
        expenseDao.setRefundedAmount(id, refundAmount)
        if (preferences.isPersistentNotificationEnabled) {
            LiveExpenditureNotificationService.updateLiveExpenditure(context)
        }
    }

    suspend fun getExpensesSince(sinceTimestamp: Long): List<ExpenseEntity> = withContext(Dispatchers.IO) {
        expenseDao.getExpensesSinceSync(sinceTimestamp)
    }

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
            val parsedTime = com.example.sms.SmsParser.extractDateTime(rawBody) ?: System.currentTimeMillis()
            val inserted = com.example.service.ExpenseProcessingHelper.processRawSms(
                context = context,
                rawText = rawBody,
                sender = sender,
                timestamp = parsedTime
            )
            inserted != null
        }

    suspend fun syncInbox(onProgress: ((current: Int, total: Int) -> Unit)? = null): Int = withContext(Dispatchers.IO + com.example.ai.CloudRequestSession()) {
        if (!SmsReader.hasReadSmsPermission(context)) {
            return@withContext 0
        }

        // Guard against concurrent re-entrant syncs (app relaunch + pull-to-refresh + SmsCatchUpWorker
        // firing simultaneously all read the same stale lastSyncTimestamp before any writes commit).
        if (!syncMutex.tryLock()) {
            return@withContext 0
        }

        try {
            val lastSync = preferences.lastSyncTimestamp
            val isFirstScan = lastSync == 0L
            val sinceTimestamp = if (isFirstScan) {
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            } else {
                lastSync
            }
            val candidateMessages = SmsReader.readCandidateSmsMessages(
                context = context,
                sinceTimestamp = sinceTimestamp,
                limit = Int.MAX_VALUE
            )
            var insertedCount = 0
            val preloadedRules = merchantRuleDao?.getAllRulesSync()
            val preloadedInstrumentRules = instrumentRuleDao?.getAllInstrumentsSync()

            for ((index, msg) in candidateMessages.withIndex()) {
                onProgress?.invoke(index + 1, candidateMessages.size)

                // Validate with AI (anthropic/claude-haiku-5.5) to confirm if actually spend/transfer and intelligently enhance
                val inserted = com.example.service.ExpenseProcessingHelper.processRawSms(
                    context = context,
                    rawText = msg.body,
                    sender = msg.sender,
                    timestamp = msg.timestamp,
                    smsId = msg.smsId,
                    isBatchSync = true,
                    preloadedRules = preloadedRules,
                    preloadedInstrumentRules = preloadedInstrumentRules
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
        } finally {
            syncMutex.unlock()
        }
    }

    suspend fun rescanAllInbox(onProgress: ((current: Int, total: Int) -> Unit)? = null): Int = withContext(Dispatchers.IO + com.example.ai.CloudRequestSession()) {
        if (!SmsReader.hasReadSmsPermission(context)) {
            return@withContext 0
        }
        if (!syncMutex.tryLock()) {
            return@withContext 0
        }
        try {
            clearAll()
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.DAY_OF_MONTH, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val startOfMonthTimestamp = cal.timeInMillis
            preferences.lastSyncTimestamp = startOfMonthTimestamp
            val candidateMessages = SmsReader.readCandidateSmsMessages(
                context = context,
                sinceTimestamp = startOfMonthTimestamp,
                limit = Int.MAX_VALUE
            )
            var insertedCount = 0
            val preloadedRules = merchantRuleDao?.getAllRulesSync()
            val preloadedInstrumentRules = instrumentRuleDao?.getAllInstrumentsSync()

            for ((index, msg) in candidateMessages.withIndex()) {
                onProgress?.invoke(index + 1, candidateMessages.size)

                val inserted = com.example.service.ExpenseProcessingHelper.processRawSms(
                    context = context,
                    rawText = msg.body,
                    sender = msg.sender,
                    timestamp = msg.timestamp,
                    smsId = msg.smsId,
                    isBatchSync = true,
                    preloadedRules = preloadedRules,
                    preloadedInstrumentRules = preloadedInstrumentRules
                )
                if (inserted != null) {
                    insertedCount++
                }
            }

            preferences.lastSyncTimestamp = System.currentTimeMillis()
            if (preferences.isPersistentNotificationEnabled) {
                LiveExpenditureNotificationService.updateLiveExpenditure(context)
            }
            insertedCount
        } finally {
            syncMutex.unlock()
        }
    }

    suspend fun importInitialSampleDataIfNeeded() = withContext(Dispatchers.IO) {
        if (!preferences.hasImportedInitialSamples) {
            val samples = SampleSmsData.createInitialSampleExpenses(preferences.currency)
            expenseDao.insertExpenses(samples)
            preferences.hasImportedInitialSamples = true
            if (preferences.isPersistentNotificationEnabled) {
                LiveExpenditureNotificationService.updateLiveExpenditure(context)
            }
        }
    }
}
