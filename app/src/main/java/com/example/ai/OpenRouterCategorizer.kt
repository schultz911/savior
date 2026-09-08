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

    /**
     * Normalizes raw category string to standard SAVIO categories.
     * Uses semantic keyword inference if category is unknown or generic.
     */
    fun normalizeCategory(rawCat: String, merchant: String = "", rawText: String = ""): String {
        val clean = rawCat.trim()
        val directMatch = KNOWN_CATEGORIES.firstOrNull { it.equals(clean, ignoreCase = true) }
        if (directMatch != null) return directMatch

        val lowerCat = clean.lowercase(Locale.US)
        val combined = "$clean $merchant $rawText".lowercase(Locale.US)

        return when {
            lowerCat.contains("health") || lowerCat.contains("wellness") || lowerCat.contains("pharma") ||
                lowerCat.contains("pharmacy") || lowerCat.contains("chemist") || lowerCat.contains("medical") ||
                lowerCat.contains("medicine") || lowerCat.contains("hospital") || lowerCat.contains("clinic") ||
                combined.contains("pharma") || combined.contains("pharmacy") || combined.contains("chemist") ||
                combined.contains("medplus") || combined.contains("apollo") || combined.contains("1mg") ||
                combined.contains("pharmeasy") || combined.contains("netmeds") || combined.contains("hospital") ||
                combined.contains("clinic") || combined.contains("doctor") || combined.contains("dr.") ||
                combined.contains("diagnostic") || combined.contains("pathology") || combined.contains("lab") ||
                combined.contains("medicine") || combined.contains("medicos") || combined.contains("dental") ||
                combined.contains("gym") || combined.contains("fitness") || combined.contains("cult.fit") -> "Health & Wellness"

            lowerCat.contains("food") || lowerCat.contains("dining") || lowerCat.contains("restaurant") ||
                lowerCat.contains("cafe") || lowerCat.contains("coffee") ||
                combined.contains("swiggy") || combined.contains("zomato") || combined.contains("starbucks") ||
                combined.contains("mcdonald") || combined.contains("kfc") || combined.contains("burger king") ||
                combined.contains("domino") || combined.contains("pizza") || combined.contains("cafe") ||
                combined.contains("restaurant") || combined.contains("dining") || combined.contains("coffee") -> "Food & Dining"

            lowerCat.contains("grocer") || lowerCat.contains("supermarket") ||
                combined.contains("zepto") || combined.contains("blinkit") || combined.contains("instamart") ||
                combined.contains("bigbasket") || combined.contains("dmart") || combined.contains("supermarket") ||
                combined.contains("kirana") || combined.contains("provisions") || combined.contains("grocery") -> "Groceries"

            lowerCat.contains("travel") || lowerCat.contains("commute") || lowerCat.contains("transport") || lowerCat.contains("cab") ||
                combined.contains("uber") || combined.contains("ola") || combined.contains("rapido") ||
                combined.contains("metro") || combined.contains("irctc") || combined.contains("flight") ||
                combined.contains("fuel") || combined.contains("petrol") || combined.contains("diesel") ||
                combined.contains("shell") || combined.contains("fastag") -> "Travel & Commute"

            lowerCat.contains("bill") || lowerCat.contains("utilit") || lowerCat.contains("recharge") || lowerCat.contains("electricity") ||
                combined.contains("bescom") || combined.contains("electricity") || combined.contains("water") ||
                combined.contains("gas") || combined.contains("broadband") || combined.contains("airtel") ||
                combined.contains("jio") || combined.contains("recharge") || combined.contains("dth") -> "Bills & Utilities"

            lowerCat.contains("shop") || lowerCat.contains("retail") || lowerCat.contains("ecommerce") || lowerCat.contains("e-commerce") ||
                combined.contains("amazon") || combined.contains("flipkart") || combined.contains("myntra") ||
                combined.contains("ajio") || combined.contains("nykaa") || combined.contains("zara") ||
                combined.contains("h&m") || combined.contains("croma") || combined.contains("mall") -> "Shopping"

            lowerCat.contains("entertain") || lowerCat.contains("movie") || lowerCat.contains("cinema") || lowerCat.contains("streaming") ||
                combined.contains("bookmyshow") || combined.contains("netflix") || combined.contains("spotify") ||
                combined.contains("prime video") || combined.contains("hotstar") || combined.contains("pvr") ||
                combined.contains("inox") || combined.contains("cinema") || combined.contains("movie") ||
                combined.contains("steam") || combined.contains("playstation") || combined.contains("youtube") -> "Entertainment"

            lowerCat.contains("personal") || lowerCat.contains("care") || lowerCat.contains("salon") || lowerCat.contains("spa") || lowerCat.contains("beauty") ||
                combined.contains("salon") || combined.contains("spa") || combined.contains("barber") ||
                combined.contains("parlour") || combined.contains("grooming") || combined.contains("skincare") ||
                combined.contains("cosmetics") || combined.contains("urban company") -> "Personal Care"

            lowerCat.contains("invest") || lowerCat.contains("stock") || lowerCat.contains("mutual") || lowerCat.contains("trading") ||
                combined.contains("zerodha") || combined.contains("groww") || combined.contains("upstox") ||
                combined.contains("mutual fund") || combined.contains("sip") || combined.contains("stocks") ||
                combined.contains("angel one") || combined.contains("smallcase") -> "Investments"

            lowerCat.contains("educat") || lowerCat.contains("tuition") || lowerCat.contains("course") || lowerCat.contains("school") ||
                combined.contains("coursera") || combined.contains("udemy") || combined.contains("unacademy") ||
                combined.contains("school") || combined.contains("college") || combined.contains("university") ||
                combined.contains("tuition") || combined.contains("fee") || combined.contains("books") -> "Education"

            lowerCat.contains("self") || combined.contains("self transfer") || combined.contains("own account") ||
                combined.contains("to own") || combined.contains("to self") || combined.contains("linked account") -> "Self"

            lowerCat.contains("credit card") || lowerCat.contains("card dues") || lowerCat.contains("card bill") ||
                combined.contains("towards your credit card") || combined.contains("credit card bill") ||
                combined.contains("card dues") || combined.contains("paid to cred") || combined.contains("bill payment for card") -> "Credit Card Bill"

            lowerCat.contains("transfer") || lowerCat.contains("p2p") ||
                combined.contains("transfer to") || combined.contains("sent to") || combined.contains("paid to") ||
                combined.contains("vpa") || combined.contains("upi") -> "Transfers"

            else -> "Uncategorized"
        }
    }

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
1. "MERCHANT": An actual outgoing payment, purchase, or debit made to a store, business, vendor, restaurant, app, pharmacy, utility, or service.
   CRITICAL — PURCHASES MADE USING A CREDIT CARD ARE "MERCHANT" PURCHASES!
   When an SMS indicates that money was spent, charged, debited, or paid USING a credit card at a merchant (e.g. Swiggy, Amazon, Uber, restaurant, supermarket, pharmacy, retail store, online purchase):
   - classification is ALWAYS "MERCHANT" (NOT "CREDIT_CARD"!)
   - isExpense is true
   - merchant is the EXACT merchant/store name from the SMS (e.g. "SWIGGY BANGALORE", "AMAZON INDIA", "SANDEEP PHARMA")
   - accountInfo is the card reference, e.g. "Card ••4821"
   - category is what was bought (e.g. "Food & Dining", "Shopping", "Groceries", "Health & Wellness", "Travel & Commute"), NEVER "Credit Card Bill"!

