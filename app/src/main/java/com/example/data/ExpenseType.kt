package com.example.data

enum class ExpenseType(val displayName: String) {
    SPEND("Spend"),
    TRANSFER("Transfer");

    companion object {
        fun fromString(value: String): ExpenseType {
            return when (value.uppercase()) {
                "TRANSFER" -> TRANSFER
                else -> SPEND
            }
        }
    }
}
