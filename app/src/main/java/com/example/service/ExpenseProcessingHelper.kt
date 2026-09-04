package com.example.service

import android.content.Context
import com.example.SpendTrackerApplication
import com.example.ai.OpenRouterCategorizer
import com.example.data.ExpenseEntity
import com.example.sms.ParsedSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExpenseProcessingHelper {

    /**
     * Categorizes using remembered merchant rules, OpenRouter gemini-3.5-flash-lite,
     * or heuristic classification. Inserts the expense, alerts if category is UNKNOWN,
     * alerts if 80% or 100% of category limit is reached, and updates notification.
     */
    suspend fun processAndInsertExpense(
        context: Context,
        parsed: ParsedSms,
        sender: String,
        timestamp: Long = System.currentTimeMillis()
    ): ExpenseEntity? = withContext(Dispatchers.IO) {
        val app = context.applicationContext as? SpendTrackerApplication ?: return@withContext null
        val dao = app.database.expenseDao()
        val prefs = app.preferences

        val exists = dao.existsByContent(sender, timestamp, parsed.amount)
        if (exists) return@withContext null

        val apiKey = prefs.openRouterApiKey.trim()
        val preferredCurrency = prefs.currency.ifEmpty { parsed.currency }

        var finalCategory = parsed.category
        var isUnrecognized = false

        // Check if user previously mapped this merchant to a category
        val rememberedCategory = prefs.getMerchantCategory(parsed.title)
        if (!rememberedCategory.isNullOrBlank()) {
            finalCategory = rememberedCategory
            isUnrecognized = false
        } else if (apiKey.isNotEmpty()) {
            val aiResult = OpenRouterCategorizer.categorizeSms(
                rawText = parsed.rawText,
                merchant = parsed.title,
                amount = parsed.amount,
                currency = preferredCurrency,
                apiKey = apiKey
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

        val entity = ExpenseEntity(
            amount = parsed.amount,
            currency = preferredCurrency,
            type = parsed.type,
            merchantOrRecipient = parsed.title,
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
