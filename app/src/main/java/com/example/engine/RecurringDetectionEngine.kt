package com.example.engine

import com.example.data.ExpenseEntity
import com.example.data.ExpenseType
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class PredictedRecurringBill(
    val merchant: String,
    val expectedAmount: Double,
    val currency: String,
    val category: String,
    val typicalDayOfMonth: Int,
    val isPaidThisMonth: Boolean,
    val lastPaidTimestamp: Long,
    val isManuallyMarked: Boolean = false
)

object RecurringDetectionEngine {

    private val KNOWN_SUBSCRIPTION_KEYWORDS = listOf(
        "netflix", "spotify", "prime", "hotstar", "youtube", "apple", "google",
        "bescom", "tata power", "electricity", "water", "bill", "broadband",
        "airtel", "jio", "vi", "vodafone", "sip", "zerodha", "groww", "mutual fund",
        "emi", "loan", "rent", "gym", "cult", "insurance", "lic", "hdfc life", "icici pru"
    )

    fun detectRecurringBills(
        allExpenses: List<ExpenseEntity>,
        currentMonthKey: String = ExpenseEntity.formatMonthKey(System.currentTimeMillis()),
        ignoredMerchants: Set<String> = emptySet()
    ): List<PredictedRecurringBill> {
        if (allExpenses.isEmpty()) return emptyList()

        val validExpenses = allExpenses.filter {
            !it.isExcluded &&
            !it.isRefundOrReversal &&
            it.type != ExpenseType.SELF &&
            !it.category.equals("Self", ignoreCase = true) &&
            it.type != ExpenseType.CREDIT_CARD &&
            !it.category.equals("Credit Card Bill", ignoreCase = true)
        }
        if (validExpenses.isEmpty()) return emptyList()

        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)

        val normalizedIgnored = ignoredMerchants.map { it.trim().lowercase(Locale.US) }

        // Group by normalized merchant, excluding ignored recurring merchants
        val grouped = validExpenses.groupBy { it.merchantOrRecipient.trim().lowercase(Locale.US) }
            .filterKeys { key ->
                key.isNotBlank() && key != "unknown" && key != "merchant / payee" &&
                    normalizedIgnored.none { key.contains(it) || it.contains(key) }
            }

        val result = mutableListOf<PredictedRecurringBill>()

        for ((_, merchantExpenses) in grouped) {
            val sorted = merchantExpenses.sortedByDescending { it.timestamp }
            val latest = sorted.first()
            val merchantName = latest.merchantOrRecipient.trim()
            val currency = latest.currency
            val category = latest.category

            val hasManualRecurring = sorted.any { it.isRecurring }
            val isKnownKeyword = KNOWN_SUBSCRIPTION_KEYWORDS.any {
                merchantName.lowercase(Locale.US).contains(it)
            }

            // Check cadence: 2+ occurrences across distinct months
            val distinctMonths = sorted.map { it.monthKey }.distinct()
            val hasMultiMonthCadence = distinctMonths.size >= 2

            // Single-pass accumulation — zero intermediate list allocations
            var amountSum = 0.0
            for (exp in sorted) { amountSum += exp.amount }
            val avgAmount = if (sorted.isNotEmpty()) amountSum / sorted.size else 0.0
            val isConsistentAmount = avgAmount > 0.0 && sorted.all { abs(it.amount - avgAmount) / avgAmount <= 0.20 }

            val isRecurring = hasManualRecurring || isKnownKeyword || (hasMultiMonthCadence && isConsistentAmount)

            if (isRecurring) {
                // Find median day of month — direct accumulation without intermediate list
                val days = ArrayList<Int>(sorted.size)
                for (exp in sorted) {
                    cal.timeInMillis = exp.timestamp
                    days.add(cal.get(Calendar.DAY_OF_MONTH))
                }
                days.sort()
                val typicalDay = days[days.size / 2]

                val paidThisMonth = sorted.any { it.monthKey == currentMonthKey }

                result.add(
                    PredictedRecurringBill(
                        merchant = merchantName,
                        expectedAmount = latest.amount,
                        currency = currency,
                        category = category,
                        typicalDayOfMonth = typicalDay,
                        isPaidThisMonth = paidThisMonth,
                        lastPaidTimestamp = latest.timestamp,
                        isManuallyMarked = hasManualRecurring
                    )
                )
            }
        }

        return result.sortedWith(
            compareBy<PredictedRecurringBill> { it.isPaidThisMonth }
                .thenBy { it.typicalDayOfMonth }
        )
    }
}
