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
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            return@withContext null
        }

        val bearer = if (cleanKey.startsWith("Bearer ")) cleanKey else "Bearer $cleanKey"

        val systemPrompt = """
You are an expert financial transaction intelligence and validation engine for Savio₹ personal expense tracker.
Your mission: Analyze the incoming SMS message, validate whether it represents an ACTUAL OUTGOING EXPENDITURE, and intelligently enhance the transaction details into clean, structured JSON.

VALIDATION & CLASSIFICATION CRITERIA:
1. "MERCHANT": An actual outgoing payment, purchase, or debit made to a business, merchant, store, utility, online service, restaurant, groceries, or vendor (e.g. Swiggy, Amazon, Uber, POS swipe, merchant UPI).
2. "P2P": Outgoing money transferred to another person, friend, contact, family member, landlord, peer-to-peer UPI transfer, NEFT, IMPS, or wire.
3. "SELF": Outgoing transfer between own accounts (e.g. self transfer, account-to-account transfer).
4. "CREDIT_CARD": Payment made towards a credit card bill, card statement repayment, or credit card dues.
5. "CREDIT": Incoming money credited, deposited, salary received, refund, cashback, or loan disbursement -> (isExpense: FALSE).
6. "INTIMATION": Non-transactional bank notification, available balance update, credit limit alert, statement generated, bill due reminder -> (isExpense: FALSE).
7. "AD": Promotional advertisement, credit card offer, loan pre-approval, cashback scheme, marketing -> (isExpense: FALSE).
8. "OTP": One-time password, verification PIN, security code -> (isExpense: FALSE).
9. "OTHER": Irrelevant spam, personal message, or unidentifiable message -> (isExpense: FALSE).

ENHANCEMENT RULES:
- Amount: Extract the exact numerical value of the transaction. Never truncate or misread digits (e.g., Rs.50000.00 is 50000.00, Rs.500.00 is 500.00). Do not confuse with available balance!
- Currency: Detect currency symbol ("₹", "$", "€", "£", etc.). Default to "₹" for Indian banking SMS.
- Merchant: Extract the actual person or business being paid.
  CRITICAL: NEVER use the message sender, carrier, or bank name (e.g. HDFC, ICICI, SBI, AXIS, KOTAK, Bank, VM-HDFCBK) as the merchant name. Always parse the actual person, store, or service being paid from the message body (e.g. from 'to VPA', 'paid to', 'spent at', 'transfer to', etc.). If it's a self-transfer, use 'Self Transfer'. If it's a credit card bill, use 'Credit Card Bill'. If unknown, use 'Merchant / Payee' or 'Transfer Recipient'.
- AccountInfo: Detect card or account info (e.g. 'Card ••1234', 'A/c ••5678', 'UPI ••9012').
- Category: Assign the most accurate category from:
  ["Transfers", "Credit Card Bill", "Self", "Groceries", "Food & Dining", "Shopping", "Bills & Utilities", "Travel & Commute", "Entertainment", "Health & Wellness", "Investments", "Education", "Personal Care"].
  * NEVER use "UPI" as a category. UPI is strictly a payment method.
  * If paying a credit card bill, categorize as "Credit Card Bill".
  * If transferring to own account, categorize as "Self".
  * If transferring to another person/contact, categorize as "Transfers".

OUTPUT FORMAT (Output STRICT RAW JSON ONLY, no markdown fences, no code blocks):
{
  "classification": "MERCHANT" | "P2P" | "SELF" | "CREDIT_CARD" | "CREDIT" | "INTIMATION" | "AD" | "OTP" | "OTHER",
  "amount": 1250.00,
  "currency": "₹",
  "merchant": "Clean Merchant or Recipient Name",
  "accountInfo": "Card ••1234 or A/c ••5678 or UPI ••9012",
  "category": "Category Name"
}
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
            if (category.equals("UNKNOWN", ignoreCase = true) || category.isBlank() || category.equals("UPI", ignoreCase = true)) {
                category = if (classification == "P2P" || classification == "TRANSFER") "Transfers"
                           else if (classification == "SELF") "Self"
                           else if (classification == "CREDIT_CARD") "Credit Card Bill"
                           else "General Spend"
            }

            val isExpense = (classification == "MERCHANT" || classification == "SPEND" ||
                             classification == "P2P" || classification == "TRANSFER" ||
                             classification == "SELF" || classification == "CREDIT_CARD") && amount > 0.0

            val expenseType = when (classification) {
                "P2P", "TRANSFER" -> ExpenseType.P2P
                "SELF" -> ExpenseType.SELF
                "CREDIT_CARD" -> ExpenseType.CREDIT_CARD
                else -> {
                    if (category.equals("Self", ignoreCase = true)) ExpenseType.SELF
                    else if (category.equals("Credit Card Bill", ignoreCase = true)) ExpenseType.CREDIT_CARD
                    else ExpenseType.MERCHANT
                }
            }

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
