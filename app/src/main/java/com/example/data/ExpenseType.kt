package com.example.data

enum class ExpenseType(val displayName: String) {
    DEBIT("Debit"),
    TRANSFER("Transfer"),
    SPEND("Spend");

    companion object {
        fun fromString(value: String): ExpenseType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: SPEND
        }
    }
}
