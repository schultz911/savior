package com.example.engine

import com.example.data.ExpenseEntity
import com.example.data.ExpenseType
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class PredictedRecurringBill(
    val id: String = "",
    val merchant: String,
    val expectedAmount: Double,
    val currency: String,
    val category: String,
    val typicalDayOfMonth: Int,
    val isPaidThisMonth: Boolean,
    val lastPaidTimestamp: Long,
    val isManuallyMarked: Boolean = true,
    val expenseId: Long = 0L
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

        // Strictly only track manually tagged transactions as recurring
        val recurringCandidates = validExpenses.filter { exp ->
            exp.isRecurring && normalizedIgnored.none {
                val norm = exp.merchantOrRecipient.trim().lowercase(Locale.US)
                norm.contains(it) || it.contains(norm)
            }
        }
        if (recurringCandidates.isEmpty()) return emptyList()

        val result = mutableListOf<PredictedRecurringBill>()

        // 1. Current month bills: show each individual transaction marked as a bill
        val currentMonthBills = recurringCandidates.filter { it.monthKey == currentMonthKey }
            .sortedByDescending { it.timestamp }

        for (exp in currentMonthBills) {
            cal.timeInMillis = exp.timestamp
            val day = cal.get(Calendar.DAY_OF_MONTH)
            result.add(
                PredictedRecurringBill(
                    id = "settled_${exp.id}",
                    merchant = exp.merchantOrRecipient.trim(),
                    expectedAmount = exp.netAmount,
                    currency = exp.currency,
                    category = exp.category,
                    typicalDayOfMonth = day,
                    isPaidThisMonth = true,
                    lastPaidTimestamp = exp.timestamp,
                    isManuallyMarked = true,
                    expenseId = exp.id
                )
            )
        }

        // 2. Past recurring transactions: show unpaid recurring bills individually in upcoming
        val pastBills = recurringCandidates.filter { it.monthKey != currentMonthKey }
        if (pastBills.isNotEmpty()) {
            val pastByMerchant = pastBills.groupBy { it.merchantOrRecipient.trim().lowercase(Locale.US) }
            val currentByMerchant = currentMonthBills.groupBy { it.merchantOrRecipient.trim().lowercase(Locale.US) }

            for ((merchantKey, pastMerchantBills) in pastByMerchant) {
                val currentCount = currentByMerchant[merchantKey]?.size ?: 0

                // Reference past month: latest month before currentMonthKey where this merchant had bills
                val latestPastMonth = pastMerchantBills.map { it.monthKey }.maxOrNull() ?: continue
                val referenceBills = pastMerchantBills.filter { it.monthKey == latestPastMonth }
                    .sortedByDescending { it.timestamp }

                // If current month has fewer settled payments than the reference month, the remaining are upcoming
                if (currentCount < referenceBills.size) {
                    val remainingUnpaid = referenceBills.drop(currentCount)
                    for (pastExp in remainingUnpaid) {
                        cal.timeInMillis = pastExp.timestamp
                        val day = cal.get(Calendar.DAY_OF_MONTH)
                        result.add(
                            PredictedRecurringBill(
                                id = "upcoming_${pastExp.id}",
                                merchant = pastExp.merchantOrRecipient.trim(),
                                expectedAmount = pastExp.netAmount,
                                currency = pastExp.currency,
                                category = pastExp.category,
                                typicalDayOfMonth = day,
                                isPaidThisMonth = false,
                                lastPaidTimestamp = pastExp.timestamp,
                                isManuallyMarked = true,
                                expenseId = pastExp.id
                            )
                        )
                    }
                }
            }
        }

        return result.sortedWith(
            compareBy<PredictedRecurringBill> { it.isPaidThisMonth }
                .thenBy { it.typicalDayOfMonth }
        )
    }
}