2. "CREDIT_CARD": ONLY for paying off a credit card bill, card statement dues, or card account repayment (e.g. "Payment received towards your credit card ending 4821", "Auto-debit for credit card bill successful", "Bill payment for card ending 1234", "Paid to CRED for credit card").
   - classification is "CREDIT_CARD"
   - isExpense is true
   - merchant is the credit card bill name (e.g. "HDFC Credit Card Bill", "CRED - Credit Card Bill")
   - category is "Credit Card Bill"

3. "SELF": Transfer between user's own bank accounts (e.g. "transfer to self account", "to own account", "to own a/c", "to self a/c", "linked account", "between your accounts", "to my account", "self transfer").
   - classification is "SELF"
   - isExpense is true
   - merchant is "Self Transfer"
   - category is "Self"

4. "P2P": Outgoing money transferred to another person/contact via UPI, NEFT, IMPS, Zelle, Venmo, or wire.
   - classification is "P2P"
   - isExpense is true
   - merchant is the recipient's exact name or VPA handle (e.g. "Ramesh Kumar" or "rahul@okaxis")
   - category is "Transfers"

5. "REFUND": A confirmed refund or reversal credited back from a merchant.
   - classification is "REFUND"
   - isExpense is false
   - merchant is the exact company/service that issued the refund (e.g. "Swiggy", "Zomato", "Amazon")
   - category is "Refund"

