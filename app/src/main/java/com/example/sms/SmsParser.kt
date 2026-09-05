package com.example.sms

import com.example.data.ExpenseType
import java.util.Locale
import java.util.regex.Pattern

object SmsParser {

    // Negative keywords: Non-expenditures, OTPs, incoming credits, ads, intimations
    private val EXCLUSION_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(otp|one time password|verification code|security code|is your code|secret code)\\b"),
        Pattern.compile("(?i)\\b(refund(?:ed)?|cashback|salary credited|credited with|deposited|credit alert)\\b"),
        Pattern.compile("(?i)\\b(pre-approved|apply now|congratulations|click here|claim your|loan offer|discount on|cash prize)\\b"),
        Pattern.compile("(?i)\\b(received (?:rs\\.?|usd|\\$)?\\s*\\d+)\\b")
    )

    // Keywords identifying outgoing spend / debit / transfer
    private val DEBIT_KEYWORDS = listOf(
        "debited", "debit alert", "withdrawn", "atm wdl", "cash withdrawal",
        "deducted", "deduction", "charged to your a/c", "direct debit",
        "upi", "via upi", "upi txn", "upi-debit", "upi transfer", "upi payment",
        "upi/p2m", "upi/p2a", "by upi"
    )

    private val TRANSFER_KEYWORDS = listOf(
        "transferred", "transfer of", "transfer to", "sent to", "sent via zelle",
        "zelle to", "venmo to", "wire transfer", "neft", "imps", "upi to",
        "sent via upi", "transferred via upi", "upi transfer to", "sent to vpa",
        "p2p transfer", "sent money", "you paid"
    )

    private val SPEND_KEYWORDS = listOf(
        "spent", "purchase of", "charged", "swiped", "pos txn", "transaction of",
        "payment of", "paid at", "spent on", "apple pay", "google pay", "card ending",
        "txn of", "paid to", "paid via upi", "paid using upi", "upi/p2m"
    )

    // Regex for matching amounts with currency symbols or codes
    // Matches: $100, $ 100.50, USD 45.00, Rs.50000.00, Rs. 50,000.00, INR 5,00,000.00
    private val AMOUNT_PATTERNS = listOf(
        // $ / € / £ / ₹ / Rs followed by number (greedy match for full digits and comma separators)
        Pattern.compile("""(?i)([$€£¥₹]|Rs\.?|INR|USD|EUR|GBP|CAD|AUD)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""),
        // Number followed by USD / EUR / etc.
        Pattern.compile("""(?i)([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*([$€£¥₹]|USD|EUR|GBP|INR|CAD|AUD)"""),
        // "debited by/for/with 50000.00" or "spent 45.50"
        Pattern.compile("""(?i)(?:debited|spent|paid|charged|amount of|sum of|txn of|withdrawn)\s*(?:of|by|for|with)?\s*([$€£¥₹]|Rs\.?|INR|USD|EUR|GBP)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""")
    )

    // Regex for card / account / UPI references
    private val ACCOUNT_PATTERNS = listOf(
        Pattern.compile("""(?i)(?:card|a/c|account|acct)(?:\s*(?:no\.?|ending|xx|\*+|-))*\s*([0-9]{3,4})"""),
        Pattern.compile("""(?i)(?:ending\s+in\s+)([0-9]{4})"""),
        Pattern.compile("""(?i)(?:xx|[*]{2,})([0-9]{4})"""),
        Pattern.compile("""(?i)(?:upi\s*(?:ref|reference)?(?:\s*no\.?)?[\s:]+)([0-9]{4,16})""")
    )

    // Regex for merchant / recipient
    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("""(?i)(?:via\s+upi\s+to|by\s+upi\s+to|upi\s+to|transfer\s+to|paid\s+to|to\s+vpa|towards|in\s+favor\s+of)\s+([A-Za-z0-9&.\-_/@ ]{2,35}?)(?:\s+(?:on|via|using|upi\s+ref|ref\s+no|ref|avl|bal|\.|,|$))"""),
        Pattern.compile("""(?i)(?:upi/(?:p2m|p2a)/[0-9]+/)([A-Za-z0-9&.\-_/ ]{2,30})"""),
        Pattern.compile("""(?i)(?:at|to)\s+([A-Za-z0-9&.\-_/ ]{2,30}?)(?:\s+(?:on|via|using|ref|avl|bal|dated|\.|,|$))"""),
        Pattern.compile("""(?i)(?:approved at)\s+([A-Za-z0-9&.\-_/ ]{2,30}?)(?:\s+(?:on|for|using|\.|,|$))""")
    )

    fun parse(smsBody: String, sender: String = ""): ParsedSms? {
        val cleanBody = smsBody.trim()
        if (cleanBody.isEmpty()) return null

        // Check for exclusions (OTP, refunds, credits, ads)
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
        val isTransfer = TRANSFER_KEYWORDS.any { lower.contains(it) } ||
                Regex("""(?i)\b(?:sent|transferred|transfer)\b.{1,35}\bto\b""").containsMatchIn(cleanBody) ||
                Regex("""(?i)\b(?:upi/p2a/|p2p)\b""").containsMatchIn(cleanBody)
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

        // Only two types of transactions: SPEND (paying merchants) and TRANSFER (money sent to people)
        val expenseType = when {
            isTransfer -> ExpenseType.TRANSFER
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
            while (matcher.find()) {
                // Handle different pattern group configurations
                var curr = "₹"
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
                    val prefix = if (start > 12) text.substring(start - 12, start).lowercase() else text.substring(0, start).lowercase()
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
                    val isUpi = pattern.pattern().contains("upi", ignoreCase = true) || text.contains("upi", ignoreCase = true)
                    val isCard = text.contains("card", ignoreCase = true)
                    val prefix = when {
                        isCard -> "Card ••"
                        isUpi -> "UPI ••"
                        else -> "A/c ••"
                    }
                    val suffix = if (num.length > 4) num.takeLast(4) else num
                    return "$prefix$suffix"
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

        return "UPI / Payment"
    }

    private fun cleanMerchantName(name: String): String {
        var raw = name
        // If it's a VPA handle like swiggy@icici or 9876543210@paytm
        if (raw.contains("@")) {
            val prefix = raw.substringBefore("@").trim()
            if (prefix.all { it.isDigit() }) {
                return "UPI (${prefix.takeLast(4)})"
            } else if (prefix.isNotBlank()) {
                raw = prefix
            }
        }

        var clean = raw.replace(Regex("(?i)^(the|a|an)\\s+"), "")
            .replace(Regex("(?i)\\s+(ltd|inc|corp|co|llc|pvt|services|vpa)$"), "")
            .replace(Regex("[*#_/]"), " ")
            .trim()

        // Capitalize words nicely
        val words = clean.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                if (word.length <= 3 && word.all { it.isLetter() }) word.uppercase(Locale.US)
                else word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            }

        return words.ifEmpty { "Payment" }
    }

    private fun cleanSenderName(sender: String): String {
        val s = sender.trim().replace(Regex("[^A-Za-z0-9]"), " ")
        val parts = s.split(" ").filter { it.isNotBlank() && it.length > 2 }
        return parts.firstOrNull()?.uppercase(Locale.US) ?: ""
    }

    fun isBankSender(sender: String): Boolean {
        val s = sender.lowercase(Locale.US)
        return s.contains("bank") || s.contains("chase") || s.contains("citi") ||
                s.contains("amex") || s.contains("wells") || s.contains("hdfc") ||
                s.contains("icici") || s.contains("sbi") || s.contains("pay") ||
                s.contains("card") || s.contains("alert") || s.contains("money") ||
                s.contains("axis") || s.contains("kotak") || s.contains("pnb") ||
                s.contains("boi") || s.contains("canara") || s.contains("idfc") ||
                s.contains("indus") || s.contains("yes") || s.contains("rbl") ||
                s.contains("upi") || s.contains("gpay") || s.contains("phonepe") ||
                s.contains("paytm") || s.contains("cred")
    }

    /**
     * Categorizes spend, explicitly identifying UPI transactions alongside specific merchants.
     */
    private fun categorize(text: String, merchant: String, type: ExpenseType): String {
        val combined = "$text $merchant".lowercase(Locale.US)

        return when {
            combined.contains("whole foods") || combined.contains("trader joe") || combined.contains("walmart") || combined.contains("costco") || combined.contains("kroger") || combined.contains("target") || combined.contains("supermarket") || combined.contains("blinkit") || combined.contains("instamart") || combined.contains("zepto") || combined.contains("bigbasket") -> "Groceries"
            combined.contains("starbucks") || combined.contains("mcdonald") || combined.contains("chipotle") || combined.contains("restaurant") || combined.contains("cafe") || combined.contains("pizza") || combined.contains("burger") || combined.contains("dining") || combined.contains("coffee") || combined.contains("swiggy") || combined.contains("zomato") -> "Food & Dining"
            combined.contains("uber") || combined.contains("lyft") || combined.contains("taxi") || combined.contains("gas") || combined.contains("shell") || combined.contains("chevron") || combined.contains("metro") || combined.contains("flight") || combined.contains("ola") || combined.contains("rapido") || combined.contains("irctc") || combined.contains("fuel") -> "Travel & Commute"
            combined.contains("netflix") || combined.contains("spotify") || combined.contains("electric") || combined.contains("utility") || combined.contains("water") || combined.contains("bill") || combined.contains("recharge") || combined.contains("internet") || combined.contains("att") || combined.contains("verizon") || combined.contains("bescom") || combined.contains("airtel") || combined.contains("jio") -> "Bills & Utilities"
            combined.contains("amazon") || combined.contains("apple") || combined.contains("ebay") || combined.contains("best buy") || combined.contains("nike") || combined.contains("zara") || combined.contains("flipkart") || combined.contains("myntra") -> "Shopping"
            combined.contains("upi") || combined.contains("vpa") || combined.contains("gpay") || combined.contains("phonepe") || combined.contains("paytm") || combined.contains("bhim") || combined.contains("cred") -> "UPI"
            type == ExpenseType.TRANSFER || combined.contains("zelle") || combined.contains("venmo") || combined.contains("transfer") || combined.contains("imps") || combined.contains("neft") -> "Transfers"
            combined.contains("store") || combined.contains("mall") || combined.contains("shop") -> "Shopping"
            combined.contains("food") -> "Food & Dining"
            combined.contains("grocery") -> "Groceries"
            else -> "General Spend"
        }
    }

    /**
     * Quickly checks if an SMS message is a candidate financial transaction message
     * prior to dispatching to AI or full parsing.
     */
    fun isCandidateFinancialSms(smsBody: String, sender: String = ""): Boolean {
        val clean = smsBody.trim()
        if (clean.length < 10) return false

        // Exclude clear OTPs
        for (pattern in EXCLUSION_PATTERNS) {
            if (pattern.matcher(clean).find()) {
                if (clean.contains("otp", ignoreCase = true) || clean.contains("verification code", ignoreCase = true)) {
                    return false
                }
            }
        }

        val lower = clean.lowercase(Locale.US)
        val hasDebit = DEBIT_KEYWORDS.any { lower.contains(it) }
        val hasTransfer = TRANSFER_KEYWORDS.any { lower.contains(it) }
        val hasSpend = SPEND_KEYWORDS.any { lower.contains(it) }
        val hasUpi = lower.contains("upi") || lower.contains("vpa")
        val looksBank = isBankSender(sender)
        val hasAmount = AMOUNT_PATTERNS.any { it.matcher(clean).find() }

        return (hasDebit || hasTransfer || hasSpend || hasUpi || (looksBank && hasAmount)) && hasAmount
    }
}
