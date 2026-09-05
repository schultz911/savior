package com.example.data

import java.util.Locale

enum class ExpenseType(val displayName: String) {
    MERCHANT("Merchants"),
    P2P("P2P"),
    SELF("Self"),
    CREDIT_CARD("Credit Cards");

    companion object {
        fun fromString(value: String): ExpenseType {
            val normalized = value.trim().uppercase(Locale.US).replace(" ", "_").replace("-", "_")
            return when (normalized) {
                "P2P", "TRANSFER", "TRANSFERS" -> P2P
                "SELF", "SELF_TRANSFER" -> SELF
                "CREDIT_CARD", "CREDIT_CARDS", "CREDITCARD", "CREDIT_CARD_BILL", "CREDIT_BILL" -> CREDIT_CARD
                "MERCHANT", "MERCHANTS", "SPEND", "SPENDS" -> MERCHANT
                else -> MERCHANT
            }
        }
    }
}
