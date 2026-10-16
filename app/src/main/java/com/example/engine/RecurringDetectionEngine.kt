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
            !it.category.equals("Credit Card Bill", ignoreCase = true) &&
            !it.category.contains("Credit Card", ignoreCase = true) &&
            !it.accountInfo.contains("Credit Card", ignoreCase = true) &&
            !it.merchantOrRecipient.contains("Credit Card", ignoreCase = true)
        }
        if (validExpenses.isEmpty()) return emptyList()

        val cal = Calendar.getInstance()

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
            val hasManualRecurring = sorted.any { it.isRecurring }

            // Strictly only track manually tagged transactions as recurring
            if (hasManualRecurring) {
                val recurringExpenses = sorted.filter { it.isRecurring }
                val latest = recurringExpenses.firstOrNull() ?: sorted.first()
                val merchantName = latest.merchantOrRecipient.trim()
                val currency = latest.currency
                val category = latest.category

                val targetList = if (recurringExpenses.isNotEmpty()) recurringExpenses else sorted
                val days = ArrayList<Int>(targetList.size)
                for (exp in targetList) {
                    cal.timeInMillis = exp.timestamp
                    days.add(cal.get(Calendar.DAY_OF_MONTH))
                }
                days.sort()
                val typicalDay = days[days.size / 2]

                val paidThisMonth = sorted.any { it.monthKey == currentMonthKey }
                val thisMonthPaidExp = sorted.firstOrNull { it.monthKey == currentMonthKey }

                result.add(
                    PredictedRecurringBill(
                        merchant = merchantName,
                        expectedAmount = latest.amount,
                        currency = currency,
                        category = category,
                        typicalDayOfMonth = typicalDay,
                        isPaidThisMonth = paidThisMonth,
                        lastPaidTimestamp = thisMonthPaidExp?.timestamp ?: latest.timestamp,
                        isManuallyMarked = true
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
