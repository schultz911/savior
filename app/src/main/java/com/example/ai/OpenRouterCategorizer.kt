package com.example.ai

import android.util.Log
import com.example.data.ExpenseType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

data class AiParsedTransaction(
    val classification: String, // "SPEND", "TRANSFER", "CREDIT", "INTIMATION", "AD", "OTP", "OTHER"
    val isExpense: Boolean,     // true only for SPEND or TRANSFER
    val type: ExpenseType,      // SPEND or TRANSFER
    val amount: Double,
    val currency: String,
    val merchant: String,
    val accountInfo: String,
    val category: String,
    val isAiClassified: Boolean,
    val rawText: String
)

data class CategorizationResult(
    val category: String,
    val isAiClassified: Boolean,
    val confidence: Float,
    val errorMessage: String? = null
)

object OpenRouterCategorizer {
    private const val TAG = "OpenRouterCategorizer"
    const val DEFAULT_MODEL = "google/gemini-3.5-flash-lite"

    // Standard SAVIO spend categories
    val KNOWN_CATEGORIES = listOf(
        "Groceries",
        "Food & Dining",
        "Shopping",
        "Bills & Utilities",
        "Travel & Commute",
        "Transfers",
        "Entertainment",
        "Health & Wellness",
        "Investments",
        "Education",
        "Personal Care"
    )

