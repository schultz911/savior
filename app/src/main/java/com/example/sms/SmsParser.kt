package com.example.sms

import com.example.data.ExpenseType
import java.util.Locale
import java.util.regex.Pattern

object SmsParser {

    // Negative keywords: Non-expenditures, OTPs, incoming credits, ads, intimations
    private val EXCLUSION_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(otp|one time password|verification code|security code|is your code|secret code)\\b"),
        Pattern.compile("(?i)\\b(pre-approved|apply now|congratulations|click here|claim your|loan offer|discount on|cash prize)\\b"),
        Pattern.compile("(?i)\\b(salary credited|deposited)\\b")
    )

    // Keywords identifying refunds, reversals, and chargebacks
    private val REFUND_KEYWORDS = listOf(
        "refund", "refunded", "reversal", "reversed", "credited back", "returned", "reversed to your"
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
        Pattern.compile("""(?i)(?:debited|spent|paid|charged|amount of|sum of|txn of|withdrawn|refund of|reversed|refund)\s*(?:of|by|for|with)?\s*([$€£¥₹]|Rs\.?|INR|USD|EUR|GBP)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""")
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
        // 1. Info / BIL*REFUND*FLIPKART or INFO: BIL-REV-SWIGGY
        Pattern.compile("""(?i)(?:info[:\s]+(?:bil|ips|inf|info|txn)?[*_\s-]*(?:refund|rev|reversal|ret)[*_\s-]+)([A-Za-z0-9&.\-_/ ]{2,30}?)(?:\s*(?:on\b|ref\b|avl\b|bal\b)|[.!,;]|$)"""),
        // 2. Specific refund/reversal phrases: towards refund from / reversal of txn at / refund for order at
        Pattern.compile("""(?i)(?:towards\s+refund\s+from|refund\s+from|refunded\s+(?:from|by|at|towards)\s+|reversal\s+of\s+(?:(?:upi\s+)?txn\s+(?:at|to)\s+)?|returned\s+from|refund\s+for\s+(?:(?:order|txn)\s+(?:at|on|from)\s+)?|refunded\s+for\s+(?:(?:order|txn)\s+(?:at|on|from)\s+)?|(?:refund|refunded|reversal|credited\s+back)\s+(?:from|for|at|by)\s+|refund\s+(?:of|for)\s+(?:upi\s+)?txn\s+to\s+|(?:refund|refunded|credited|reversal).{0,50}?\bfrom\s+)([A-Za-z0-9&.\-_/@ ]{2,35}?)(?:\s+(?:to\b|on\b|via\b|using\b|upi\s+ref\b|ref\s+no\b|ref\b|avl\b|bal\b|dated\b)|[.!,;]|$)"""),
        // 3. Card reversal at / done on Card at merchant
        Pattern.compile("""(?i)(?:reversal\s+of\s+.{0,40}?\bat\s+|done\s+on\s+(?:card|a/c).{0,30}?\bat\s+)([A-Za-z0-9&.\-_/ ]{2,30}?)(?:\s*(?:on\b|dated\b)|[.!,;]|$)"""),
        // 4. Refund of Rs X from <Merchant> or Rs X refunded to ... from/by/at/for <Merchant>
        Pattern.compile("""(?i)(?:refund\s+(?:of\s+)?(?:rs\.?|inr|[$€£])?\s*[0-9,.]+\s+from\s+|(?:amount\s+of\s+)?(?:rs\.?|inr|[$€£])?\s*[0-9,.]+\s+(?:has\s+been\s+)?refunded\s+(?:to\s+.{0,35}?\s+)?(?:from|by|at|for)\s+)([A-Za-z0-9&.\-_/@ ]{2,35}?)(?:\s*(?:is\s+credited|credited|on\b|via\b|using\b|ref\b|avl\b)|[.!,;]|$)"""),
        // 5. Transfer to / paid to / towards / in favor of
        Pattern.compile("""(?i)(?:towards\s+transfer\s+to|transfer(?:red)?\s+to|sent\s+to|paid\s+to|via\s+upi\s+to|by\s+upi\s+to|upi\s+to|to\s+vpa|in\s+favor\s+of)\s+([A-Za-z0-9&.\-_/@ ]{2,35}?)(?:\s+(?:on|via|using|upi\s+ref|ref\s+no|ref|avl|bal|dated)\b|[.!,;]|$)"""),
        // 6. Sent / paid / transferred ... to
        Pattern.compile("""(?i)(?:sent|paid|transferred)\s+.{0,45}?\bto\s+([A-Za-z0-9&.\-_/@ ]{2,35}?)(?:\s+(?:on|via|using|upi\s+ref|ref\s+no|ref|avl|bal|dated)\b|[.!,;]|$)"""),
        // 7. Zelle / UPI / Venmo to
        Pattern.compile("""(?i)(?:with|via)\s+(?:zelle|upi|venmo)\s+to\s+([A-Za-z0-9&.\-_/@ ]{2,35}?)(?:\s+(?:on|via|using|ref)\b|[.!,;]|$)"""),
        // 8. UPI P2M / P2A
        Pattern.compile("""(?i)(?:upi/(?:p2m|p2a)/[0-9]+/)([A-Za-z0-9&.\-_/ ]{2,30})"""),
        // 9. Spent at / purchase at / charged at / swiped at / approved at / refunded at
        Pattern.compile("""(?i)(?:spent\s+at|purchase\s+at|charged\s+at|swiped\s+at|approved\s+at|refunded\s+at|\bat)\s+([A-Za-z0-9&.\-_/ ]{2,30}?)(?:\s+(?:on|via|for|using|ref|avl|bal|dated)\b|[.!,;]|$)"""),
        // 10. Debited ... to
        Pattern.compile("""(?i)(?:debited\s+.{0,40}\s+to)\s+([A-Za-z0-9&.\-_/@ ]{2,30}?)(?:\s+(?:on|via|using|ref)\b|[.!,;]|$)"""),
        // 11. General info prefix
        Pattern.compile("""(?i)(?:info[:\s]+)([A-Za-z0-9&.\-_/ ]{2,30}?)(?:\s+(?:on|ref|avl)\b|[.!,;]|$)""")
    )

    // Pre-compiled hot-path patterns
    private val RECEIVED_CREDIT_PATTERN = Pattern.compile("(?i)\\b(received (?:rs\\.?|usd|\\$)?\\s*\\d+)\\b")
    private val TRANSFER_REGEX_1 = Regex("""(?i)\b(?:sent|transferred|transfer)\b.{1,35}\bto\b""")
    private val TRANSFER_REGEX_2 = Regex("""(?i)\b(?:upi/p2a/|p2p)\b""")

    private val PREFIX_ARTICLE_REGEX = Regex("(?i)^(the|a|an)\\s+")
    private val TRANSFER_PREFIX_REGEX = Regex("(?i)^transfer(?:red)?\\s+to\\s+")
    private val PREFIX_ORDER_REGEX = Regex("""(?i)^(?:your\s+)?(?:order|txn|transaction|purchase)(?:\s+(?:at|on|from|to|for|#\w+))+\s+""")
    private val PREFIX_BIL_REFUND_REGEX = Regex("""(?i)^(?:bil|ips|inf|info|txn)[*_\s-]*(?:refund|rev|reversal|ret)[*_\s-]+""")
    private val SUFFIX_ENTITY_REGEX = Regex("(?i)\\s+(ltd|inc|corp|co|llc|pvt|services|vpa)$")
    private val SPECIAL_CHARS_REGEX = Regex("[*#_/]")

    // Hot-path pre-compiled regex constants for pending refund timing and candidate merchant sanitization
    private val PENDING_TIMING_REGEX = Regex("""(?i)\b(?:in|within|takes?)\s+\d+(?:-\d+|\s+to\s+\d+)?\s*(?:days|hrs|hours)\b""")
    private val REFUND_CONTEXT_PREFIX_REGEX = Regex("""(?i)^(?:cancelled\s+)?(?:order|ride|purchase|txn|transaction|booking)\s+(?:at|on|with|from|to)\s+""")
    private val REFUND_LEADING_PREP_REGEX = Regex("""(?i)^(?:at|on|from|to|with)\s+""")
    private val REFUND_TRAILING_ORDER_REGEX = Regex("""(?i)\s+(?:for|towards)\s+(?:(?:your|the|cancelled)?\s*(?:order|ride|purchase|txn|transaction|booking).*)$""")
    private val REFUND_TRAILING_REF_REGEX = Regex("""[-/]\d{4,}$""")

    // Dedicated refund-merchant extraction patterns (ordered most-specific → least-specific)
    private val REFUND_MERCHANT_PATTERNS = listOf(
        // 1. BIL*REFUND*FLIPKART, BIL*FLIPKART*REFUND, INFO: BIL-REV-SWIGGY, NEFT-REFUND-MAKEMYTRIP
        Pattern.compile("""(?i)(?:bil|ips|inf|info|txn|neft|imps|upi|rtgs)[*_\s-]*(?:refund|rev|reversal|ret)[*_\s-]+([A-Za-z0-9&.\-_/ ]{2,30}?)(?:\s+(?:on|via|dated|ref|avl|bal)\b|[.!,;]|$)"""),
        Pattern.compile("""(?i)(?:bil|ips|inf|info|txn|neft|imps|upi|rtgs)[*_\s-]+([A-Za-z0-9&.\-_/ ]{2,30}?)[*_\s-]+(?:refund|rev|reversal|ret)(?:\s+(?:on|via|dated|ref|avl|bal)\b|[.!,;]|$)"""),

        // 2. "towards refund / reversal / credit back / purchase / order"
        Pattern.compile("""(?i)towards\s+(?:(?:refund|reversal|credit\s+back)\s+)?(?:of\s+(?:(?:upi\s+)?txn|transaction|order|purchase|ride|booking)\s+)?(?:from|by|at|on|to|with)\s+([A-Za-z0-9&.\-_/@ ]{2,35}?)(?:\s+(?:on|via|dated|ref|avl|bal)\b|[.!,;]|$)"""),
        Pattern.compile("""(?i)towards\s+(?:(?:refund|reversal|credit\s+back)\s+)?(?:for\s+)?(?:(?:your\s+)?(?:cancelled\s+)?(?:order|ride|purchase|txn|transaction|booking)\s+(?:at|on|from|to|with)\s+)([A-Za-z0-9&.\-_/@ ]{2,35}?)(?:\s+(?:on|via|dated|ref|avl|bal)\b|[.!,;]|$)"""),
        Pattern.compile("""(?i)towards\s+(?:your\s+)?(?:order|purchase|txn|transaction|ride|booking)\s+(?:at|on|from|to|with)\s+([A-Za-z0-9&.\-_/@ ]{2,35}?)(?:\s+(?:on|via|dated|ref|avl|bal)\b|[.!,;]|$)"""),

        // 3. "refund for <order/ride/purchase> at/on/with/from <Merchant>" or "refund of <Amount> for <order> at <Merchant>"
        Pattern.compile("""(?i)\brefund\s+(?:of\s+(?:rs\.?|inr|usd|[$€£])?\s*[0-9,.]+\s+)?(?:for\s+.{1,35}?\b(?:at|on|from|to|with)\s+|(?:from|at|with)\s+)([A-Za-z0-9&.\-_/@ ]{2,35}?)(?:\s+(?:is|has|on|via|dated|ref|credited|avl|bal)\b|[.!,;]|$)"""),
        Pattern.compile("""(?i)\brefund\s+for\s+(?:(?:your\s+)?(?:cancelled\s+)?(?:order|ride|purchase|txn|transaction|booking)\s+(?:at|on|from|to|with)\s+)?([A-Za-z0-9&.\-_/@ ]{2,35}?)(?:\s+(?:of\s+(?:rs\.?|inr|usd|[$€£])|is|has|on|via|dated|ref|credited|avl|bal)\b|[.!,;]|$)"""),
        Pattern.compile("""(?i)\brefund\s+(?:from|at)\s+([A-Za-z0-9&.\-_/@ ]{2,35}?)\s+(?:of\s+(?:rs\.?|inr|usd|[$€£])?\s*[0-9,.]+\s+)?(?:\s+(?:is|has|on|via|dated|ref|credited|avl|bal)\b|[.!,;]|$)"""),

        // 4. "refund initiated by <Merchant>" / "reversed by <Merchant>"
        Pattern.compile("""(?i)(?:refund|reversal)\s+(?:initiated\s+by|done\s+(?:at|by))\s+([A-Za-z0-9&.\-_/@ ]{2,35}?)(?:\s+(?:has|is|on|via|dated|ref)\b|[.!,;]|$)"""),

        // 5. "credited back / refunded / reversed to card/ac from/at <Merchant>"
        Pattern.compile("""(?i)(?:credited\s+(?:back\s+)?|refunded\s+|reversed\s+)(?:to\s+(?:your\s+)?(?:a/c|account|acct|card|wallet|vpa|upi|source)[^.]*?\s+)?(?:from|at|with)\s+([A-Za-z0-9&.\-_/@ ]{2,35}?)(?:\s+(?:for|on|via|dated|ref|is\s+credited|avl|bal)\b|[.!,;]|$)"""),
        Pattern.compile("""(?i)(?:rs\.?|inr|usd|[$€£])?\s*[0-9,.]+\s+(?:has\s+been\s+)?(?:refunded|reversed|credited\s+back)\s+(?:to\s+(?:your\s+)?(?:a/c|account|card|wallet|source)[^.]*?\s+)?(?:from|at|with)\s+([A-Za-z0-9&.\-_/@ ]{2,35}?)(?:\s+(?:for|on|via|dated|ref|avl|bal)\b|[.!,;]|$)"""),

        // 6. "reversal of (upi) txn at/to/from/on <Merchant>"
        Pattern.compile("""(?i)(?:reversal\s+(?:of\s+(?:(?:upi\s+)?txn\s+)?(?:at|to|from|on|with)\s+)|reversed\s+(?:by|from|at)\s+)([A-Za-z0-9&.\-_/@ ]{2,35}?)(?:\s+(?:on|dated|via|ref)\b|[.!,;]|$)"""),

        // 7. "Your <Merchant> refund"
        Pattern.compile("""(?i)\byour\s+([A-Za-z][A-Za-z0-9&.\- ]{1,25}?)\s+(?:refund|cashback|reversal)\b"""),

        // 8. "cashback from/at <Merchant>"
        Pattern.compile("""(?i)\bcashback\s+(?:of\s+(?:rs\.?|inr|usd|[$€£])?\s*[0-9,.]+\s+)?(?:from|at|by\s+(?!(?:rs\.?|inr|usd|[$€£]|\d)))\s*([A-Za-z0-9&.\-_/@ ]{2,35}?)(?:\s+(?:on|via|has|is|credited)\b|[.!,;]|$)""")
    )

    fun isRefundIntimationOrPending(text: String): Boolean {
        val lower = text.lowercase(Locale.US)

        // If the SMS explicitly confirms that money was "refunded" (past tense), it is an actual refund, NOT a pending intimation!
        if (lower.contains("refunded") &&
            !lower.contains("will be refunded") &&
            !lower.contains("being refunded") &&
            !lower.contains("will reflect") &&
            !lower.contains("will be credited") &&
            !lower.contains("in process")
        ) {
            return false
        }

        // 1. Explicit future-credit promises or pending timing phrasing
        val hasPendingTiming = lower.contains("will be credited") ||
                lower.contains("will reflect") ||
                lower.contains("will be processed") ||
                lower.contains("will be refunded") ||
                lower.contains("would be credited") ||
                lower.contains("shall be credited") ||
                lower.contains("business days") ||
                lower.contains("working days") ||
                PENDING_TIMING_REGEX.containsMatchIn(text)

        // 2. Processing or initiated phrasing
        val hasProcessingOrInitiated = lower.contains("refund processing") ||
                lower.contains("processing refund") ||
                lower.contains("is being processed") ||
                lower.contains("processing your refund") ||
                lower.contains("refund is in process") ||
                lower.contains("refund initiated") ||
                lower.contains("has initiated") ||
                lower.contains("initiated your refund") ||
                lower.contains("initiated a refund") ||
                lower.contains("we have initiated") ||
                lower.contains("refund request") ||
                lower.contains("request for refund")

        // 3. Status updates or reference-only messages that do not confirm actual bank settlement
        val hasConfirmedCredit = hasConfirmedAccountCredit(lower)

        val hasProcessedWithoutCredit = (lower.contains("has been processed") ||
                lower.contains("processed successfully") ||
                lower.contains("refund successful")) && !hasConfirmedCredit

        val hasRefundRefOnly = (lower.contains("refund reference") ||
                lower.contains("refund ref no") ||
                lower.contains("refund ref number") ||
                lower.contains("refund ref.") ||
                lower.contains("refund arn") ||
                lower.contains("refund tracking") ||
                lower.contains("refund id")) && !hasConfirmedCredit

        return hasPendingTiming || hasProcessingOrInitiated || hasProcessedWithoutCredit || hasRefundRefOnly
    }

    private fun hasConfirmedAccountCredit(lower: String): Boolean {
        if (lower.contains("refunded") && !lower.contains("will be refunded") && !lower.contains("being refunded")) {
            return true
        }

        val hasCreditVerb = lower.contains("credited") ||
                lower.contains("refunded") ||
                lower.contains("reversed") ||
                lower.contains("reversal") ||
                lower.contains("credited back")

        val hasAccountContext = lower.contains("a/c") ||
                lower.contains("account") ||
                lower.contains("card") ||
                lower.contains("acct") ||
                lower.contains("ending in") ||
                lower.contains("ending with") ||
                lower.contains("ending") ||
                lower.contains("avl bal") ||
                lower.contains("balance") ||
                lower.contains("vpa") ||
                lower.contains("upi") ||
                lower.contains("wallet") ||
                lower.contains("bank")

        return hasCreditVerb && (hasAccountContext || lower.contains("credited to"))
    }

    fun parse(smsBody: String, sender: String = ""): ParsedSms? {
        val cleanBody = smsBody.trim()
        if (cleanBody.isEmpty()) return null

        // Check for refund intimations, processing, or reference-only messages (never record pending refunds)
        if (isRefundIntimationOrPending(cleanBody)) {
            return null
        }

        // Check for exclusions (OTP, loans, pure deposits)
        for (pattern in EXCLUSION_PATTERNS) {
            if (pattern.matcher(cleanBody).find()) {
                return null
            }
        }

        val lower = cleanBody.lowercase(Locale.US)

        // 1. Check for Credit Reversal & Refund (only confirmed settled refunds)
        val isRefund = !isRefundIntimationOrPending(cleanBody) &&
                REFUND_KEYWORDS.any { lower.contains(it) } &&
                hasConfirmedAccountCredit(lower)

        if (isRefund) {
            val (amount, currency) = extractAmountAndCurrency(cleanBody) ?: return null
            if (amount <= 0.0) return null
            val accountInfo = extractAccountInfo(cleanBody)
            // Use dedicated refund-merchant extractor first, fall back to general extractor
            val merchant = extractRefundMerchant(cleanBody)
            return ParsedSms(
                amount = amount,
                currency = currency,
                type = ExpenseType.MERCHANT,
                title = merchant,
                accountInfo = accountInfo,
                category = "Refund",
                isExpense = false,
                rawText = cleanBody,
                isRefund = true
            )
        }

        // If it's a general credit alert without debit or refund keywords, ignore
        if (cleanBody.contains("credited", ignoreCase = true) && !cleanBody.contains("debited", ignoreCase = true) && !isRefund) {
            return null
        }
        if (RECEIVED_CREDIT_PATTERN.matcher(cleanBody).find() && !isRefund) {
            return null
        }

        // Determine if message is an expenditure
        val isTransfer = TRANSFER_KEYWORDS.any { lower.contains(it) } ||
                TRANSFER_REGEX_1.containsMatchIn(cleanBody) ||
                TRANSFER_REGEX_2.containsMatchIn(cleanBody)
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

        val isCardPurchase = cleanBody.contains(" at ", ignoreCase = true) ||
                lower.contains("spent on") ||
                lower.contains("purchase of") ||
                lower.contains("charged on") ||
                lower.contains("swiped") ||
                lower.contains("pos txn") ||
                lower.contains("paid at")

        val isPaymentTowardsCard = (lower.contains("towards") && (lower.contains("card") || lower.contains("credit card"))) ||
                lower.contains("credit card bill") ||
                lower.contains("card dues") ||
                lower.contains("bill payment for card") ||
                lower.contains("autopay for card") ||
                lower.contains("card repayment") ||
                lower.contains("paid to cred") ||
                lower.contains("payment to cred") ||
                (lower.contains("payment received") && lower.contains("card"))

        val isCreditCardBill = isPaymentTowardsCard && !isCardPurchase
        val isSelfTransfer = lower.contains("self") || lower.contains("own account") ||
                lower.contains("to own") || lower.contains("linked account") ||
                lower.contains("between your accounts") || lower.contains("to my account")

        // Extract Amount and Currency
        val (amount, currency) = extractAmountAndCurrency(cleanBody) ?: return null

        if (amount <= 0.0) return null

        // Extract Account Info
        val accountInfo = extractAccountInfo(cleanBody)

        // Extract Merchant / Recipient (Never use bank name or SMS sender)
        val merchant = when {
            isCreditCardBill -> "Credit Card Bill"
            isSelfTransfer -> "Self Transfer"
            else -> extractMerchant(cleanBody, isTransfer)
        }

        val lowerMerchant = merchant.lowercase(Locale.US)
        val isStoreOrMerchant = listOf(
            "store", "shop", "mart", "swiggy", "zomato", "amazon", "flipkart",
            "uber", "ola", "cafe", "coffee", "restaurant", "hotel", "supermarket",
            "general", "bills", "bescom", "electricity", "retail", "foods", "whole foods"
        ).any { lowerMerchant.contains(it) } || cleanBody.contains(" at ", ignoreCase = true)

        val isEffectiveTransfer = isTransfer || merchant.contains("@") ||
                lower.contains("sent to") || lower.contains("sent rs") || lower.contains("sent inr") ||
                lower.contains("sent ₹") || lower.contains("transferred to") || lower.contains("transfer to") ||
                lower.contains("paid to") || lower.contains("upi/p2p")

        // Categorize & Resolve Expense Type
        val detectedCategory = when {
            isCreditCardBill -> "Credit Card Bill"
            isSelfTransfer -> "Self"
            else -> categorize(cleanBody, merchant, if (isEffectiveTransfer) ExpenseType.P2P else ExpenseType.MERCHANT)
        }

        val finalExpenseType = when {
            isCreditCardBill -> ExpenseType.CREDIT_CARD
            isSelfTransfer -> ExpenseType.SELF
            detectedCategory == "Transfers" -> ExpenseType.P2P
            detectedCategory != "General Spend" -> ExpenseType.MERCHANT
            isStoreOrMerchant -> ExpenseType.MERCHANT
            isEffectiveTransfer -> ExpenseType.P2P
            else -> ExpenseType.MERCHANT
        }

        return ParsedSms(
            amount = amount,
            currency = currency,
            type = finalExpenseType,
            title = merchant,
            accountInfo = accountInfo,
            category = detectedCategory,
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
                    val matchedText = matcher.group(0)?.lowercase(Locale.US) ?: ""
                    val isCard = matchedText.contains("card") || (!matchedText.contains("a/c") && !matchedText.contains("account") && text.contains("card", ignoreCase = true))
                    val isAccount = matchedText.contains("a/c") || matchedText.contains("account") || matchedText.contains("acct")
                    val isUpi = pattern.pattern().contains("upi", ignoreCase = true) || (!isAccount && !isCard && text.contains("upi", ignoreCase = true))
                    val prefix = when {
                        isCard -> "Card ••"
                        isAccount -> "A/c ••"
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

    private fun extractMerchant(text: String, isTransfer: Boolean): String {
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val match = matcher.group(1)?.trim() ?: ""
                val clean = cleanMerchantName(match)
                if (clean.length in 2..35 && !isBankName(clean) && clean.any { it.isLetter() } && !clean.startsWith("UPI (", ignoreCase = true)) {
                    return clean
                }
            }
        }

        // Never fall back to bank name or SMS sender!
        return if (isTransfer) "Transfer Recipient" else "Merchant / Payee"
    }

    /**
     * Dedicated refund merchant extractor.
     * Runs exhaustive refund-specific patterns first, then falls back to the
     * generic merchant extractor. Only returns "Refund / Reversal" as a true last resort.
     */
    fun isSpecificMerchant(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val clean = name.trim()
        if (clean.length < 2 || clean.length > 35) return false
        if (!clean.any { it.isLetter() }) return false
        val lower = clean.lowercase(Locale.US)
        val genericWords = setOf(
            "refund", "reversal", "refund / reversal", "merchant / payee", "transfer recipient",
            "unknown", "your", "a c", "account", "card", "rs", "inr", "usd", "cashback",
            "order", "txn", "transaction", "purchase", "booking", "source", "upi", "vpa",
            "cancelled ride", "cancelled", "payment", "spend", "general", "bank"
        )
        if (genericWords.contains(lower)) return false
        if (lower.startsWith("rs ") || lower.startsWith("inr ") || lower.startsWith("usd ") || lower.startsWith("rs.")) return false
        if (lower.startsWith("upi (") && lower.endsWith(")")) return false
        if (isBankName(clean)) return false
        return true
    }

    /**
     * Dedicated refund merchant extractor.
     * Runs exhaustive refund-specific patterns first, then falls back to the
     * generic merchant extractor. Only returns "Refund / Reversal" as a true last resort.
     */
    fun extractRefundMerchant(text: String): String {
        // 1. Try all dedicated refund-specific patterns first
        for (pattern in REFUND_MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val match = matcher.group(1)?.trim() ?: ""
                val clean = cleanMerchantName(match)
                if (isSpecificMerchant(clean)) {
                    return clean
                }
            }
        }

        // 2. Fall back to generic merchant extractor (covers POS/spend patterns that may
        //    appear in reversal SMS like "reversal at <merchant>")
        val genericResult = extractMerchant(text, false)
        if (isSpecificMerchant(genericResult)) {
            return genericResult
        }

        // 3. True last resort
        return "Refund / Reversal"
    }

    private fun isBankName(name: String): Boolean {
        val lower = name.lowercase(Locale.US).trim()
        val bankKeywords = listOf(
            "hdfc", "icici", "sbi", "axis", "kotak", "pnb", "bob", "boi",
            "canara", "idfc", "indus", "yes bank", "rbl", "chase", "citi",
            "amex", "wells fargo", "bank of america", "bank", "federal bank",
            "central bank", "union bank", "card ending", "account ending"
        )
        return bankKeywords.any { lower == it || lower == "$it bank" || lower.startsWith("$it ") }
    }

    private fun cleanMerchantName(name: String): String {
        var raw = name.trim().trimEnd('.', ',', ';', ':', '-', ' ')
        // If it's a VPA handle like swiggy@icici or 9876543210@paytm
        if (raw.contains("@")) {
            val prefix = raw.substringBefore("@").trim()
            if (prefix.all { it.isDigit() }) {
                return "UPI (${prefix.takeLast(4)})"
            }
            return raw.trimEnd('.', ',')
        }

        var clean = raw.replace(PREFIX_ARTICLE_REGEX, "")
            .replace(TRANSFER_PREFIX_REGEX, "")
            .replace(PREFIX_BIL_REFUND_REGEX, "")
            .replace(PREFIX_ORDER_REGEX, "")
            .replace(SUFFIX_ENTITY_REGEX, "")
            .replace(SPECIAL_CHARS_REGEX, " ")
            .trim()
            .trimEnd('.', ',', ';', ':', '-', ' ')

        // Strip contextual prefixes like "order on ", "order at ", "cancelled ride with ", "ride with "
        clean = clean.replace(REFUND_CONTEXT_PREFIX_REGEX, "")
            .replace(REFUND_LEADING_PREP_REGEX, "")
            .replace(REFUND_TRAILING_ORDER_REGEX, "")
            .trim()

        // Strip trailing reference numbers like "-01234" or "/9876"
        clean = clean.replace(REFUND_TRAILING_REF_REGEX, "").trim()

        // Capitalize words nicely
        val words = clean.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                val cleanWord = word.trimEnd('.', ',')
                if (cleanWord.length <= 3 && cleanWord.all { it.isLetter() }) cleanWord.uppercase(Locale.US)
                else cleanWord.lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) }
            }

        return if (words.any { it.isLetter() }) words else "Merchant / Payee"
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
     * Categorizes spend into standard SAVIO categories (no UPI category).
     */
    private fun categorize(text: String, merchant: String, type: ExpenseType): String {
        val combined = "$text $merchant".lowercase(Locale.US)

        return when {
            type == ExpenseType.CREDIT_CARD || combined.contains("credit card bill") ||
                combined.contains("towards your credit card") || combined.contains("card dues") ||
                combined.contains("paid to cred") || combined.contains("payment to cred") -> "Credit Card Bill"

            type == ExpenseType.SELF || combined.contains("self transfer") || combined.contains("own account") ||
                combined.contains("to own") || combined.contains("to self") || combined.contains("linked account") ||
                combined.contains("to my account") || combined.contains("self a/c") -> "Self"

            combined.contains("openrouter") || combined.contains("railway") || combined.contains("exitlag") ||
                combined.contains("torbox") || combined.contains("onedrive") || combined.contains("google play") ||
                combined.contains("play store") || combined.contains("google storage") || combined.contains("google one") ||
                combined.contains("quillbot") || combined.contains("openai") || combined.contains("chatgpt") ||
                combined.contains("claude") || combined.contains("anthropic") || combined.contains("github") ||
                combined.contains("cursor") || combined.contains("copilot") || combined.contains("replit") ||
                combined.contains("vercel") || combined.contains("netlify") || combined.contains("heroku") ||
                combined.contains("render") || combined.contains("supabase") || combined.contains("firebase") ||
                combined.contains("aws") || combined.contains("amazon web services") || combined.contains("digitalocean") ||
                combined.contains("linode") || combined.contains("cloudflare") || combined.contains("godaddy") ||
                combined.contains("namecheap") || combined.contains("hostinger") || combined.contains("notion") ||
                combined.contains("slack") || combined.contains("zoom") || combined.contains("canva") ||
                combined.contains("adobe") || combined.contains("midjourney") || combined.contains("figma") ||
                combined.contains("linear") || combined.contains("jira") || combined.contains("atlassian") ||
                combined.contains("dropbox") || combined.contains("1password") || combined.contains("bitwarden") ||
                combined.contains("nordvpn") || combined.contains("expressvpn") || combined.contains("surfshark") ||
                combined.contains("proton") || combined.contains("microsoft 365") || combined.contains("office 365") ||
                combined.contains("jetbrains") || combined.contains("grammarly") || combined.contains("laundrymate") ||
                combined.contains("urban company") || combined.contains("icloud") || combined.contains("apple services") ||
                combined.contains("electric") || combined.contains("utility") || combined.contains("water") ||
                combined.contains("gas bill") || combined.contains("piped gas") || combined.contains("lpg") ||
                combined.contains("cylinder") || combined.contains("indane") || combined.contains("hp gas") ||
                combined.contains("bharat gas") || combined.contains("bill") || combined.contains("recharge") ||
                combined.contains("internet") || combined.contains("broadband") || combined.contains("bescom") ||
                combined.contains("airtel") || combined.contains("jio") || combined.contains(" vi ") ||
                combined.contains("vi recharge") || combined.contains("vodafone") ||
                combined.contains("dth") || combined.contains("tataplay") || combined.contains("subscription") ||
                combined.contains("software") || combined.contains("saas") || combined.contains("hosting") ||
                combined.contains("cloud") -> "Bills & Utilities"

            combined.contains("pharma") || combined.contains("pharmacy") || combined.contains("chemist") ||
                combined.contains("apollo") || combined.contains("pharmeasy") || combined.contains("1mg") ||
                combined.contains("netmeds") || combined.contains("medplus") || (combined.contains("hospital") && !combined.contains("hospitality")) ||
                combined.contains("clinic") || combined.contains("doctor") || combined.contains("dr.") ||
                combined.contains("diagnostic") || combined.contains("pathology") || combined.contains("lab") ||
                combined.contains("medicine") || combined.contains("medicos") || combined.contains("medical") ||
                combined.contains("cult.fit") || combined.contains("gym") || combined.contains("fitness") ||
                combined.contains("dental") || combined.contains("healthcare") -> "Health & Wellness"

            combined.contains("whole foods") || combined.contains("trader joe") || combined.contains("walmart") ||
                combined.contains("costco") || combined.contains("kroger") || combined.contains("target") ||
                combined.contains("supermarket") || combined.contains("blinkit") || combined.contains("instamart") ||
                combined.contains("swiggy instamart") ||
                combined.contains("zepto") || combined.contains("bigbasket") || combined.contains("general store") ||
                combined.contains("kirana") || combined.contains("provision") || combined.contains("grocery") -> "Groceries"

            combined.contains("starbucks") || combined.contains("mcdonald") || combined.contains("chipotle") ||
                combined.contains("restaurant") || combined.contains("cafe") || combined.contains("pizza") ||
                combined.contains("burger") || combined.contains("dining") || combined.contains("coffee") ||
                (combined.contains("swiggy") && !combined.contains("instamart")) || combined.contains("zomato") || combined.contains("food") ||
                combined.contains("bakery") || combined.contains("dhaba") || combined.contains("biryani") ||
                combined.contains("hospitality") -> "Food & Dining"

            combined.contains("uber") || combined.contains("lyft") || combined.contains("taxi") ||
                combined.contains("gas station") || combined.contains("shell") || combined.contains("chevron") ||
                combined.contains("metro") || combined.contains("flight") || combined.contains("ola") ||
                combined.contains("rapido") || combined.contains("irctc") || combined.contains("fuel") ||
                combined.contains("petrol") || combined.contains("diesel") || combined.contains("cng") ||
                combined.contains("fastag") -> "Travel & Commute"

            combined.contains("bookmyshow") || combined.contains("pvr") || combined.contains("inox") ||
                combined.contains("cinema") || combined.contains("movie") || combined.contains("steam") ||
                combined.contains("playstation") || combined.contains("hotstar") || combined.contains("prime video") ||
                combined.contains("netflix") || combined.contains("spotify") || combined.contains("youtube") ||
                combined.contains("disney") || combined.contains("xbox") || combined.contains("gaming") -> "Entertainment"

            combined.contains("amazon") || combined.contains("apple") || combined.contains("ebay") ||
                combined.contains("best buy") || combined.contains("nike") || combined.contains("zara") ||
                combined.contains("flipkart") || combined.contains("myntra") || combined.contains("store") ||
                combined.contains("mall") || combined.contains("shop") || combined.contains("croma") ||
                combined.contains("retail") || combined.contains("ajio") -> "Shopping"

            combined.contains("salon") || combined.contains("spa") || combined.contains("barber") ||
                combined.contains("parlour") || combined.contains("grooming") || combined.contains("skincare") ||
                combined.contains("cosmetics") || combined.contains("urban company") -> "Personal Care"

            combined.contains("zerodha") || combined.contains("groww") || combined.contains("upstox") ||
                combined.contains("mutual fund") || combined.contains("sip") || combined.contains("stocks") ||
                combined.contains("angel one") || combined.contains("smallcase") -> "Investments"

            combined.contains("coursera") || combined.contains("udemy") || combined.contains("unacademy") ||
                combined.contains("school") || combined.contains("college") || combined.contains("university") ||
                combined.contains("tuition") || combined.contains("fee") || combined.contains("books") -> "Education"

            type == ExpenseType.P2P || combined.contains("zelle") || combined.contains("venmo") ||
                combined.contains("transfer") || combined.contains("imps") || combined.contains("neft") ||
                combined.contains("vpa") || combined.contains("sent to") || combined.contains("paid to") -> "Transfers"

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
        val hasRefund = REFUND_KEYWORDS.any { lower.contains(it) }
        val hasUpi = lower.contains("upi") || lower.contains("vpa")
        val looksBank = isBankSender(sender)
        val hasAmount = AMOUNT_PATTERNS.any { it.matcher(clean).find() }

        return (hasDebit || hasTransfer || hasSpend || hasRefund || hasUpi || (looksBank && hasAmount)) && hasAmount
    }
}
