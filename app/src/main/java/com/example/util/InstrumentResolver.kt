package com.example.util

import com.example.data.ExpenseEntity
import com.example.data.ExpenseType
import com.example.data.InstrumentRuleEntity
import com.example.ui.models.InstrumentType
import java.util.Locale
import java.util.regex.Pattern

object InstrumentResolver {

    private val PREFIX_STRIP_REGEX = Regex("""(?i)^(?:a/c|acct|account|card|credit\s+card|upi)[\s:/\-•*]*""")
    private val DIGITS_SUFFIX_REGEX = Regex("""\b\d{3,6}\b|(?<=[^0-9]|^)\d{3,6}$""")
    private val ALPHANUM_SUFFIX_REGEX = Regex("""[0-9A-Za-z]{3,6}$""")
    private val RAW_BODY_ENDING_IN_REGEX = Regex("""(?i)(?:ending\s+in\s+)([0-9A-Za-z]{4})""")
    private val RAW_BODY_MASKED_REGEX = Regex("""(?i)(?:xx|[*]{2,}|••)([0-9A-Za-z]{3,6})""")
    private val RAW_BODY_CARD_AC_REGEX = Regex("""(?i)(?:card|a/c|account|acct)(?:\s*(?:no\.?|ending\s+in|ending|xx|\*+|••|-))*\s*([0-9A-Za-z]{3,6})""")
    private val RAW_BODY_PATTERNS = listOf(
        RAW_BODY_ENDING_IN_REGEX,
        RAW_BODY_MASKED_REGEX,
        RAW_BODY_CARD_AC_REGEX
    )

    private val REGEX_METACHARACTERS = charArrayOf('\\', '[', ']', '(', ')', '{', '}', '.', '*', '+', '?', '^', '$', '|')
    private val MARKER_REGEX_CACHE = java.util.concurrent.ConcurrentHashMap<String, Regex?>()

