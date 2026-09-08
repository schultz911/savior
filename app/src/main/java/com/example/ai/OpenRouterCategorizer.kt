package com.example.ai

import android.util.Log
import com.example.data.ExpenseType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

data class AiParsedTransaction(
    val classification: String, // "MERCHANT", "P2P", "SELF", "CREDIT_CARD", "CREDIT", "REFUND", "INTIMATION", "AD", "OTP", "OTHER"
    val isExpense: Boolean,     // true only for outgoing expenditure types
    val isRefund: Boolean = false, // true for REFUND classification
    val type: ExpenseType,      // MERCHANT, P2P, SELF, or CREDIT_CARD
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
    const val FALLBACK_MODEL = "google/gemini-2.5-flash-lite"

    // Standard SAVIO spend categories
    val KNOWN_CATEGORIES = listOf(
        "Transfers",
        "Credit Card Bill",
        "Self",
        "Groceries",
        "Food & Dining",
        "Shopping",
        "Bills & Utilities",
        "Travel & Commute",
        "Entertainment",
        "Health & Wellness",
        "Investments",
        "Education",
        "Personal Care"
    )

    private suspend fun executeChatCompletion(
        bearer: String,
        chosenModel: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int
    ): OpenRouterChatResponse {
        val reasoningConfig = if (chosenModel.contains("gemini", ignoreCase = true) || chosenModel.contains("reasoning", ignoreCase = true)) {
            OpenRouterReasoning(effort = "minimal")
        } else {
            null
        }
        val request = OpenRouterChatRequest(
            model = chosenModel,
            messages = listOf(
                OpenRouterMessage(role = "system", content = systemPrompt),
                OpenRouterMessage(role = "user", content = userPrompt)
            ),
            temperature = 0.0,
            maxTokens = maxTokens,
            reasoning = reasoningConfig
        )
        return try {
            OpenRouterClient.api.createChatCompletion(
                authorization = bearer,
                request = request
            )
        } catch (e: retrofit2.HttpException) {
            if ((e.code() == 404 || e.code() == 400) && chosenModel == DEFAULT_MODEL) {
                Log.w(TAG, "Model $chosenModel returned HTTP ${e.code()}, retrying with fallback model $FALLBACK_MODEL")
                val fallbackReasoning = OpenRouterReasoning(effort = "minimal")
                OpenRouterClient.api.createChatCompletion(
                    authorization = bearer,
                    request = request.copy(model = FALLBACK_MODEL, reasoning = fallbackReasoning)
                )
            } else {
                throw e
            }
        }
    }

    /**
     * Parses and validates an SMS message using OpenRouter (gemini-3.5-flash-lite).
     * Rigorously confirms whether message is an actual OUTGOING EXPENDITURE vs CREDIT/INTIMATION/AD/OTP.
     * Intelligently cleans and enhances merchant name, amount, account reference, and category.
     */
    suspend fun parseSmsTransaction(
        rawText: String,
        sender: String = "",
        apiKey: String,
        model: String = DEFAULT_MODEL
    ): AiParsedTransaction? = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim().removeSurrounding("\"").removeSurrounding("'").trim()
        if (cleanKey.isEmpty()) {
            return@withContext null
        }

        val bearer = if (cleanKey.startsWith("Bearer ", ignoreCase = true)) cleanKey else "Bearer $cleanKey"

        val systemPrompt = """
You are an expert financial transaction extraction engine for Savio₹ personal expense tracker.
Your mission: Analyze the incoming SMS message, extract all transaction details accurately into STRICT JSON.

CLASSIFICATION & VALIDATION RULES:
1. "MERCHANT": An actual outgoing payment, purchase, or debit made to a store, business, vendor, restaurant, app, utility, or service.
   CRITICAL — PURCHASES MADE USING A CREDIT CARD ARE "MERCHANT" PURCHASES!
   When an SMS indicates that money was spent, charged, debited, or paid USING a credit card at a merchant (e.g. Swiggy, Amazon, Uber, restaurant, supermarket, retail store, online purchase):
   - classification is ALWAYS "MERCHANT" (NOT "CREDIT_CARD"!)
   - isExpense is true
   - merchant is the EXACT merchant/store name from the SMS (e.g. "SWIGGY BANGALORE", "AMAZON INDIA")
   - accountInfo is the card reference, e.g. "Card ••4821"
   - category is what was bought (e.g. "Food & Dining", "Shopping", "Groceries", "Travel & Commute"), NEVER "Credit Card Bill"!

2. "CREDIT_CARD": ONLY for paying off a credit card bill, card statement dues, or card account repayment (e.g. "Payment received towards your credit card ending 4821", "Auto-debit for credit card bill successful", "Bill payment for card ending 1234").
   - classification is "CREDIT_CARD"
   - isExpense is true
   - merchant is the credit card bill name (e.g. "HDFC Credit Card Bill")
   - category is "Credit Card Bill"

3. "P2P": Outgoing money transferred to another person/contact via UPI, NEFT, IMPS, Zelle, Venmo, or wire.
   - classification is "P2P"
   - isExpense is true
   - merchant is the recipient's exact name or VPA handle (e.g. "Ramesh Kumar" or "rahul@okaxis")
   - category is "Transfers"

4. "SELF": Transfer between own bank accounts.
   - classification is "SELF"
   - isExpense is true
   - merchant is "Self Transfer"
   - category is "Self"

5. "REFUND": A confirmed refund or reversal credited back from a merchant.
   - classification is "REFUND"
   - isExpense is false
   - merchant is the exact company/service that issued the refund (e.g. "Swiggy", "Zomato", "Amazon")
   - category is "Refund"

6. "CREDIT": Salary, general deposit, interest credited. (isExpense: false)
7. "INTIMATION", "AD", "OTP", "OTHER": Non-transactional bank notifications, balance alerts, ads, OTPs. (isExpense: false)

MERCHANT EXTRACTION RULES:
- Extract the EXACT merchant, vendor, store, or recipient name VERBATIM as written in the SMS (e.g. "SWIGGY BANGALORE", "AMAZON INDIA", "BLUE TOKAI COFFEE", "SHELL PETROL PUMP", "ZEPTO COMMERCE").
- DO NOT summarize, abbreviate, titlecase, or replace with generic brand names. Keep the exact merchant name from the SMS text because the user will assign aliases themselves in the app.
- NEVER use the bank or carrier sender name (e.g. HDFC, ICICI, SBI, AXIS, KOTAK, Bank, VM-HDFCBK) as the merchant name.
- If paying a credit card bill: use the card bill name (e.g. "HDFC Credit Card Bill").
- If transferring to self: use "Self Transfer".
- If unknown: use "Merchant / Payee" or "Transfer Recipient".

ACCOUNT INFO:
- Detect card or account info with 4-digit mask (e.g. "Card ••4821", "A/c ••3391", "UPI ••9012").

PERMITTED CATEGORIES:
["Transfers", "Credit Card Bill", "Self", "Groceries", "Food & Dining", "Shopping", "Bills & Utilities", "Travel & Commute", "Entertainment", "Health & Wellness", "Investments", "Education", "Personal Care"]
- NEVER use "Credit Card Bill" for purchases made at a store/merchant with a credit card!

FEW-SHOT EXAMPLES:
SMS: "Spent INR 6,890.00 on Axis Card ending 1004 at AMAZON INDIA on 01-Sep. Avl Limit: Rs 1,85,000.00."
Output: {"classification":"MERCHANT","amount":6890.00,"currency":"₹","merchant":"AMAZON INDIA","accountInfo":"Card ••1004","category":"Shopping"}

SMS: "Thank you for using HDFC Bank Credit Card ending 4821 for payment of Rs 1,450.00 at SWIGGY BANGALORE on 04-Sep. Avl Limit: Rs 48,250.00."
Output: {"classification":"MERCHANT","amount":1450.00,"currency":"₹","merchant":"SWIGGY BANGALORE","accountInfo":"Card ••4821","category":"Food & Dining"}

SMS: "Payment received of INR 8,500.00 towards your HDFC Bank Credit Card ending 4821 on 02-Sep."
Output: {"classification":"CREDIT_CARD","amount":8500.00,"currency":"₹","merchant":"HDFC Credit Card Bill","accountInfo":"Card ••4821","category":"Credit Card Bill"}

SMS: "Auto-debit of Rs 12,300.00 towards your SBI Credit Card ending 5512 was successful."
Output: {"classification":"CREDIT_CARD","amount":12300.00,"currency":"₹","merchant":"SBI Credit Card Bill","accountInfo":"Card ••5512","category":"Credit Card Bill"}

SMS: "Dear SBI User, your A/c XX3391 debited by Rs 5,000.00 on 03-Sep towards Transfer to Ramesh Kumar. UPI Ref 382910."
Output: {"classification":"P2P","amount":5000.00,"currency":"₹","merchant":"Ramesh Kumar","accountInfo":"A/c ••3391","category":"Transfers"}

SMS: "Rs 650.00 refunded to A/c ending 9821 from Swiggy for cancelled order. UPI Ref: 778899."
Output: {"classification":"REFUND","amount":650.00,"currency":"₹","merchant":"Swiggy","accountInfo":"A/c ••9821","category":"Refund"}

OUTPUT FORMAT (Output STRICT RAW JSON ONLY, no markdown fences, no code blocks):
{"classification":"...","amount":0.0,"currency":"₹","merchant":"...","accountInfo":"...","category":"..."}
""".trimIndent()

        val userPrompt = """
Sender: "$sender"
SMS Body: "$rawText"
""".trimIndent()

        try {
            val chosenModel = if (model.isNotBlank()) model.trim() else DEFAULT_MODEL
            val response = executeChatCompletion(
                bearer = bearer,
                chosenModel = chosenModel,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                maxTokens = 1000
            )

            val choice = response.choices?.firstOrNull()?.message
            val contentText = choice?.content?.trim() ?: ""
            val reasoningText = choice?.reasoning?.trim() ?: ""
            val rawResult = if (contentText.isNotBlank()) contentText else reasoningText
            Log.d(TAG, "OpenRouter full SMS parse response: '$rawResult'")

            if (rawResult.isBlank()) return@withContext null

            // Robust JSON boundary extraction — handles markdown fences, preambles, and trailing text
            val cleanedJson = rawResult
                .replace("```json", "")
                .replace("```", "")
                .trim()
            val jsonStart = cleanedJson.indexOf('{')
            val jsonEnd = cleanedJson.lastIndexOf('}')
            if (jsonStart < 0 || jsonEnd < 0 || jsonEnd <= jsonStart) return@withContext null
            val boundedJson = cleanedJson.substring(jsonStart, jsonEnd + 1)

            val json = JSONObject(boundedJson)
            val classification = json.optString("classification", "OTHER").uppercase(Locale.US)
            val rawAmount = json.opt("amount")
            val amount = when (rawAmount) {
                is Number -> rawAmount.toDouble()
                is String -> rawAmount.replace(",", "").replace("₹", "").replace("$", "")
                    .replace("Rs.", "", ignoreCase = true).replace("Rs", "", ignoreCase = true)
                    .trim().toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val currency = json.optString("currency", "₹").ifEmpty { "₹" }
            val merchant = json.optString("merchant", "Unknown").ifEmpty { "Unknown" }
            val accountInfo = json.optString("accountInfo", "")
            var category = json.optString("category", "General")
            if (category.equals("UNKNOWN", ignoreCase = true) || category.isBlank() || category.equals("UPI", ignoreCase = true)) {
                category = if (classification == "P2P" || classification == "TRANSFER") "Transfers"
                           else if (classification == "SELF") "Self"
                           else if (classification == "CREDIT_CARD") "Credit Card Bill"
                           else "General Spend"
            }

            val isRefund = classification == "REFUND" || classification == "REVERSAL"
            val isExpense = (classification == "MERCHANT" || classification == "SPEND" ||
                             classification == "P2P" || classification == "TRANSFER" ||
                             classification == "SELF" || classification == "CREDIT_CARD" ||
                             classification == "DEBIT" || classification == "EXPENSE" ||
                             classification == "PAYMENT" || classification == "PURCHASE") && amount > 0.0

            val expenseType = when (classification) {
                "MERCHANT", "SPEND", "DEBIT", "PURCHASE" -> ExpenseType.MERCHANT
                "P2P", "TRANSFER" -> ExpenseType.P2P
                "SELF" -> ExpenseType.SELF
                "CREDIT_CARD" -> ExpenseType.CREDIT_CARD
                else -> {
                    if (category.equals("Self", ignoreCase = true)) ExpenseType.SELF
                    else if (category.equals("Credit Card Bill", ignoreCase = true)) ExpenseType.CREDIT_CARD
                    else if (category.equals("Transfers", ignoreCase = true)) ExpenseType.P2P
                    else ExpenseType.MERCHANT
                }
            }

            if (expenseType == ExpenseType.MERCHANT && category.equals("Credit Card Bill", ignoreCase = true)) {
                category = "General Spend"
            }

            AiParsedTransaction(
                classification = classification,
                isExpense = isExpense,
                isRefund = isRefund,
                type = expenseType,
                amount = amount,
                currency = currency,
                merchant = merchant,
                accountInfo = accountInfo,
                category = category,
                isAiClassified = true,
                rawText = rawText
            )
        } catch (e: retrofit2.HttpException) {
            val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            Log.e(TAG, "OpenRouter HTTP ${e.code()} parse error: $errorBody", e)
            null
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
        val cleanKey = apiKey.trim().removeSurrounding("\"").removeSurrounding("'").trim()
        if (cleanKey.isEmpty()) {
            return@withContext CategorizationResult(
                category = "UNKNOWN",
                isAiClassified = false,
                confidence = 0f
            )
        }

        val bearer = if (cleanKey.startsWith("Bearer ", ignoreCase = true)) cleanKey else "Bearer $cleanKey"

        val systemPrompt = """
You are a precise financial transaction categorizer for the Savio₹ personal expense tracking application.
Analyze the bank SMS and extract the exact spend category.
Permitted categories:
- Transfers
- Credit Card Bill
- Self
- Groceries
- Food & Dining
- Shopping
- Bills & Utilities
- Travel & Commute
- Entertainment
- Health & Wellness
- Investments
- Education
- Personal Care

Rules:
1. If the message clearly belongs to one of the above categories, output ONLY the exact category name.
2. Never use "UPI" as a category. UPI is only a payment method.
3. If the message is ambiguous, generic, cannot be determined, or is not an identifiable purchase, output ONLY "UNKNOWN".
4. Do not add explanations, prefixes, punctuation or quotes.
5. CRITICAL: A purchase made USING a credit card at a store or merchant (e.g. Swiggy, Amazon, Uber, restaurant, supermarket) MUST be categorized by what was bought (e.g. Food & Dining, Shopping, Travel & Commute). NEVER categorize a purchase as "Credit Card Bill" just because a credit card was used! Use "Credit Card Bill" ONLY when paying off the credit card bill itself.
""".trimIndent()

        val userPrompt = """
SMS Text: "$rawText"
Extracted Merchant/Entity: "$merchant"
Amount: $currency$amount

Output category:
""".trimIndent()

        try {
            val chosenModel = if (model.isNotBlank()) model.trim() else DEFAULT_MODEL
            val response = executeChatCompletion(
                bearer = bearer,
                chosenModel = chosenModel,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                maxTokens = 500
            )

            val choice = response.choices?.firstOrNull()?.message
            val contentText = choice?.content?.trim() ?: ""
            val reasoningText = choice?.reasoning?.trim() ?: ""
            val rawResult = if (contentText.isNotBlank()) contentText else reasoningText

            if (rawResult.equals("UNKNOWN", ignoreCase = true) || rawResult.isBlank()) {
                return@withContext CategorizationResult(
                    category = "UNKNOWN",
                    isAiClassified = true,
                    confidence = 0.0f
                )
            }

            val cleaned = rawResult.removeSurrounding("\"").removeSurrounding("'").replace("```", "").trim()
            val matched = KNOWN_CATEGORIES.firstOrNull { it.equals(cleaned, ignoreCase = true) }
                ?: KNOWN_CATEGORIES.firstOrNull { cleaned.contains(it, ignoreCase = true) }

            if (matched != null) {
                CategorizationResult(
                    category = matched,
                    isAiClassified = true,
                    confidence = 0.95f
                )
            } else {
                val sanitized = cleaned.filter { it.isLetterOrDigit() || it.isWhitespace() || it == '&' }.trim()
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
        } catch (e: retrofit2.HttpException) {
            val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            Log.e(TAG, "OpenRouter HTTP ${e.code()} categorization error: $errorBody", e)
            CategorizationResult(
                category = "UNKNOWN",
                isAiClassified = false,
                confidence = 0f,
                errorMessage = "HTTP ${e.code()}: $errorBody"
            )
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