    /**
     * Parses an SMS message using OpenRouter (gemini-3.5-flash-lite).
     * Determines whether message is credit, debit/spend, transfer, intimation, ad, OTP, etc.
     * Extracts merchant, amount, type, and category.
     */
    suspend fun parseSmsTransaction(
        rawText: String,
        sender: String = "",
        apiKey: String,
        model: String = DEFAULT_MODEL
    ): AiParsedTransaction? = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            return@withContext null
        }

        val bearer = if (cleanKey.startsWith("Bearer ")) cleanKey else "Bearer $cleanKey"

        val systemPrompt = """
You are a financial transaction and SMS classification engine for Savio₹ personal expense tracker.
Analyze the provided SMS message and extract structured financial information in strict JSON.

Classification rules:
1. "SPEND": Outgoing payment or purchase made to a merchant, company, utility, shop, or online service.
2. "TRANSFER": Money sent/transferred to another person or individual (e.g. UPI transfer, P2P, friend/family, rent).
3. "CREDIT": Incoming funds deposited, salary credited, refund received, cashback.
4. "INTIMATION": Non-transactional bank alert, account balance notice, statement generated, credit limit alert, bill due reminder.
5. "AD": Promotional advertising, loan offer, credit card sale, pre-approved offer, discount coupon.
6. "OTP": One-time password, verification code, authorization PIN, security code.
7. "OTHER": Spam or unidentifiable message.

Categories (choose most accurate for SPEND/TRANSFER):
Groceries, Food & Dining, Shopping, Bills & Utilities, Travel & Commute, Transfers, Entertainment, Health & Wellness, Investments, Education, Personal Care, or UNKNOWN.

Output format (MUST BE RAW VALID JSON ONLY, no backticks, no markdown):
{
  "classification": "SPEND" | "TRANSFER" | "CREDIT" | "INTIMATION" | "AD" | "OTP" | "OTHER",
  "amount": 50000.00,
  "currency": "₹",
  "merchant": "Exact Merchant or Recipient Name",
  "accountInfo": "Card ••1234 or A/c ••5678",
  "category": "Category Name"
}
If amount cannot be identified, set amount to 0.0.
""".trimIndent()

        val userPrompt = """
Sender: "$sender"
SMS Body: "$rawText"
""".trimIndent()

        try {
            val chosenModel = if (model.isNotBlank()) model else DEFAULT_MODEL
            val request = OpenRouterChatRequest(
                model = chosenModel,
                messages = listOf(
                    OpenRouterMessage(role = "system", content = systemPrompt),
                    OpenRouterMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.0,
                maxTokens = 200
            )

            val response = OpenRouterClient.api.createChatCompletion(
                authorization = bearer,
                request = request
            )

            val rawResult = response.choices?.firstOrNull()?.message?.content?.trim() ?: ""
            Log.d(TAG, "OpenRouter full SMS parse response: '$rawResult'")

            if (rawResult.isBlank()) return@withContext null

            // Clean json response if wrapped in codeblocks
            val cleanedJson = rawResult
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val json = JSONObject(cleanedJson)
            val classification = json.optString("classification", "OTHER").uppercase(Locale.US)
            val amount = json.optDouble("amount", 0.0)
            val currency = json.optString("currency", "₹").ifEmpty { "₹" }
            val merchant = json.optString("merchant", "Unknown").ifEmpty { "Unknown" }
            val accountInfo = json.optString("accountInfo", "")
            var category = json.optString("category", "General")
            if (category.equals("UNKNOWN", ignoreCase = true) || category.isBlank()) {
                category = "Uncategorized"
            }

            val isExpense = (classification == "SPEND" || classification == "TRANSFER") && amount > 0.0
            val expenseType = if (classification == "TRANSFER") ExpenseType.TRANSFER else ExpenseType.SPEND

            AiParsedTransaction(
                classification = classification,
                isExpense = isExpense,
                type = expenseType,
                amount = amount,
                currency = currency,
                merchant = merchant,
                accountInfo = accountInfo,
                category = category,
                isAiClassified = true,
                rawText = rawText
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in AI full parse: ${e.message}", e)
            null
        }
    }

    /**
     * Categorizes SMS text using gemini-3.5-flash-lite via OpenRouter.
     */
    suspend fun categorizeSms(
        rawText: String,
        merchant: String,
        amount: Double,
        currency: String,
        apiKey: String,
        model: String = DEFAULT_MODEL
    ): CategorizationResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            return@withContext CategorizationResult(
                category = "UNKNOWN",
                isAiClassified = false,
                confidence = 0f
            )
        }

        val bearer = if (cleanKey.startsWith("Bearer ")) cleanKey else "Bearer $cleanKey"

        val systemPrompt = """
You are a precise financial transaction categorizer for the Savio₹ personal expense tracking application.
Analyze the bank SMS and extract the exact spend category.
Permitted categories:
- Groceries
- Food & Dining
- Shopping
- Bills & Utilities
- Travel & Commute
- Transfers
- Entertainment
- Health & Wellness
- Investments
- Education
- Personal Care

Rules:
1. If the message clearly belongs to one of the above categories, output ONLY the exact category name.
2. If the message is ambiguous, generic, cannot be determined, or is not an identifiable purchase, output ONLY "UNKNOWN".
3. Do not add explanations, prefixes, punctuation or quotes.
""".trimIndent()

        val userPrompt = """
SMS Text: "$rawText"
Extracted Merchant/Entity: "$merchant"
Amount: $currency$amount

Output category:
""".trimIndent()

        try {
            val chosenModel = if (model.isNotBlank()) model else DEFAULT_MODEL
            val request = OpenRouterChatRequest(
                model = chosenModel,
                messages = listOf(
                    OpenRouterMessage(role = "system", content = systemPrompt),
                    OpenRouterMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.0,
                maxTokens = 30
            )

            val response = OpenRouterClient.api.createChatCompletion(
                authorization = bearer,
                request = request
            )

            val rawResult = response.choices?.firstOrNull()?.message?.content?.trim() ?: ""

            if (rawResult.equals("UNKNOWN", ignoreCase = true) || rawResult.isBlank()) {
                return@withContext CategorizationResult(
                    category = "UNKNOWN",
                    isAiClassified = true,
                    confidence = 0.0f
                )
            }

            val matched = KNOWN_CATEGORIES.firstOrNull { it.equals(rawResult, ignoreCase = true) }
                ?: KNOWN_CATEGORIES.firstOrNull { rawResult.contains(it, ignoreCase = true) }

            if (matched != null) {
                CategorizationResult(
                    category = matched,
                    isAiClassified = true,
                    confidence = 0.95f
                )
            } else {
                val sanitized = rawResult.filter { it.isLetterOrDigit() || it.isWhitespace() || it == '&' }.trim()
                if (sanitized.length in 3..25) {
                    CategorizationResult(
                        category = sanitized,
                        isAiClassified = true,
                        confidence = 0.8f
                    )
                } else {
                    CategorizationResult(
                        category = "UNKNOWN",
                        isAiClassified = true,
                        confidence = 0f
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error categorizing via OpenRouter: ${e.message}", e)
            CategorizationResult(
                category = "UNKNOWN",
                isAiClassified = false,
                confidence = 0f,
                errorMessage = e.message
            )
        }
    }
}
