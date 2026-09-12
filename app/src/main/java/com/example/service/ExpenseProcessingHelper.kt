package com.example.service

import android.content.Context
import android.util.Log
import com.example.SpendTrackerApplication
import com.example.ai.AiCoreCategorizer
import com.example.ai.OpenRouterCategorizer
import com.example.data.ExpenseEntity
import com.example.data.MerchantRuleEntity
import com.example.sms.ParsedSms
import com.example.sms.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

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
        timestamp: Long = System.currentTimeMillis(),
        smsId: Long = 0L,
        isBatchSync: Boolean = false,
        preloadedRules: List<MerchantRuleEntity>? = null
    ): ExpenseEntity? = withContext(Dispatchers.IO) {
        val app = context.applicationContext as? SpendTrackerApplication ?: return@withContext null
        val prefs = app.preferences
        val apiKey = prefs.openRouterApiKey.trim()

        // 0. Fast-path smsId deduplication — skip AI and parsing entirely for known SMS IDs.
        if (smsId > 0L) {
            val dao = app.database.expenseDao()
            if (dao.existsBySmsId(smsId)) {
                Log.d(TAG, "Fast-path smsId dedup: smsId=$smsId already stored, skipping.")
                return@withContext null
            }
        }

        // 0.5. Suppress refund intimation, processing, or reference number messages (only actual settled refunds are recorded)
        if (SmsParser.isRefundIntimationOrPending(rawText)) {
            Log.d(TAG, "SMS classified as refund intimation/processing/reference, ignoring: '$rawText'")
            return@withContext null
        }

        // Local parser candidate for comparison or fallback
        val localParsed = SmsParser.parse(rawText, sender)

        val effectiveTimestamp = if (timestamp > 0L) timestamp else (localParsed?.parsedTimestamp ?: SmsParser.extractDateTime(rawText) ?: System.currentTimeMillis())

        // 1. Tier 1: Cloud AI (OpenRouter Gemini 3.5 Flash Lite) if API key is provided
        if (apiKey.isNotEmpty()) {
            val aiParsed = OpenRouterCategorizer.parseSmsTransaction(
                rawText = rawText,
                sender = sender,
                apiKey = apiKey,
                model = prefs.openRouterModel
            )
            if (aiParsed != null) {
                // If AI classified this as a REFUND, route to the refund handler with the
                // extracted merchant name (preferring specific merchant name if available)
                if (aiParsed.isRefund && aiParsed.amount > 0.0) {
                    val specificAi = aiParsed.merchant.takeIf { SmsParser.isSpecificMerchant(it) }
                    val specificLocal = localParsed?.title?.takeIf { SmsParser.isSpecificMerchant(it) }
                    val refundMerchant = specificAi ?: specificLocal ?: "Refund / Reversal"
                    Log.d(TAG, "SMS classified by Cloud AI as REFUND from '$refundMerchant', routing to handleRefund.")
                    val refundParsed = com.example.sms.ParsedSms(
                        amount = aiParsed.amount,
                        currency = aiParsed.currency,
                        type = aiParsed.type,
                        title = refundMerchant,
                        accountInfo = aiParsed.accountInfo,
                        category = "Refund",
                        isExpense = false,
                        rawText = rawText,
                        isRefund = true
                    )
                    return@withContext handleRefund(context, refundParsed, sender, effectiveTimestamp, smsId, isBatchSync)
                }

                if (!aiParsed.isExpense) {
                    Log.d(TAG, "SMS classified by Cloud AI as non-expense (${aiParsed.classification}), ignoring: '$rawText'")
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
                return@withContext processAndInsertExpense(context, parsed, sender, effectiveTimestamp, smsId, isBatchSync, preloadedRules)
            }
        }

        // 2. Tier 2: On-Device AI (Android AICore / Gemini Nano) fallback if OpenRouter is empty or fails
        if (AiCoreCategorizer.isAiCoreAvailable(context)) {
            val nanoParsed = AiCoreCategorizer.parseSmsTransaction(
                context = context,
                rawText = rawText,
                sender = sender
            )
            if (nanoParsed != null) {
                if (nanoParsed.isRefund && nanoParsed.amount > 0.0) {
                    val specificNano = nanoParsed.merchant.takeIf { SmsParser.isSpecificMerchant(it) }
                    val specificLocal = localParsed?.title?.takeIf { SmsParser.isSpecificMerchant(it) }
                    val refundMerchant = specificNano ?: specificLocal ?: "Refund / Reversal"
                    val refundParsed = com.example.sms.ParsedSms(
                        amount = nanoParsed.amount,
                        currency = nanoParsed.currency,
                        type = nanoParsed.type,
                        title = refundMerchant,
                        accountInfo = nanoParsed.accountInfo,
                        category = "Refund",
                        isExpense = false,
                        rawText = rawText,
                        isRefund = true
                    )
                    return@withContext handleRefund(context, refundParsed, sender, effectiveTimestamp, smsId, isBatchSync)
                }

                // If local parser detected a confirmed refund that AICore missed or labeled as credit, honor the refund
                if (localParsed?.isRefund == true && localParsed.amount > 0.0) {
                    Log.d(TAG, "Local parser detected confirmed refund where AICore classified as non-expense/credit, routing to handleRefund.")
                    return@withContext handleRefund(context, localParsed, sender, effectiveTimestamp, smsId, isBatchSync)
                }

                if (!nanoParsed.isExpense) {
                    Log.d(TAG, "SMS classified by AICore as non-expense (${nanoParsed.classification}), ignoring: '$rawText'")
                    return@withContext null
                }

                val parsed = ParsedSms(
                    amount = nanoParsed.amount,
                    currency = nanoParsed.currency,
                    type = nanoParsed.type,
                    title = nanoParsed.merchant,
                    accountInfo = nanoParsed.accountInfo,
                    category = nanoParsed.category,
                    isExpense = true,
                    rawText = rawText
                )
                return@withContext processAndInsertExpense(context, parsed, sender, effectiveTimestamp, smsId, isBatchSync, preloadedRules)
            }
        }

        // 3. Tier 3: Enhanced Local Regex Parser (100% offline, universal compatibility)
        if (localParsed != null) {
            if (localParsed.isRefund) {
                return@withContext handleRefund(context, localParsed, sender, effectiveTimestamp, smsId, isBatchSync)
            }
            if (localParsed.isExpense) {
                return@withContext processAndInsertExpense(context, localParsed, sender, effectiveTimestamp, smsId, isBatchSync, preloadedRules)
            }
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
        timestamp: Long,
        smsId: Long = 0L,
        isBatchSync: Boolean = false
    ): ExpenseEntity? = withContext(Dispatchers.IO) {
        val app = context.applicationContext as? SpendTrackerApplication ?: return@withContext null
        val dao = app.database.expenseDao()
        val prefs = app.preferences

        // 1. Fast-path smsId deduplication — skip if this SMS ID was already ingested
        if (smsId > 0L && dao.existsBySmsId(smsId)) {
            Log.d(TAG, "Fast-path smsId dedup in handleRefund: smsId=$smsId already stored, skipping.")
            return@withContext null
        }

        // 2. Exact content deduplication
        val existsExact = dao.existsByContent(sender, timestamp, parsed.amount)
        if (existsExact) {
            Log.d(TAG, "Duplicate refund SMS detected by exact content, skipping: sender=$sender, time=$timestamp, amount=${parsed.amount}")
            return@withContext null
        }

        // 3. Robust duplicate refund check within a 24-hour window
        val isDuplicate = dao.existsRefundDuplicate(
            amount = parsed.amount,
            rawBody = parsed.rawText,
            sender = sender,
            merchant = parsed.title,
            minTimestamp = timestamp - 86400000L,
            maxTimestamp = timestamp + 86400000L
        )
        if (isDuplicate) {
            Log.d(TAG, "Duplicate refund detected within 24h window, skipping: amount=${parsed.amount}, merchant=${parsed.title}")
            return@withContext null
        }

        // Look back up to 30 days for a matching debit transaction
        val lookbackMillis = 30L * 24 * 60 * 60 * 1000
        val minTimestamp = timestamp - lookbackMillis
        val maxTimestamp = timestamp + 3600000L

        val hasSpecificMerchant = SmsParser.isSpecificMerchant(parsed.title)

        val matching: ExpenseEntity? = if (hasSpecificMerchant) {
            dao.findMatchingDebitByMerchant(
                merchantKeyword = parsed.title.trim(),
                amount = parsed.amount,
                minTimestamp = minTimestamp,
                maxTimestamp = maxTimestamp
            )
        } else {
            dao.findMatchingDebitByAmount(
                amount = parsed.amount,
                minTimestamp = minTimestamp,
                maxTimestamp = maxTimestamp
            )
        }

        val preferredCurrency = prefs.currency.ifEmpty { parsed.currency }
        val effectiveMerchant = when {
            hasSpecificMerchant -> parsed.title.trim()
            matching != null && SmsParser.isSpecificMerchant(matching.merchantOrRecipient) -> matching.merchantOrRecipient.trim()
            else -> {
                val extracted = SmsParser.extractRefundMerchant(parsed.rawText)
                if (SmsParser.isSpecificMerchant(extracted)) {
                    extracted
                } else {
                    "Refund / Reversal"
                }
            }
        }
        val effectiveType = matching?.type ?: com.example.data.ExpenseType.MERCHANT

        val refundOriginal = when {
            hasSpecificMerchant -> parsed.title.trim()
            effectiveMerchant.isNotBlank() -> effectiveMerchant.trim()
            else -> "Refund / Reversal"
        }

        val effectiveCategory = if (matching != null && matching.category.isNotBlank() && !matching.category.equals("Refund", ignoreCase = true)) {
            matching.category
        } else {
            "Refund"
        }

        Log.d(TAG, "Recording refund transaction of ${parsed.amount} from '$effectiveMerchant'")
        val refundEntity = ExpenseEntity(
            smsId = smsId,
            amount = parsed.amount,
            currency = preferredCurrency,
            type = effectiveType,
            merchantOrRecipient = effectiveMerchant,
            originalMerchant = refundOriginal,
            accountInfo = parsed.accountInfo,
            category = effectiveCategory,
            rawBody = parsed.rawText,
            sender = sender,
            timestamp = timestamp,
            refundedAmount = 0.0,
            isReversal = true
        )
        val id = dao.insertExpense(refundEntity)
        val resultExpense = refundEntity.copy(id = id)

        // If a matching debit was identified, update its refunded amount
        if (matching != null) {
            dao.applyRefund(matching.id, parsed.amount)
        }

        if (!isBatchSync && prefs.isPersistentNotificationEnabled) {
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
        timestamp: Long = System.currentTimeMillis(),
        smsId: Long = 0L,
        isBatchSync: Boolean = false,
        preloadedRules: List<MerchantRuleEntity>? = null
    ): ExpenseEntity? = withContext(Dispatchers.IO) {
        val app = context.applicationContext as? SpendTrackerApplication ?: return@withContext null
        val dao = app.database.expenseDao()
        val ruleDao = app.database.merchantRuleDao()
        val prefs = app.preferences

        // 0. Fast-path smsId check (indexed O(1)) — primary deduplication guard
        if (smsId > 0L && dao.existsBySmsId(smsId)) {
            Log.d(TAG, "Fast-path smsId dedup in processAndInsert: smsId=$smsId already stored, skipping.")
            return@withContext null
        }

        // 1. Content-based dedup: same sender + ±5s timestamp + same amount
        val exists = dao.existsByContent(sender, timestamp, parsed.amount)
        if (exists) return@withContext null

        // 2. Raw-body fuzzy dedup for live SMS with smsId=0 that may race with syncInbox.
        //    Matches identical message body from the same sender within the last 60 seconds.
        if (smsId == 0L && parsed.rawText.length > 10) {
            val sixtySecondsAgo = timestamp - 60_000L
            val sixtySecondsAhead = timestamp + 60_000L
            if (dao.existsByRawBody(parsed.rawText, sender, sixtySecondsAgo, sixtySecondsAhead)) {
                Log.d(TAG, "Raw-body fuzzy dedup hit for sender=$sender, skipping duplicate.")
                return@withContext null
            }
        }

        val apiKey = prefs.openRouterApiKey.trim()
        val preferredCurrency = prefs.currency.ifEmpty { parsed.currency }

        var effectiveMerchant = parsed.title.trim()
        var finalCategory = parsed.category
        var isUnrecognized = false

        // 1. Auto-Rule & Merchant Alias Engine (Deterministic Local Classifier takes top precedence)
        val activeRules = preloadedRules ?: ruleDao.getAllRulesSync()
        var matchedRuleCategory: String? = null

        for (rule in activeRules) {
            val pattern = rule.merchantPattern.trim()
            if (pattern.isBlank()) continue
            val matches = if (rule.isRegex) {
                try {
                    val regex = Regex(pattern, RegexOption.IGNORE_CASE)
                    regex.containsMatchIn(effectiveMerchant) || regex.containsMatchIn(parsed.rawText)
                } catch (e: Exception) { false }
            } else {
                val clean = pattern.removePrefix("*").removeSuffix("*").trim()
                effectiveMerchant.contains(clean, ignoreCase = true) ||
                parsed.rawText.contains(clean, ignoreCase = true)
            }
            if (matches) {
                matchedRuleCategory = rule.assignedCategory
                if (rule.normalizedAlias.isNotBlank() && !rule.normalizedAlias.equals(rule.merchantPattern, ignoreCase = true)) {
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
            val rememberedCategory = prefs.getMerchantCategory(effectiveMerchant)?.let {
                OpenRouterCategorizer.normalizeCategory(it, effectiveMerchant, parsed.rawText)
            }
            if (!rememberedCategory.isNullOrBlank() && rememberedCategory != "Uncategorized") {
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
            } else if (AiCoreCategorizer.isAiCoreAvailable(context) && (finalCategory.isBlank() || finalCategory.equals("General", ignoreCase = true) || finalCategory.equals("Uncategorized", ignoreCase = true))) {
                val nanoResult = AiCoreCategorizer.categorizeSms(
                    context = context,
                    rawText = parsed.rawText,
                    merchant = effectiveMerchant,
                    amount = parsed.amount,
                    currency = preferredCurrency
                )
                if (nanoResult.category == "UNKNOWN") {
                    finalCategory = "Uncategorized"
                    isUnrecognized = true
                } else {
                    finalCategory = nanoResult.category
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

        finalCategory = OpenRouterCategorizer.normalizeCategory(finalCategory, effectiveMerchant, parsed.rawText)

        val lowerRaw = parsed.rawText.lowercase(Locale.US)
        val isSelfPayment = parsed.type == com.example.data.ExpenseType.SELF ||
                finalCategory.equals("Self", ignoreCase = true) ||
                lowerRaw.contains("to own account") || lowerRaw.contains("self transfer") ||
                lowerRaw.contains("transferred to your own") || lowerRaw.contains("linked account") ||
                lowerRaw.contains("between your accounts") || lowerRaw.contains("to self") ||
                lowerRaw.contains("transfer to self") || lowerRaw.contains("paid to self") ||
                lowerRaw.contains("to own a/c") || lowerRaw.contains("to self a/c") ||
                lowerRaw.contains("to my account")

        val isExplicitCardPurchase = (lowerRaw.contains(" at ") || lowerRaw.contains(" swiped ") ||
                lowerRaw.contains(" spent on card ") || lowerRaw.contains(" purchase ")) &&
                !lowerRaw.contains("towards") && !lowerRaw.contains("bill") && !lowerRaw.contains("cred")

        val isCreditCardPayment = !isExplicitCardPurchase && (
                parsed.type == com.example.data.ExpenseType.CREDIT_CARD ||
                finalCategory.equals("Credit Card Bill", ignoreCase = true) ||
                lowerRaw.contains("towards your credit card") ||
                lowerRaw.contains("towards credit card") ||
                lowerRaw.contains("credit card bill") ||
                lowerRaw.contains("card dues") ||
                lowerRaw.contains("paid to cred") ||
                lowerRaw.contains("payment to cred") ||
                lowerRaw.contains("bill payment for card") ||
                lowerRaw.contains("autopay for card") ||
                (lowerRaw.contains("payment received") && lowerRaw.contains("card"))
        )

        val resolvedType = when {
            isSelfPayment -> com.example.data.ExpenseType.SELF
            isCreditCardPayment -> com.example.data.ExpenseType.CREDIT_CARD
            else -> parsed.type
        }

        if (isSelfPayment) {
            finalCategory = "Self"
            isUnrecognized = false
            if (effectiveMerchant.isBlank() || effectiveMerchant.equals("Unknown", ignoreCase = true) || effectiveMerchant.equals("Merchant / Payee", ignoreCase = true)) {
                effectiveMerchant = "Self Transfer"
            }
        } else if (isCreditCardPayment) {
            finalCategory = "Credit Card Bill"
            isUnrecognized = false
            if (effectiveMerchant.isBlank() || effectiveMerchant.equals("Unknown", ignoreCase = true) || effectiveMerchant.equals("Merchant / Payee", ignoreCase = true)) {
                effectiveMerchant = "Credit Card Bill"
            }
        }

        // Deduplicate credit card bill repayment SMSs in the same month based on the amount
        if (isCreditCardPayment) {
            val monthKey = ExpenseEntity.formatMonthKey(timestamp)
            val duplicateCount = dao.countCreditCardPaymentsInMonth(monthKey, parsed.amount)
            if (duplicateCount > 0) {
                Log.d(TAG, "Deduplicating credit card payment of amount ${parsed.amount} in month $monthKey")
                return@withContext null
            }
        }

        val firstParsedMerchant = when {
            parsed.title.isNotBlank() && !parsed.title.equals("Merchant / Payee", ignoreCase = true) && parsed.title.any { it.isLetter() } -> parsed.title.trim()
            effectiveMerchant.isNotBlank() -> effectiveMerchant.trim()
            else -> "Unknown"
        }

        val entity = ExpenseEntity(
            smsId = smsId,
            amount = parsed.amount,
            currency = preferredCurrency,
            type = resolvedType,
            merchantOrRecipient = effectiveMerchant,
            originalMerchant = firstParsedMerchant,
            accountInfo = parsed.accountInfo,
            category = finalCategory,
            rawBody = parsed.rawText,
            sender = sender,
            timestamp = timestamp
        )

        val insertedId = dao.insertExpense(entity)
        val insertedExpense = entity.copy(id = insertedId)

        // Real-time alerts and guardrail checks (suppressed during batch sync to prevent notification storms & redundant I/O)
        if (!isBatchSync) {
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

            // 3. Proactive Velocity Pacing & Anomaly Guardrails
            if (prefs.isVelocityAlertsEnabled) {
                checkVelocityPacingAlert(context, entity)
            }
            if (prefs.isAnomalyAlertsEnabled) {
                checkAnomalySpikeAlert(context, entity)
            }
        }

        // 4. Update persistent notification (deferred during batch sync)
        if (!isBatchSync && prefs.isPersistentNotificationEnabled) {
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

    suspend fun checkVelocityPacingAlert(context: Context, expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        val app = context.applicationContext as? SpendTrackerApplication ?: return@withContext
        val dao = app.database.expenseDao()
        val prefs = app.preferences
        val budget = prefs.monthlyBudget
        if (budget <= 0.0) return@withContext

        val monthExpenses = dao.getExpensesForMonthSync(expense.monthKey)
        val blacklisted = prefs.getBlacklistedMerchants().map { it.trim().lowercase(java.util.Locale.US) }
        var currentSpent = 0.0
        for (exp in monthExpenses) {
            if (exp.isExcluded) continue
            val norm = exp.merchantOrRecipient.trim().lowercase(java.util.Locale.US)
            if (blacklisted.any { norm.contains(it) || it.contains(norm) }) continue
            if (exp.type == com.example.data.ExpenseType.SELF || exp.category.equals("Self", ignoreCase = true) ||
                exp.type == com.example.data.ExpenseType.CREDIT_CARD || exp.category.equals("Credit Card Bill", ignoreCase = true)
            ) continue

            if (exp.isRefundOrReversal) {
                currentSpent = (currentSpent - exp.amount).coerceAtLeast(0.0)
            } else {
                currentSpent += (exp.amount - exp.refundedAmount).coerceAtLeast(0.0)
            }
        }

        SpendAlertManager.checkAndNotifySpendVelocity(
            context = context,
            currentSpent = currentSpent,
            monthlyBudget = budget,
            currency = prefs.currency
        )
    }

    suspend fun checkAnomalySpikeAlert(context: Context, expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        val app = context.applicationContext as? SpendTrackerApplication ?: return@withContext
        val dao = app.database.expenseDao()
        val prefs = app.preferences

        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val recentAmounts = dao.getRecentDebitAmounts(thirtyDaysAgo)
        val median = if (recentAmounts.isEmpty()) 500.0 else {
            val mid = recentAmounts.size / 2
            if (recentAmounts.size % 2 == 0 && mid > 0) {
                (recentAmounts[mid - 1] + recentAmounts[mid]) / 2.0
            } else {
                recentAmounts[mid]
            }
        }

        SpendAlertManager.checkAndNotifyHighValueAnomaly(
            context = context,
            expense = expense,
            medianAmount = median,
            currency = prefs.currency
        )
    }
}
