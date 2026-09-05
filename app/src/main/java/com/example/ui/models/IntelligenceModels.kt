package com.example.ui.models

enum class InstrumentType(val displayName: String) {
    CARD("Card"),
    BANK_ACCOUNT("Bank A/c"),
    UPI("UPI"),
    OTHER("Other");

    companion object {
        fun fromAccountInfo(accountInfo: String): InstrumentType {
            val lower = accountInfo.lowercase(java.util.Locale.US)
            return when {
                lower.contains("card") || lower.contains("credit") -> CARD
                lower.contains("upi") -> UPI
                lower.contains("a/c") || lower.contains("acct") || lower.contains("bank") -> BANK_ACCOUNT
                else -> OTHER
            }
        }
    }
}

data class InstrumentSpendSummary(
    val accountInfo: String,
    val instrumentType: InstrumentType,
    val totalSpent: Double,
    val transactionCount: Int,
    val percentageOfTotal: Double
)

data class DaySpendPoint(
    val dayOfMonth: Int,
    val daySpent: Double,
    val cumulativeSpend: Double,
    val targetPacingSpend: Double
)

data class DailyBurnDownData(
    val points: List<DaySpendPoint> = emptyList(),
    val currentDay: Int = 1,
    val daysInMonth: Int = 30,
    val currentCumulativeSpend: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val projectedMonthEndSpend: Double = 0.0,
    val isOverPaced: Boolean = false,
    val currentBurnRatePerDay: Double = 0.0
)