    /**
     * Finds a matching user-defined instrument rule by evaluating regex markers and last 4 digits.
     */
    fun findMatchingRule(
        accountInfo: String,
        rawBody: String = "",
        sender: String = "",
        rules: List<InstrumentRuleEntity>
    ): InstrumentRuleEntity? {
        if (rules.isEmpty()) return null
        val suffix = extractSuffix(accountInfo, rawBody)?.uppercase(Locale.US)

        for (rule in rules) {
            // 1. Evaluate user regex markers against rawBody, accountInfo, and sender
            val markers = rule.getMarkerList()
            for (marker in markers) {
                val hasMetacharacters = marker.any { it in REGEX_METACHARACTERS }
                if (!hasMetacharacters) {
                    if (rawBody.contains(marker, ignoreCase = true) ||
                        accountInfo.contains(marker, ignoreCase = true) ||
                        sender.contains(marker, ignoreCase = true)
                    ) {
                        return rule
                    }
                } else {
                    val regex = MARKER_REGEX_CACHE.computeIfAbsent(marker) { p ->
                        try {
                            Regex(p, RegexOption.IGNORE_CASE)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (regex != null) {
                        if (regex.containsMatchIn(rawBody) || regex.containsMatchIn(accountInfo) || regex.containsMatchIn(sender)) {
                            return rule
                        }
                    } else {
                        if (rawBody.contains(marker, ignoreCase = true) ||
                            accountInfo.contains(marker, ignoreCase = true) ||
                            sender.contains(marker, ignoreCase = true)
                        ) {
                            return rule
                        }
                    }
                }
            }

            // 2. Check last 4 digits / suffix
            val ruleLast4 = rule.last4.trim().uppercase(Locale.US)
            if (ruleLast4.isNotBlank()) {
                if (suffix != null && suffix == ruleLast4) {
                    return rule
                }
                if (accountInfo.contains(ruleLast4, ignoreCase = true) || rawBody.contains(ruleLast4, ignoreCase = true)) {
                    return rule
                }
            }
        }
        return null
    }

    /**
     * Determines if a transaction has an unidentified instrument.
     */
    fun isUnidentified(
        expense: ExpenseEntity,
        rules: List<InstrumentRuleEntity>
    ): Boolean {
        if (expense.isRefundOrReversal || expense.type == ExpenseType.CREDIT_CARD || expense.type == ExpenseType.SELF) {
            return false
        }
        val matchedRule = findMatchingRule(expense.accountInfo, expense.rawBody, expense.sender, rules)
        if (matchedRule != null) return false

        val suffix = extractSuffix(expense.accountInfo, expense.rawBody)
        val raw = expense.accountInfo.trim()
        return suffix == null || raw.isBlank() || raw.equals("Other / Cash", ignoreCase = true) || raw.contains("Unknown", ignoreCase = true)
    }

    /**
     * Extracts the 3 to 6 character account identifier / last 4 digits (e.g. "4821", "XXXX").
     */
    fun extractSuffix(accountInfo: String, rawBody: String = ""): String? {
        val trimmed = accountInfo.trim()
        if (trimmed.isNotBlank()) {
            val stripped = PREFIX_STRIP_REGEX.replace(trimmed, "").trim()
            val target = if (stripped.isNotBlank()) stripped else trimmed

            // 1. If target contains 3 to 6 digits (e.g. "XX1234", "••4821", "4821"), extract those digits
            val digitsMatch = DIGITS_SUFFIX_REGEX.find(target)
            if (digitsMatch != null) {
                return digitsMatch.value
            }

            // 2. If target is a masked alphanumeric suffix (e.g. "XXXX", "XX48"), extract it
            val alphaNumMatch = ALPHANUM_SUFFIX_REGEX.find(target)
            if (alphaNumMatch != null) {
                val candidate = alphaNumMatch.value
                if (!candidate.equals("cash", ignoreCase = true) && !candidate.equals("null", ignoreCase = true)) {
                    return candidate.uppercase(Locale.US)
                }
            }
        }

        // Try extracting from raw SMS text if accountInfo lacks suffix
        if (rawBody.isNotBlank()) {
            for (p in RAW_BODY_PATTERNS) {
                val m = p.find(rawBody)
                if (m != null) {
                    val candidate = m.groupValues[1]
                    if (candidate.isNotBlank() && !candidate.equals("cash", ignoreCase = true) && !candidate.equals("ending", ignoreCase = true) && !candidate.equals("null", ignoreCase = true)) {
                        return candidate.uppercase(Locale.US)
                    }
                }
            }
        }
        return null
    }

    /**
     * Determines whether the instrument is a Card, Bank Account, UPI, or Other.
     */
    fun determineType(accountInfo: String, rawBody: String = "", expenseType: ExpenseType = ExpenseType.MERCHANT): InstrumentType {
        if (expenseType == ExpenseType.CREDIT_CARD) return InstrumentType.CARD
        val combined = "$accountInfo $rawBody".lowercase(Locale.US)
        return when {
            combined.contains("card") || combined.contains("credit") -> InstrumentType.CARD
            combined.contains("upi") -> InstrumentType.UPI
            combined.contains("a/c") || combined.contains("acct") || combined.contains("account") || combined.contains("bank") -> InstrumentType.BANK_ACCOUNT
            else -> InstrumentType.fromAccountInfo(accountInfo)
        }
    }

    /**
     * Detects bank or financial institution name from SMS sender and body text.
     */
    fun detectBankOrIssuer(sender: String, rawBody: String): String? {
        val s = sender.uppercase(Locale.US)
        val b = rawBody.lowercase(Locale.US)

        // 1. Sender ID matching
        val senderBank = when {
            s.contains("HDFC") -> "HDFC"
            s.contains("ICICI") -> "ICICI"
            s.contains("SBICRD") || s.contains("SBIINB") || s.contains("SBIPSG") || s.endsWith("SBI") || s.contains("-SBI") -> "SBI"
            s.contains("AXIS") -> "Axis"
            s.contains("KOTAK") -> "Kotak"
            s.contains("AMEX") -> "Amex"
            s.contains("CITI") -> "Citi"
            s.contains("IDFC") -> "IDFC FIRST"
            s.contains("RBL") -> "RBL"
            s.contains("INDUS") -> "IndusInd"
            s.contains("PNB") -> "PNB"
            s.contains("BOB") -> "Bank of Baroda"
            s.contains("CANARA") || s.contains("CANBNK") -> "Canara"
            s.contains("YES") -> "Yes Bank"
            s.contains("ONECRD") || s.contains("ONECARD") -> "OneCard"
            s.contains("AUBL") || s.contains("AUBANK") -> "AU Bank"
            s.contains("FED") -> "Federal Bank"
            s.contains("HSBC") -> "HSBC"
            s.contains("SCB") || s.contains("STAN") -> "StanChart"
            else -> null
        }
        if (senderBank != null) return senderBank

        // 2. Body text matching
        return when {
            b.contains("hdfc bank") || b.contains("hdfc") -> "HDFC"
            b.contains("icici bank") || b.contains("icici") -> "ICICI"
            b.contains("state bank of india") || b.contains("sbi card") || b.contains("sbi") -> "SBI"
            b.contains("axis bank") || b.contains("axis") -> "Axis"
            b.contains("kotak bank") || b.contains("kotak mahindra") || b.contains("kotak") -> "Kotak"
            b.contains("american express") || b.contains("amex") -> "Amex"
            b.contains("citibank") || b.contains("citi bank") || b.contains("citi") -> "Citi"
            b.contains("idfc first") || b.contains("idfc bank") || b.contains("idfc") -> "IDFC FIRST"
            b.contains("rbl bank") || b.contains("rbl") -> "RBL"
            b.contains("indusind bank") || b.contains("indusind") -> "IndusInd"
            b.contains("bank of baroda") || b.contains("bob bank") -> "Bank of Baroda"
            b.contains("punjab national bank") || b.contains("pnb") -> "PNB"
            b.contains("canara bank") || b.contains("canara") -> "Canara"
            b.contains("yes bank") -> "Yes Bank"
            b.contains("onecard") -> "OneCard"
            b.contains("federal bank") -> "Federal Bank"
            b.contains("hsbc bank") || b.contains("hsbc") -> "HSBC"
            b.contains("standard chartered") || b.contains("stanchart") -> "StanChart"
            else -> null
        }
    }

    /**
     * Detects specific card product names (e.g. Millennia, Regalia, Amazon Pay, etc.).
     */
    fun detectCardProduct(rawBody: String): String? {
        val b = rawBody.lowercase(Locale.US)
        return when {
            b.contains("millennia") -> "Millennia"
            b.contains("regalia") -> "Regalia"
            b.contains("infinia") -> "Infinia"
            b.contains("tata neu") -> "Tata Neu"
            b.contains("simplyclick") -> "SimplyCLICK"
            b.contains("simplysave") -> "SimplySAVE"
            b.contains("amazon pay") -> "Amazon Pay"
            b.contains("flipkart axis") -> "Flipkart Axis"
            b.contains("coral") -> "Coral"
            b.contains("rubyx") -> "Rubyx"
            b.contains("sapphiro") -> "Sapphiro"
            b.contains("onecard") -> "OneCard"
            b.contains("ace") -> "ACE"
            b.contains("magnus") -> "Magnus"
            b.contains("atlas") -> "Atlas"
            b.contains("swiggy hdfc") -> "Swiggy HDFC"
            else -> null
        }
    }

    /**
     * Constructs a cross-linking map across all transactions.
     * Maps instrument keys (e.g. "CARD_4821", "BANK_ACCOUNT_XXXX") to the richest
     * unified canonical display name (e.g. "HDFC Millennia Card ••4821", "A/c ••XXXX").
     */
    fun buildCrossLinkMap(expenses: List<ExpenseEntity>): Map<String, String> {
        data class Meta(
            val type: InstrumentType,
            val suffix: String,
            val banks: MutableSet<String> = mutableSetOf(),
            val products: MutableSet<String> = mutableSetOf()
        )

        val metaMap = mutableMapOf<String, Meta>()

        for (exp in expenses) {
            val suffix = extractSuffix(exp.accountInfo, exp.rawBody) ?: continue
            val type = determineType(exp.accountInfo, exp.rawBody, exp.type)
            val key = "${type.name}_$suffix"

            val entry = metaMap.getOrPut(key) { Meta(type, suffix) }
            val bank = detectBankOrIssuer(exp.sender, exp.rawBody)
            if (bank != null) entry.banks.add(bank)

            val product = detectCardProduct(exp.rawBody)
            if (product != null) entry.products.add(product)
        }

        val resultMap = mutableMapOf<String, String>()
        for ((key, meta) in metaMap) {
            val bank = meta.banks.firstOrNull()
            val product = meta.products.firstOrNull()
            val suffix = meta.suffix

            val canonicalName = when (meta.type) {
                InstrumentType.CARD -> {
                    when {
                        bank != null && product != null -> {
                            if (product.startsWith(bank, ignoreCase = true)) {
                                "$product Card ••$suffix"
                            } else {
                                "$bank $product Card ••$suffix"
                            }
                        }
                        bank != null -> "$bank Card ••$suffix"
                        product != null -> "$product Card ••$suffix"
                        else -> "Card ••$suffix"
                    }
                }
                InstrumentType.BANK_ACCOUNT -> {
                    if (bank != null) "$bank A/c ••$suffix" else "A/c ••$suffix"
                }
                InstrumentType.UPI -> {
                    if (bank != null) "$bank UPI ••$suffix" else "UPI ••$suffix"
                }
                InstrumentType.OTHER -> {
                    if (bank != null) "$bank ••$suffix" else "A/c ••$suffix"
                }
            }
            resultMap[key] = canonicalName
        }

        return resultMap
    }

    /**
     * Resolves an individual transaction to its canonical instrument name.
     * Evaluates user-defined instrument rules first before falling back to crossLinkMap or standard extraction.
     */
    fun resolveInstrument(
        expense: ExpenseEntity,
        crossLinkMap: Map<String, String> = emptyMap(),
        rules: List<InstrumentRuleEntity> = emptyList()
    ): String {
        if (expense.type == ExpenseType.CREDIT_CARD || expense.category.equals("Credit Card Bill", ignoreCase = true)) {
            return "Credit Card Bills"
        }

        // 1. Check user-defined instrument rules first
        val matchedRule = findMatchingRule(expense.accountInfo, expense.rawBody, expense.sender, rules)
        if (matchedRule != null) {
            return matchedRule.canonicalName
        }

        val raw = expense.accountInfo.trim()
        if (raw.isBlank() || raw.equals("null", ignoreCase = true) || raw.equals("Other / Cash", ignoreCase = true) || raw.equals("Cash / Manual", ignoreCase = true)) {
            return "Other / Cash"
        }

        val suffix = extractSuffix(raw, expense.rawBody)
        val type = determineType(raw, expense.rawBody, expense.type)

        if (suffix != null) {
            val key = "${type.name}_$suffix"
            val mapped = crossLinkMap[key]
            if (mapped != null) return mapped

            // Fallback canonical formatting if not in crossLinkMap
            val bank = detectBankOrIssuer(expense.sender, expense.rawBody)
            return when (type) {
                InstrumentType.CARD -> if (bank != null) "$bank Card ••$suffix" else "Card ••$suffix"
                InstrumentType.BANK_ACCOUNT -> if (bank != null) "$bank A/c ••$suffix" else "A/c ••$suffix"
                InstrumentType.UPI -> if (bank != null) "$bank UPI ••$suffix" else "UPI ••$suffix"
                InstrumentType.OTHER -> if (bank != null) "$bank ••$suffix" else "A/c ••$suffix"
            }
        }

        // Clean A/C prefix variations even if no standard suffix could be isolated
        return cleanRawAccountInfo(raw)
    }

    /**
     * Cleans up casing and formatting discrepancies like A/CXXXX vs A/cXXXX.
     */
    fun cleanRawAccountInfo(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("a/c", ignoreCase = true)) {
            val rest = trimmed.substring(3).trimStart(' ', ':', '-', '•', '*')
            return if (rest.isNotBlank()) "A/c ••$rest" else "A/c"
        }
        if (trimmed.startsWith("card", ignoreCase = true)) {
            val rest = trimmed.substring(4).trimStart(' ', ':', '-', '•', '*')
            return if (rest.isNotBlank()) "Card ••$rest" else "Card"
        }
        if (trimmed.startsWith("upi", ignoreCase = true)) {
            val rest = trimmed.substring(3).trimStart(' ', ':', '-', '•', '*')
            return if (rest.isNotBlank()) "UPI ••$rest" else "UPI"
        }
        return trimmed
    }

    /**
     * Checks if a given transaction matches the selected instrument filter.
     */
    fun matchesInstrument(
        expense: ExpenseEntity,
        selectedAccount: String?,
        crossLinkMap: Map<String, String> = emptyMap(),
        rules: List<InstrumentRuleEntity> = emptyList()
    ): Boolean {
        if (selectedAccount == null) return true

        if (selectedAccount.equals("Credit Card Bills", ignoreCase = true)) {
            return expense.type == ExpenseType.CREDIT_CARD ||
                    expense.category.equals("Credit Card Bill", ignoreCase = true) ||
                    expense.accountInfo.contains("Credit", ignoreCase = true) ||
                    expense.accountInfo.contains("Card", ignoreCase = true)
        }

        if (selectedAccount.equals("Other / Cash", ignoreCase = true)) {
            val raw = expense.accountInfo.trim()
            return raw.isBlank() ||
                    raw.equals("null", ignoreCase = true) ||
                    raw.equals("Other / Cash", ignoreCase = true) ||
                    raw.equals("Cash / Manual", ignoreCase = true) ||
                    resolveInstrument(expense, crossLinkMap, rules).equals("Other / Cash", ignoreCase = true)
        }

        val resolved = resolveInstrument(expense, crossLinkMap, rules)
        if (resolved.equals(selectedAccount, ignoreCase = true)) return true

        // Deep matching by suffix & type (e.g. matching A/CXXXX with A/c ••XXXX or HDFC Card ••4821 with Card ••4821)
        val selectedSuffix = extractSuffix(selectedAccount)
        val expenseSuffix = extractSuffix(expense.accountInfo, expense.rawBody)
        if (selectedSuffix != null && expenseSuffix != null && selectedSuffix.equals(expenseSuffix, ignoreCase = true)) {
            val selectedType = InstrumentType.fromAccountInfo(selectedAccount)
            val expenseType = determineType(expense.accountInfo, expense.rawBody, expense.type)
            if (selectedType == expenseType) return true
        }

        return expense.accountInfo.contains(selectedAccount, ignoreCase = true) ||
                selectedAccount.contains(expense.accountInfo, ignoreCase = true)
    }
}
