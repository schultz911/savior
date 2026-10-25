package com.example

import com.example.data.ExpenseEntity
import com.example.data.ExpenseType
import com.example.engine.RecurringDetectionEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringInputReadinessTest {
    @Test fun recurringOnlyInputPreservesSixMonthAndOldManualBillResults() {
        val now = 1792886400000L
        val history = (0 until 10000).map { i ->
            val timestamp = now - i * 3600000L
            ExpenseEntity(id = i.toLong() + 1, amount = (i % 1000 + 1).toDouble(),
                merchantOrRecipient = "Merchant " + (i % 29), timestamp = timestamp,
                monthKey = ExpenseEntity.formatMonthKey(timestamp), isRecurring = i % 20 == 0,
                isExcluded = i % 17 == 0, isReversal = i % 31 == 0,
                type = if (i % 37 == 0) ExpenseType.CREDIT_CARD else ExpenseType.MERCHANT)
        }
        val manual = history.filter { it.isRecurring }
        val priorInput = (history.filter { it.timestamp >= now - 180L * 86400000 } + manual).distinctBy { it.id }
        for (month in listOf("2026-10", "2026-09", "2026-01")) {
            for (ignored in listOf(emptySet(), setOf("Merchant 1"))) {
                assertEquals(RecurringDetectionEngine.detectRecurringBills(priorInput, month, ignored),
                    RecurringDetectionEngine.detectRecurringBills(manual, month, ignored))
            }
        }
    }
}
