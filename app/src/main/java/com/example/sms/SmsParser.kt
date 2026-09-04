package com.example.sms

import com.example.data.ExpenseType
import java.util.Locale
import java.util.regex.Pattern

object SmsParser {

    // Negative keywords: Non-expenditures, OTPs, incoming credits
    private val EXCLUSION_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(otp|one time password|verification code|security code|is your code|secret code)\\b"),
        Pattern.compile("(?i)\\b(refund(?:ed)?|cashback|salary credited|credited with|deposited|credit alert)\\b"),
        Pattern.compile("(?i)\\b(received (?:rs\\.?|usd|\\$)?\\s*\\d+)\\b")
    )

    // Keywords identifying outgoing spend / debit / transfer
    private val DEBIT_KEYWORDS = listOf(
        "debited", "debit alert", "withdrawn", "atm wdl", "cash withdrawal",
        "deducted", "deduction", "charged to your a/c", "direct debit"
    )

    private val TRANSFER_KEYWORDS = listOf(
        "transferred", "transfer of", "transfer to", "sent to", "sent via zelle",
        "zelle to", "venmo to", "wire transfer", "neft", "imps", "upi to",
        "p2p transfer", "sent money", "you paid"
    )

    private val SPEND_KEYWORDS = listOf(
        "spent", "purchase of", "charged", "swiped", "pos txn", "transaction of",
        "payment of", "paid at", "spent on", "apple pay", "google pay", "card ending",
        "txn of", "paid to"
    )

    // Regex for matching amounts with currency symbols or codes
    // Matches: $100, $ 100.50, USD 45.00, EUR 12.30, Rs. 500, INR 1,200.00, 1,450.50 USD
    private val AMOUNT_PATTERNS = listOf(
        // $ / € / £ / ₹ / Rs followed by number
        Pattern.compile("""(?i)([$€£¥₹]|Rs\.?|INR|USD|EUR|GBP|CAD|AUD)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"""),
        // Number followed by USD / EUR / etc.
        Pattern.compile("""(?i)([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\s*([$€£¥₹]|USD|EUR|GBP|INR|CAD|AUD)"""),
        // "debited by/for/with 120.00" or "spent 45.50"
        Pattern.compile("""(?i)(?:debited|spent|paid|charged|amount of|sum of|txn of|withdrawn)\s*(?:of|by|for|with)?\s*([$€£¥₹]|Rs\.?|USD|EUR|GBP)?\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""")
    )

    // Regex for card / account numbers
    private val ACCOUNT_PATTERNS = listOf(
        Pattern.compile("""(?i)(?:card|a/c|account|acct)(?:\s*(?:no\.?|ending|xx|\*+|-))*\s*([0-9]{3,4})"""),
        Pattern.compile("""(?i)(?:ending\s+in\s+)([0-9]{4})"""),
        Pattern.compile("""(?i)(?:xx|[*]{2,})([0-9]{4})""")
    )

    // Regex for merchant / recipient
    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("""(?i)(?:at|to|towards|in favor of|vpa|paid to)\s+([A-Za-z0-9&.\-_/ ]{2,30}?)(?:\s+(?:on|via|using|ref|avl|bal|dated|\.|,|$))"""),
        Pattern.compile("""(?i)(?:approved at)\s+([A-Za-z0-9&.\-_/ ]{2,30}?)(?:\s+(?:on|for|using|\.|,|$))""")
    )

    fun parse(smsBody: String, sender: String = ""): ParsedSms? {
        val cleanBody = smsBody.trim()
        if (cleanBody.isEmpty()) return null

        // Check for exclusions (OTP, refunds, credits)
        for (pattern in EXCLUSION_PATTERNS) {
            if (pattern.matcher(cleanBody).find()) {
                // If it's pure OTP or credited, ignore
                if (cleanBody.contains("credited", ignoreCase = true) && !cleanBody.contains("debited", ignoreCase = true)) {
                    return null
                }
                if (cleanBody.contains("otp", ignoreCase = true) || cleanBody.contains("verification code", ignoreCase = true)) {
                    return null
                }
            }
        }

        // Determine if message is an expenditure
        val lower = cleanBody.lowercase(Locale.US)
        val isTransfer = TRANSFER_KEYWORDS.any { lower.contains(it) }
        val isDebit = DEBIT_KEYWORDS.any { lower.contains(it) }
        val isSpend = SPEND_KEYWORDS.any { lower.contains(it) }

        if (!isTransfer && !isDebit && !isSpend) {
            // Check if sender looks like a bank/fintech AND message mentions amount
            val looksLikeBank = isBankSender(sender)
            val hasAmount = AMOUNT_PATTERNS.any { it.matcher(cleanBody).find() }
            if (!looksLikeBank || !hasAmount) {
                return null
            }
        }

        // Determine expense type
        val expenseType = when {
            isTransfer -> ExpenseType.TRANSFER
            isDebit -> ExpenseType.DEBIT
            else -> ExpenseType.SPEND
        }

        // Extract Amount and Currency
        val (amount, currency) = extractAmountAndCurrency(cleanBody) ?: return null

        if (amount <= 0.0) return null

        // Extract Account Info
        val accountInfo = extractAccountInfo(cleanBody)

        // Extract Merchant / Recipient
        val merchant = extractMerchant(cleanBody, sender)

        // Categorize
        val category = categorize(cleanBody, merchant, expenseType)

        return ParsedSms(
            amount = amount,
            currency = currency,
            type = expenseType,
            title = merchant,
            accountInfo = accountInfo,
            category = category,
            isExpense = true,
            rawText = cleanBody
        )
    }

    private fun extractAmountAndCurrency(text: String): Pair<Double, String>? {
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                // Handle different pattern group configurations
                var curr = "$"
                var amtStr = ""

                if (matcher.groupCount() == 2) {
                    val g1 = matcher.group(1)?.trim() ?: ""
                    val g2 = matcher.group(2)?.trim() ?: ""

                    // Check which group is amount
                    if (isNumeric(g2)) {
                        amtStr = g2
                        curr = normalizeCurrency(g1)
                    } else if (isNumeric(g1)) {
                        amtStr = g1
                        curr = normalizeCurrency(g2)
                    }
                } else if (matcher.groupCount() >= 1) {
                    amtStr = matcher.group(matcher.groupCount()) ?: ""
                }

                val cleanAmt = amtStr.replace(",", "")
                val amt = cleanAmt.toDoubleOrNull()
                if (amt != null && amt > 0.0) {
                    // Quick sanity check: Ignore balances, e.g. if the match was "Avl Bal $3000"
                    val start = matcher.start()
                    val prefix = if (start > 10) text.substring(start - 10, start).lowercase() else ""
                    if (prefix.contains("bal") || prefix.contains("avl") || prefix.contains("limit")) {
                        // Keep searching for the actual debit/spent amount
                        continue
                    }
                    return Pair(amt, curr)
                }
            }
        }
        return null
    }

    private fun isNumeric(str: String): Boolean {
        val clean = str.replace(",", "")
        return clean.toDoubleOrNull() != null
    }

    private fun normalizeCurrency(raw: String): String {
        val upper = raw.trim().uppercase(Locale.US)
        return when {
            upper.contains("₹") || upper.contains("RS") || upper == "INR" -> "₹"
            upper.contains("$") || upper == "USD" -> "$"
            upper.contains("€") || upper == "EUR" -> "€"
            upper.contains("£") || upper == "GBP" -> "£"
            upper == "CAD" -> "CA$"
            upper == "AUD" -> "AU$"
            upper.isNotEmpty() -> upper
            else -> "₹"
        }
    }

    private fun extractAccountInfo(text: String): String {
        for (pattern in ACCOUNT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val num = matcher.group(1)
                if (!num.isNullOrBlank()) {
                    val isCard = text.contains("card", ignoreCase = true)
                    val prefix = if (isCard) "Card ••" else "A/c ••"
                    return "$prefix$num"
                }
            }
        }
        return ""
    }

    private fun extractMerchant(text: String, sender: String): String {
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val match = matcher.group(1)?.trim() ?: ""
                val clean = cleanMerchantName(match)
                if (clean.length in 2..35) {
                    return clean
                }
            }
        }

        // Fallback: If sender has identifiable name (e.g., "CHASE", "AMEX", "BOA", "VENMO")
        val cleanSender = cleanSenderName(sender)
        if (cleanSender.isNotEmpty()) {
            return cleanSender
        }

        return "Payment"
    }

    private fun cleanMerchantName(name: String): String {
        var clean = name.replace(Regex("(?i)^(the|a|an)\\s+"), "")
            .replace(Regex("(?i)\\s+(ltd|inc|corp|co|llc|pvt|services)$"), "")
            .replace(Regex("[*#_]"), " ")
            .trim()

        // Capitalize words nicely
        return clean.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                if (word.length <= 3 && word.all { it.isLetter() }) word.uppercase(Locale.US)
                else word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            }
    }

    private fun cleanSenderName(sender: String): String {
        val s = sender.trim().replace(Regex("[^A-Za-z0-9]"), " ")
        val parts = s.split(" ").filter { it.isNotBlank() && it.length > 2 }
        return parts.firstOrNull()?.uppercase(Locale.US) ?: ""
    }

    private fun isBankSender(sender: String): Boolean {
        val s = sender.lowercase(Locale.US)
        return s.contains("bank") || s.contains("chase") || s.contains("citi") ||
                s.contains("amex") || s.contains("wells") || s.contains("hdfc") ||
                s.contains("icici") || s.contains("sbi") || s.contains("pay") ||
                s.contains("card") || s.contains("alert") || s.contains("money")
    }

    private fun categorize(text: String, merchant: String, type: ExpenseType): String {
        val combined = "$text $merchant".lowercase(Locale.US)

        return when {
            type == ExpenseType.TRANSFER || combined.contains("zelle") || combined.contains("venmo") || combined.contains("transfer") -> "Transfers"
            combined.contains("uber") || combined.contains("lyft") || combined.contains("taxi") || combined.contains("gas") || combined.contains("shell") || combined.contains("chevron") || combined.contains("metro") || combined.contains("flight") -> "Travel & Commute"
            combined.contains("whole foods") || combined.contains("trader joe") || combined.contains("walmart") || combined.contains("costco") || combined.contains("kroger") || combined.contains("target") || combined.contains("grocery") || combined.contains("supermarket") -> "Groceries"
            combined.contains("starbucks") || combined.contains("mcdonald") || combined.contains("chipotle") || combined.contains("restaurant") || combined.contains("cafe") || combined.contains("pizza") || combined.contains("burger") || combined.contains("dining") || combined.contains("coffee") || combined.contains("food") -> "Food & Dining"
            combined.contains("netflix") || combined.contains("spotify") || combined.contains("electric") || combined.contains("utility") || combined.contains("water") || combined.contains("bill") || combined.contains("recharge") || combined.contains("internet") || combined.contains("att") || combined.contains("verizon") -> "Bills & Utilities"
            combined.contains("amazon") || combined.contains("apple") || combined.contains("ebay") || combined.contains("best buy") || combined.contains("nike") || combined.contains("zara") || combined.contains("store") || combined.contains("mall") -> "Shopping"
            else -> "General Spend"
        }
    }
}