6. "CREDIT": Salary, general deposit, interest credited. (isExpense: false)
7. "INTIMATION", "AD", "OTP", "OTHER": Non-transactional bank notifications, balance alerts, ads, OTPs. (isExpense: false)

MERCHANT EXTRACTION RULES:
- Extract the EXACT merchant, vendor, store, or recipient name VERBATIM as written in the SMS (e.g. "SANDEEP PHARMA", "SWIGGY BANGALORE", "AMAZON INDIA", "BLUE TOKAI COFFEE", "SHELL PETROL PUMP", "ZEPTO COMMERCE").
- DO NOT summarize, abbreviate, titlecase, or replace with generic brand names. Keep the exact merchant name from the SMS text because the user will assign aliases themselves in the app.
- NEVER use the bank or carrier sender name (e.g. HDFC, ICICI, SBI, AXIS, KOTAK, Bank, VM-HDFCBK) as the merchant name.
- If paying a credit card bill: use the card bill name (e.g. "HDFC Credit Card Bill" or "CRED - Credit Card Bill").
- If transferring to self: use "Self Transfer".
- If unknown: use "Merchant / Payee" or "Transfer Recipient".

ACCOUNT INFO:
- Detect card or account info with 4-digit mask (e.g. "Card ••4821", "A/c ••3391", "UPI ••9012").

CATEGORY MAPPING GUIDE (Choose the most accurate category):
- "Health & Wellness": Any pharmacy, chemist, medicine store, pharma (e.g. "SANDEEP PHARMA", "Apollo Pharmacy", "Medplus", "Tata 1mg", "PharmEasy", "Netmeds"), hospitals, clinics, diagnostic labs, doctors, gyms, fitness centers ("Cult.fit").
- "Groceries": Supermarkets, quick-commerce (Blinkit, Zepto, Instamart, BigBasket), kirana stores, provisions, milk, vegetables, D-Mart.
- "Food & Dining": Restaurants, cafes, food delivery (Swiggy, Zomato), coffee shops (Starbucks), fast food (McDonald's, Domino's, KFC), bakeries, dining.
- "Travel & Commute": Cabs (Uber, Ola, Rapido), public transport (Metro, IRCTC, trains), airlines (IndiGo, Air India), fuel/petrol pumps (Shell, HPCL, BPCL, Indian Oil), FASTag, tolls, parking.
- "Bills & Utilities": Electricity (BESCOM, Tata Power), water, gas (PNG/LPG), broadband/WiFi, mobile recharge/postpaid (Airtel, Jio, Vi), DTH (Tata Play).
- "Shopping": E-commerce (Amazon, Flipkart, Myntra, Ajio), clothing/apparel (Zara, H&M), electronics (Croma, Apple, Reliance Digital), retail stores, malls.
- "Entertainment": Movies (BookMyShow, PVR, INOX), streaming subscriptions (Netflix, Spotify, Prime Video, Hotstar, YouTube), gaming (Steam, PlayStation).
- "Personal Care": Salons, spas, barbers, beauty parlours, grooming, cosmetics, skincare, Urban Company.
- "Investments": Stock brokers (Zerodha, Groww, Upstox, Angel One), mutual funds, SIPs, NPS, crypto/Coin.
- "Education": School/college fees, universities, coaching/tuition, online courses (Coursera, Udemy), books.
- "Transfers": P2P transfers sent to contacts, friends, or individuals.
- "Self": Internal transfers between own bank accounts.
- "Credit Card Bill": Paying off credit card bills/statement dues.

FEW-SHOT EXAMPLES:
SMS: "Paid Rs 450.00 to SANDEEP PHARMA from A/c XX4821 on 04-Sep. UPI Ref: 445566."
Output: {"classification":"MERCHANT","amount":450.00,"currency":"₹","merchant":"SANDEEP PHARMA","accountInfo":"A/c ••4821","category":"Health & Wellness"}

SMS: "Debited INR 1,200.00 on HDFC Card ending 4821 at APOLLO PHARMACY on 05-Sep."
Output: {"classification":"MERCHANT","amount":1200.00,"currency":"₹","merchant":"APOLLO PHARMACY","accountInfo":"Card ••4821","category":"Health & Wellness"}

