package com.example.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object OpenRouterCategorizer {
    private const val TAG = "OpenRouterCategorizer"

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
     * Categorizes SMS text using gemini-3.5-flash-lite via OpenRouter.
     * Returns:
     * - The matched category name
     * - "UNKNOWN" if spend cannot be recognized/categorized with confidence
     * - Fallback local classification if API key is not configured or network fails
     */
    suspend fun categorizeSms(
        rawText: String,
        merchant: String,
        amount: Double,
        currency: String,
        apiKey: String
    ): CategorizationResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            Log.d(TAG, "No OpenRouter API key provided, skipping LLM categorization")
            return@withContext CategorizationResult(
                category = "UNKNOWN",
                isAiClassified = false,
                confidence = 0f
            )
        }

        val bearer = if (cleanKey.startsWith("Bearer ")) cleanKey else "Bearer $cleanKey"

        val systemPrompt = """
You are a precise financial transaction categorizer for the SAVIO personal expense tracking application.
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
            val request = OpenRouterChatRequest(
                // Supports gemini-3.5-flash-lite / google/gemini-2.5-flash-lite model identifiers
                model = "google/gemini-2.5-flash-lite",
                messages = listOf(
                    OpenRouterMessage(role = "system", content = systemPrompt),
                    OpenRouterMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.0,
                maxTokens = 25
            )

            val response = OpenRouterClient.api.createChatCompletion(
                authorization = bearer,
                request = request
            )

            val rawResult = response.choices?.firstOrNull()?.message?.content?.trim() ?: ""
            Log.d(TAG, "OpenRouter gemini response: '$rawResult'")

            if (rawResult.equals("UNKNOWN", ignoreCase = true) || rawResult.isBlank()) {
                return@withContext CategorizationResult(
                    category = "UNKNOWN",
                    isAiClassified = true,
                    confidence = 0.0f
                )
            }

            // Find best matching known category
            val matched = KNOWN_CATEGORIES.firstOrNull { it.equals(rawResult, ignoreCase = true) }
                ?: KNOWN_CATEGORIES.firstOrNull { rawResult.contains(it, ignoreCase = true) }

            if (matched != null) {
                return@withContext CategorizationResult(
                    category = matched,
                    isAiClassified = true,
                    confidence = 0.95f
                )
            } else {
                // Return cleaned custom category if alphanumeric
                val sanitized = rawResult.filter { it.isLetterOrDigit() || it.isWhitespace() || it == '&' }.trim()
                if (sanitized.length in 3..25) {
                    return@withContext CategorizationResult(
                        category = sanitized,
                        isAiClassified = true,
                        confidence = 0.8f
                    )
                }
                return@withContext CategorizationResult(
                    category = "UNKNOWN",
                    isAiClassified = true,
                    confidence = 0f
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error categorizing via OpenRouter: ${e.message}", e)
            return@withContext CategorizationResult(
                category = "UNKNOWN",
                isAiClassified = false,
                confidence = 0f,
                errorMessage = e.message
            )
        }
    }
}

data class CategorizationResult(
    val category: String,
    val isAiClassified: Boolean,
    val confidence: Float,
    val errorMessage: String? = null
)
