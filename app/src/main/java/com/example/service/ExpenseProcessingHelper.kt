package com.example.service

import android.content.Context
import android.util.Log
import com.example.SpendTrackerApplication
import com.example.ai.OpenRouterCategorizer
import com.example.data.ExpenseEntity
import com.example.sms.ParsedSms
import com.example.sms.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExpenseProcessingHelper {
    private const val TAG = "ExpenseProcessingHelper"

    /**
     * Process raw incoming SMS using OpenRouter AI (gemini-3.5-flash-lite) if available,
     * or fallback to local SmsParser.
     * Determines whether message is credit, debit, intimation, ad, OTP etc.
     */
    suspend fun processRawSms(
        context: Context,
        rawText: String,
        sender: String,
        timestamp: Long = System.currentTimeMillis()
    ): ExpenseEntity? = withContext(Dispatchers.IO) {
        val app = context.applicationContext as? SpendTrackerApplication ?: return@withContext null
        val prefs = app.preferences
        val apiKey = prefs.openRouterApiKey.trim()

        // 1. Check if it's a refund or reversal via deterministic local parser
        val localParsed = SmsParser.parse(rawText, sender)
        if (localParsed != null && localParsed.isRefund) {
            return@withContext handleRefund(context, localParsed, sender, timestamp)
        }

        // 2. Try OpenRouter AI processing if API key is provided
        if (apiKey.isNotEmpty()) {
            val aiParsed = OpenRouterCategorizer.parseSmsTransaction(
                rawText = rawText,
                sender = sender,
                apiKey = apiKey,
                model = prefs.openRouterModel
            )
            if (aiParsed != null) {
                if (!aiParsed.isExpense) {
                    Log.d(TAG, "SMS classified as non-expense (${aiParsed.classification}), ignoring: '$rawText'")
                    return@withContext null
                }

                val parsed = ParsedSms(
                    amount = aiParsed.amount,
                    currency = aiParsed.currency,
                    type = aiParsed.type,
                    title = aiParsed.merchant,
                    accountInfo = aiParsed.accountInfo,
                    category = aiParsed.category,
                    isExpense = true,
                    rawText = rawText
                )
                return@withContext processAndInsertExpense(context, parsed, sender, timestamp)
            }
        }

        // 3. Fallback to enhanced local regex parser
        if (localParsed != null && localParsed.isExpense) {
            return@withContext processAndInsertExpense(context, localParsed, sender, timestamp)
        }
        return@withContext null
    }

    /**
     * Credit Reversal & Refund Auto-Reconciliation.
     * Identifies matching past debit expenditures in the last 30 days and offsets them.
     */
    suspend fun handleRefund(
        context: Context,
        parsed: ParsedSms,
        sender: String,
        timestamp: Long
    ): ExpenseEntity? = withContext(Dispatchers.IO) {
        val app = context.applicationContext as? SpendTrackerApplication ?: return@withContext null
        val dao = app.database.expenseDao()
        val prefs = app.preferences

        // Look back up to 30 days for a matching debit transaction
        val lookbackMillis = 30L * 24 * 60 * 60 * 1000
        val minTimestamp = timestamp - lookbackMillis
        val matching = dao.findMatchingDebitForRefund(
            amount = parsed.amount,
            merchantKeyword = parsed.title,
            minTimestamp = minTimestamp,
            maxTimestamp = timestamp + 3600000L
        )

        val preferredCurrency = prefs.currency.ifEmpty { parsed.currency }
        val resultExpense: ExpenseEntity?

        if (matching != null) {
            Log.d(TAG, "Reconciling refund of ${parsed.amount} against existing expense id=${matching.id} (${matching.merchantOrRecipient})")
            dao.applyRefund(matching.id, parsed.amount)
            val updated = dao.getExpenseById(matching.id)
            if (updated != null && updated.amount <= updated.refundedAmount) {
                dao.updateIsReversal(matching.id, true)
            }
            resultExpense = updated
        } else {
            // Standalone refund record inserted with netAmount = 0
            Log.d(TAG, "No matching recent debit found for refund ${parsed.amount} from '${parsed.title}'. Recording standalone reversal.")
            val refundEntity = ExpenseEntity(
                amount = parsed.amount,
                currency = preferredCurrency,
                type = com.example.data.ExpenseType.MERCHANT,
                merchantOrRecipient = if (parsed.title.isNotBlank() && !parsed.title.equals("Merchant / Payee", ignoreCase = true)) parsed.title else "Refund / Reversal",
                accountInfo = parsed.accountInfo,
                category = "Refund",
                rawBody = parsed.rawText,
                sender = sender,
                timestamp = timestamp,
                refundedAmount = parsed.amount,
                isReversal = true
            )
            val id = dao.insertExpense(refundEntity)
            resultExpense = refundEntity.copy(id = id)
        }

        if (prefs.isPersistentNotificationEnabled) {
            LiveExpenditureNotificationService.updateLiveExpenditure(context)
        }

        resultExpense
    }

    /**
     * Categorizes using deterministic merchant rules & aliases, remembered merchant preferences,
     * OpenRouter gemini-3.5-flash-lite, or heuristic classification.
     */
    suspend fun processAndInsertExpense(
        context: Context,
        parsed: ParsedSms,
        sender: String,
        timestamp: Long = System.currentTimeMillis()
    ): ExpenseEntity? = withContext(Dispatchers.IO) {
        val app = context.applicationContext as? SpendTrackerApplication ?: return@withContext null
        val dao = app.database.expenseDao()
        val ruleDao = app.database.merchantRuleDao()
        val prefs = app.preferences

        val exists = dao.existsByContent(sender, timestamp, parsed.amount)
        if (exists) return@withContext null

        val apiKey = prefs.openRouterApiKey.trim()
        val preferredCurrency = prefs.currency.ifEmpty { parsed.currency }

        var effectiveMerchant = parsed.title.trim()
        var finalCategory = parsed.category
        var isUnrecognized = false

        // 1. Auto-Rule & Merchant Alias Engine (Deterministic Local Classifier takes top precedence)
        val activeRules = ruleDao.getAllRulesSync()
        var matchedRuleCategory: String? = null

        for (rule in activeRules) {
            val pattern = rule.merchantPattern.trim()
            if (pattern.isBlank()) continue
            val matches = if (rule.isRegex) {
                try {
                    Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(effectiveMerchant) ||
                    Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(parsed.rawText)
                } catch (e: Exception) { false }
            } else {
                val clean = pattern.removePrefix("*").removeSuffix("*").trim()
                effectiveMerchant.contains(clean, ignoreCase = true) ||
                parsed.rawText.contains(clean, ignoreCase = true)
            }
            if (matches) {
                matchedRuleCategory = rule.assignedCategory
                if (rule.normalizedAlias.isNotBlank()) {
                    effectiveMerchant = rule.normalizedAlias.trim()
                }
                break
            }
        }

        if (!matchedRuleCategory.isNullOrBlank()) {
            finalCategory = matchedRuleCategory
            isUnrecognized = false
        } else {
            // Check if user previously mapped this merchant to a category
            val rememberedCategory = prefs.getMerchantCategory(effectiveMerchant)
            if (!rememberedCategory.isNullOrBlank()) {
                finalCategory = rememberedCategory
                isUnrecognized = false
            } else if (apiKey.isNotEmpty() && (finalCategory.isBlank() || finalCategory.equals("General", ignoreCase = true) || finalCategory.equals("Uncategorized", ignoreCase = true))) {
                val aiResult = OpenRouterCategorizer.categorizeSms(
                    rawText = parsed.rawText,
                    merchant = effectiveMerchant,
                    amount = parsed.amount,
                    currency = preferredCurrency,
                    apiKey = apiKey,
                    model = prefs.openRouterModel
                )
                if (aiResult.category == "UNKNOWN") {
                    finalCategory = "Uncategorized"
                    isUnrecognized = true
                } else {
                    finalCategory = aiResult.category
                }
            } else {
                // Local fallback logic: if local category is General Spend, treat as unrecognized
                if (parsed.category.equals("General Spend", ignoreCase = true) ||
                    parsed.category.equals("Payment", ignoreCase = true)
                ) {
                    finalCategory = "Uncategorized"
                    isUnrecognized = true
                }
            }
        }

        // Deduplicate credit card payment SMSs in the same month based on the amount
        val isCreditCardPayment = parsed.type == com.example.data.ExpenseType.CREDIT_CARD ||
                finalCategory.equals("Credit Card Bill", ignoreCase = true) ||
                parsed.category.equals("Credit Card Bill", ignoreCase = true)

        if (isCreditCardPayment) {
            val monthKey = ExpenseEntity.formatMonthKey(timestamp)
            val duplicateCount = dao.countCreditCardPaymentsInMonth(monthKey, parsed.amount)
            if (duplicateCount > 0) {
                Log.d(TAG, "Deduplicating credit card payment of amount ${parsed.amount} in month $monthKey")
                return@withContext null
            }
        }

        val entity = ExpenseEntity(
            amount = parsed.amount,
            currency = preferredCurrency,
            type = parsed.type,
            merchantOrRecipient = effectiveMerchant,
            accountInfo = parsed.accountInfo,
            category = finalCategory,
            rawBody = parsed.rawText,
            sender = sender,
            timestamp = timestamp
        )

        val insertedId = dao.insertExpense(entity)
        val insertedExpense = entity.copy(id = insertedId)

        // 1. If unrecognized, trigger push notification asking user to assign category
        if (isUnrecognized) {
            SpendAlertManager.notifyUnrecognizedSpend(
                context = context,
                expenseId = insertedId,
                merchant = entity.merchantOrRecipient,
                amount = entity.amount,
                currency = entity.currency
            )
        }

        // 2. Check category limit and trigger 80% / overshot alerts
        checkCategoryLimitAlert(context, entity)

        // 3. Update persistent notification
        if (prefs.isPersistentNotificationEnabled) {
            LiveExpenditureNotificationService.updateLiveExpenditure(context)
        }

        insertedExpense
    }

    suspend fun checkCategoryLimitAlert(context: Context, expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        val app = context.applicationContext as? SpendTrackerApplication ?: return@withContext
        val dao = app.database.expenseDao()
        val prefs = app.preferences

        val category = expense.category
        val limit = prefs.getCategoryLimit(category)
        if (limit > 0.0) {
            val monthKey = expense.monthKey
            val totalSpentInCat = dao.getTotalForCategoryInMonthSync(monthKey, category) ?: 0.0
            SpendAlertManager.checkAndNotifyCategoryLimit(
                context = context,
                category = category,
                currentCategoryTotal = totalSpentInCat,
                categoryLimit = limit,
                currency = prefs.currency
            )
        }
    }
}