SMS: "Dear SBI User, your A/c XX3391 debited by Rs 10,000.00 on 03-Sep towards transfer to Self A/c XX8812. UPI Ref 382910."
Output: {"classification":"SELF","amount":10000.00,"currency":"₹","merchant":"Self Transfer","accountInfo":"A/c ••3391","category":"Self"}

SMS: "INR 25,000.00 debited from A/c **4821 on 04-Sep-24 to own account in ICICI Bank. UPI: 998822."
Output: {"classification":"SELF","amount":25000.00,"currency":"₹","merchant":"Self Transfer","accountInfo":"A/c ••4821","category":"Self"}

SMS: "Payment received of INR 8,500.00 towards your HDFC Bank Credit Card ending 4821 on 02-Sep."
Output: {"classification":"CREDIT_CARD","amount":8500.00,"currency":"₹","merchant":"HDFC Credit Card Bill","accountInfo":"Card ••4821","category":"Credit Card Bill"}

SMS: "Rs 15,400.00 debited from A/c ending 1234 towards payment to CRED for Credit Card bill on 05-Sep."
Output: {"classification":"CREDIT_CARD","amount":15400.00,"currency":"₹","merchant":"CRED - Credit Card Bill","accountInfo":"A/c ••1234","category":"Credit Card Bill"}

SMS: "Spent INR 6,890.00 on Axis Card ending 1004 at AMAZON INDIA on 01-Sep. Avl Limit: Rs 1,85,000.00."
Output: {"classification":"MERCHANT","amount":6890.00,"currency":"₹","merchant":"AMAZON INDIA","accountInfo":"Card ••1004","category":"Shopping"}

