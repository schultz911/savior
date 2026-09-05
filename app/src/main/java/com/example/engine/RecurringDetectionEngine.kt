package com.example.engine

import com.example.data.ExpenseEntity
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
        currentMonthKey: String = ExpenseEntity.formatMonthKey(System.currentTimeMillis())
    ): List<PredictedRecurringBill> {
        if (allExpenses.isEmpty()) return emptyList()

        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)

        // Group by normalized merchant
        val grouped = allExpenses.groupBy { it.merchantOrRecipient.trim().lowercase(Locale.US) }
            .filterKeys { it.isNotBlank() && it != "unknown" && it != "merchant / payee" }

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

            // Check amount consistency across occurrences
            val avgAmount = sorted.map { it.amount }.average()
            val isConsistentAmount = sorted.all { abs(it.amount - avgAmount) / avgAmount <= 0.20 }

            val isRecurring = hasManualRecurring || isKnownKeyword || (hasMultiMonthCadence && isConsistentAmount)

            if (isRecurring) {
                // Find median day of month
                val days = sorted.map {
                    cal.timeInMillis = it.timestamp
                    cal.get(Calendar.DAY_OF_MONTH)
                }
                val typicalDay = days.sorted().let { it[it.size / 2] }

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

    fun getUpcomingCommitmentsTotal(
        allExpenses: List<ExpenseEntity>,
        currentMonthKey: String = ExpenseEntity.formatMonthKey(System.currentTimeMillis())
    ): Double {
        val bills = detectRecurringBills(allExpenses, currentMonthKey)
        return bills.filter { !it.isPaidThisMonth }.sumOf { it.expectedAmount }
    }
}
