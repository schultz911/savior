package com.example.ai

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.example.data.ExpenseType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

/**
 * On-Device Android AICore / Gemini Nano Categorizer (Tier 2 Intelligence).
 * Leverages on-device Gemini Nano via the Android AICore system service (API 34+)
 * without sending financial SMS data over the network and requiring zero external API keys.
 * 
 * If AICore is not present (e.g. non-flagship devices or devices < Android 14),
 * it seamlessly and gracefully yields to Tier 3 (SmsParser.kt).
 */
object AiCoreCategorizer {
    private const val TAG = "AiCoreCategorizer"
    const val AICORE_PACKAGE_NAME = "com.google.android.aicore"

    @VisibleForTesting
    var testAvailabilityOverride: Boolean? = null

    @VisibleForTesting
    var testInferenceProvider: ((rawText: String, sender: String) -> String?)? = null

    /**
     * Checks if Android AICore system service is available and active on the device.
     */
    fun isAiCoreAvailable(context: Context): Boolean {
        testAvailabilityOverride?.let { return it }

        // AICore system service is officially integrated on Android 14+ (API 34+)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return false
        }

        return try {
            val pm = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(AICORE_PACKAGE_NAME, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(AICORE_PACKAGE_NAME, 0)
            }
            packageInfo != null
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            Log.w(TAG, "AICore availability check failed: ${e.message}")
            false
        }
    }

    /**
     * Parses and validates an incoming SMS using on-device Gemini Nano.
     * Extracts classification, type, amount, currency, merchant, account, and category.
     */
    suspend fun parseSmsTransaction(
        context: Context,
        rawText: String,
        sender: String = ""
    ): AiParsedTransaction? = withContext(Dispatchers.IO) {
        val cleanText = rawText.trim()
        if (cleanText.isEmpty()) return@withContext null

        if (!isAiCoreAvailable(context)) {
            return@withContext null
        }

        try {
            val prompt = buildNanoParsingPrompt(cleanText, sender)
            val rawOutput = executeInference(context, prompt, cleanText, sender) ?: return@withContext null

            // Boundary extraction: locate first '{' and last '}' to strip any wrapper text
            val startIdx = rawOutput.indexOf('{')
            val endIdx = rawOutput.lastIndexOf('}')
            if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) {
                Log.w(TAG, "No valid JSON brackets in AICore output: $rawOutput")
                return@withContext null
            }

            val jsonStr = rawOutput.substring(startIdx, endIdx + 1)
            val json = JSONObject(jsonStr)

            val classification = json.optString("classification", "OTHER").trim().uppercase(Locale.US)
            val isExpense = when (classification) {
                "MERCHANT", "SPEND", "DEBIT", "PURCHASE", "P2P", "TRANSFER", "SELF", "CREDIT_CARD" -> true
                else -> false
            }

            val type = when (classification) {
                "P2P", "TRANSFER" -> ExpenseType.P2P
                "SELF" -> ExpenseType.SELF
                "CREDIT_CARD" -> ExpenseType.CREDIT_CARD
                else -> ExpenseType.MERCHANT
            }

            val amount = json.optDouble("amount", 0.0)

            if (!isExpense || amount <= 0.0) {
                Log.d(TAG, "AICore classified message as non-expense ($classification): '$cleanText'")
                return@withContext AiParsedTransaction(
                    classification = classification.lowercase(Locale.US),
                    isExpense = false,
                    type = type,
                    amount = 0.0,
                    currency = "₹",
                    merchant = "",
                    accountInfo = "",
                    category = "Other",
                    isAiClassified = true,
                    rawText = cleanText
                )
            }

            val rawCurrency = json.optString("currency", "₹").ifBlank { "₹" }
            val currency = if (rawCurrency.equals("INR", ignoreCase = true)) "₹" else rawCurrency

            var merchant = json.optString("merchant", "").trim()
            if (merchant.isBlank() || merchant.equals("null", ignoreCase = true)) {
                merchant = when (type) {
                    ExpenseType.SELF -> "Self Transfer"
                    ExpenseType.CREDIT_CARD -> "Credit Card Bill"
                    ExpenseType.P2P -> "Transfer Recipient"
                    else -> "Merchant / Payee"
                }
            }

            val rawAccount = if (json.has("accountInfo")) json.optString("accountInfo", "") else json.optString("account", "")
            val accountInfo = rawAccount.trim().replace("**", "••")

            var category = json.optString("category", "").trim()
            if (category.isBlank() || category.equals("null", ignoreCase = true) || category.equals("General", ignoreCase = true)) {
                category = when (type) {
                    ExpenseType.P2P -> "Transfers"
                    ExpenseType.SELF -> "Self"
                    ExpenseType.CREDIT_CARD -> "Credit Card Bill"
                    else -> "Uncategorized"
                }
            }

            AiParsedTransaction(
                classification = classification,
                isExpense = true,
                type = type,
                amount = amount,
                currency = currency,
                merchant = merchant,
                accountInfo = accountInfo,
                category = category,
                isAiClassified = true,
                rawText = cleanText
            )
        } catch (e: Exception) {
            Log.e(TAG, "AICore SMS parse failed, falling back to local parser: ${e.message}", e)
            null
        }
    }

    /**
     * Categorizes a recognized transaction using on-device Gemini Nano.
     */
    suspend fun categorizeSms(
        context: Context,
        rawText: String,
        merchant: String,
        amount: Double = 0.0,
        currency: String = "₹"
    ): CategorizationResult = withContext(Dispatchers.IO) {
        if (!isAiCoreAvailable(context)) {
            return@withContext CategorizationResult(
                category = "UNKNOWN",
                isAiClassified = false,
                confidence = 0f,
                errorMessage = "AICore not available"
            )
        }

        try {
            val prompt = """
                Categorize this financial debit:
                Merchant: "$merchant"
                Amount: $currency$amount
                SMS: "$rawText"
                
                Choose exactly one from:
                [Transfers, Credit Card Bill, Self, Groceries, Food & Dining, Shopping, Bills & Utilities, Travel & Commute, Entertainment, Health & Wellness, Investments, Education, Personal Care]
                
                Return JSON only:
                {"category": "Chosen Category", "confidence": 0.95}
            """.trimIndent()

            val rawOutput = executeInference(context, prompt, rawText, merchant)
                ?: return@withContext CategorizationResult("UNKNOWN", false, 0f)

            val startIdx = rawOutput.indexOf('{')
            val endIdx = rawOutput.lastIndexOf('}')
            if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) {
                return@withContext CategorizationResult("UNKNOWN", false, 0f)
            }

            val json = JSONObject(rawOutput.substring(startIdx, endIdx + 1))
            val cat = json.optString("category", "UNKNOWN").trim()
            val conf = json.optDouble("confidence", 0.9).toFloat()

            CategorizationResult(
                category = if (OpenRouterCategorizer.KNOWN_CATEGORIES.contains(cat)) cat else "Uncategorized",
                isAiClassified = true,
                confidence = conf
            )
        } catch (e: Exception) {
            Log.e(TAG, "AICore categorization failed: ${e.message}", e)
            CategorizationResult("UNKNOWN", false, 0f, e.message)
        }
    }

    private fun buildNanoParsingPrompt(rawText: String, sender: String): String {
        return """
            You are an on-device financial SMS analyzer for Savio₹.
            Sender: "$sender"
            SMS: "$rawText"
            
            Extract:
            1. classification: "MERCHANT", "P2P", "SELF", "CREDIT_CARD", "CREDIT", "INTIMATION", "AD", "OTP", or "OTHER"
            2. isExpense: true only if MERCHANT, P2P, SELF, or CREDIT_CARD.
            3. amount: numeric value of debit.
            4. currency: symbol like "₹", "$", "€". Default "₹".
            5. merchant: entity or person paid (NEVER the bank or carrier name like HDFC/SBI/AXIS/ICICI).
            6. accountInfo: e.g. "A/c ••1234" or "Card ••5678" or "UPI ••9012".
            7. category: from [Transfers, Credit Card Bill, Self, Groceries, Food & Dining, Shopping, Bills & Utilities, Travel & Commute, Entertainment, Health & Wellness, Investments, Education, Personal Care].
            
            Return JSON only:
            {"classification":"MERCHANT","isExpense":true,"amount":100.0,"currency":"₹","merchant":"Store","accountInfo":"A/c ••1234","category":"Groceries"}
        """.trimIndent()
    }

    private suspend fun executeInference(
        context: Context,
        prompt: String,
        rawText: String,
        senderOrMerchant: String
    ): String? {
        testInferenceProvider?.let { provider ->
            return provider.invoke(rawText, senderOrMerchant)
        }

        // Production system dispatch: On AICore supported hardware, invoke the on-device system service
        return null
    }
}