SMS: "Thank you for using HDFC Bank Credit Card ending 4821 for payment of Rs 1,450.00 at SWIGGY BANGALORE on 04-Sep. Avl Limit: Rs 48,250.00."
Output: {"classification":"MERCHANT","amount":1450.00,"currency":"₹","merchant":"SWIGGY BANGALORE","accountInfo":"Card ••4821","category":"Food & Dining"}

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
            var merchant = json.optString("merchant", "Unknown").ifEmpty { "Unknown" }
            val accountInfo = json.optString("accountInfo", "")
            val rawCategory = json.optString("category", "General")

            val lowerText = rawText.lowercase(Locale.US)
            val isSelfDetected = classification == "SELF" ||
                lowerText.contains("to own account") || lowerText.contains("self transfer") ||
                lowerText.contains("transferred to your own") || lowerText.contains("linked account") ||
                lowerText.contains("between your accounts") || lowerText.contains("to self") ||
                lowerText.contains("transfer to self") || lowerText.contains("paid to self") ||
                lowerText.contains("to own a/c") || lowerText.contains("to self a/c") ||
                lowerText.contains("to my account") || lowerText.contains("self acc")

            val isCardBillDetected = classification == "CREDIT_CARD" ||
                lowerText.contains("towards your credit card") || lowerText.contains("towards credit card") ||
                lowerText.contains("credit card bill") || lowerText.contains("card dues") ||
                lowerText.contains("paid to cred") || lowerText.contains("payment to cred") ||
                lowerText.contains("bill payment for card") || lowerText.contains("autopay for card") ||
                (lowerText.contains("payment received") && lowerText.contains("card"))

            var category = normalizeCategory(rawCategory, merchant, rawText)
            if (category == "Uncategorized" || category.isBlank() || category.equals("General", ignoreCase = true) || category.equals("General Spend", ignoreCase = true)) {
                category = when {
                    isSelfDetected -> "Self"
                    isCardBillDetected -> "Credit Card Bill"
                    classification == "P2P" || classification == "TRANSFER" -> "Transfers"
                    else -> normalizeCategory(rawCategory, merchant, rawText)
                }
            }

            val isRefund = classification == "REFUND" || classification == "REVERSAL"
            val isExpense = (classification == "MERCHANT" || classification == "SPEND" ||
                             classification == "P2P" || classification == "TRANSFER" ||
                             classification == "SELF" || classification == "CREDIT_CARD" ||
                             classification == "DEBIT" || classification == "EXPENSE" ||
                             classification == "PAYMENT" || classification == "PURCHASE" ||
                             isSelfDetected || isCardBillDetected) && amount > 0.0

            val expenseType = when {
                isSelfDetected -> ExpenseType.SELF
                isCardBillDetected -> ExpenseType.CREDIT_CARD
                classification == "MERCHANT" || classification == "SPEND" || classification == "DEBIT" || classification == "PURCHASE" -> ExpenseType.MERCHANT
                classification == "P2P" || classification == "TRANSFER" -> ExpenseType.P2P
                category == "Self" -> ExpenseType.SELF
                category == "Credit Card Bill" -> ExpenseType.CREDIT_CARD
                category == "Transfers" -> ExpenseType.P2P
                else -> ExpenseType.MERCHANT
            }

            if (expenseType == ExpenseType.SELF) {
                category = "Self"
                if (merchant.isBlank() || merchant.equals("Unknown", ignoreCase = true) || merchant.equals("Merchant / Payee", ignoreCase = true)) {
                    merchant = "Self Transfer"
                }
            } else if (expenseType == ExpenseType.CREDIT_CARD) {
                category = "Credit Card Bill"
                if (merchant.isBlank() || merchant.equals("Unknown", ignoreCase = true) || merchant.equals("Merchant / Payee", ignoreCase = true)) {
                    merchant = "Credit Card Bill"
                }
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
- Health & Wellness: Pharmacies, chemists, pharma, medical stores, medicines (e.g. "SANDEEP PHARMA", "Apollo Pharmacy", "Medplus", "1mg", "PharmEasy"), hospitals, clinics, diagnostic labs, doctors, gyms, cult.fit, fitness.
- Groceries: Supermarkets, quick-commerce (Blinkit, Zepto, Instamart, BigBasket), grocery, kirana, provisions, milk, vegetables.
- Food & Dining: Restaurants, cafes, food delivery (Swiggy, Zomato), coffee shops, fast food, dining.
- Travel & Commute: Cabs (Uber, Ola, Rapido), public transport (Metro, IRCTC, trains), airlines, fuel/petrol pumps, FASTag, tolls.
- Bills & Utilities: Electricity, water, gas, broadband, mobile recharge/postpaid (Airtel, Jio, Vi), DTH.
- Shopping: E-commerce (Amazon, Flipkart, Myntra, Ajio), apparel, electronics, retail stores, malls.
- Entertainment: Movies (BookMyShow, PVR, INOX), streaming (Netflix, Spotify, Prime Video, Hotstar, YouTube), gaming.
- Personal Care: Salons, spas, barbers, beauty parlours, grooming, cosmetics, skincare.
- Investments: Stock brokers (Zerodha, Groww, Upstox, Angel One), mutual funds, SIPs, NPS.
- Education: School/college fees, universities, coaching/tuition, online courses, books.
- Transfers: P2P money sent to contacts, friends, or individuals.
- Self: Internal transfers between own bank accounts.
- Credit Card Bill: ONLY when paying off credit card bills/statement dues or card payments.

Rules:
1. If the message clearly belongs to one of the above categories, output ONLY the exact category name.
2. Never use "UPI" as a category. UPI is only a payment method.
3. If the message is ambiguous, generic, cannot be determined, or is not an identifiable purchase, output ONLY "UNKNOWN".
4. Do not add explanations, prefixes, punctuation or quotes.
5. CRITICAL: A purchase made USING a credit card at a store or merchant (e.g. Swiggy, Amazon, Uber, restaurant, pharmacy) MUST be categorized by what was bought (e.g. Food & Dining, Shopping, Health & Wellness). NEVER categorize a purchase as "Credit Card Bill" just because a credit card was used! Use "Credit Card Bill" ONLY when paying off the credit card bill itself.
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
                val inferred = normalizeCategory("UNKNOWN", merchant, rawText)
                return@withContext if (inferred != "Uncategorized") {
                    CategorizationResult(
                        category = inferred,
                        isAiClassified = true,
                        confidence = 0.85f
                    )
                } else {
                    CategorizationResult(
                        category = "UNKNOWN",
                        isAiClassified = true,
                        confidence = 0.0f
                    )
                }
            }

            val cleaned = rawResult.removeSurrounding("\"").removeSurrounding("'").replace("```", "").trim()
            val normalized = normalizeCategory(cleaned, merchant, rawText)

            if (normalized != "Uncategorized") {
                CategorizationResult(
                    category = normalized,
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
