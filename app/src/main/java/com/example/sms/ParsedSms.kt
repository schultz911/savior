package com.example.sms

import com.example.data.ExpenseType

data class ParsedSms(
    val amount: Double,
    val currency: String,
    val type: ExpenseType,
    val title: String,
    val accountInfo: String,
    val category: String,
    val isExpense: Boolean,
    val rawText: String,
    val isRefund: Boolean = false
)
