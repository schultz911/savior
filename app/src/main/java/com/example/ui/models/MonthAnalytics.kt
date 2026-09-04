package com.example.ui.models

data class MonthAnalytics(
    val monthKey: String,
    val monthLabel: String,
    val shortLabel: String,
    val year: Int,
    val monthNumber: Int,
    val totalSpent: Double,
    val salary: Double,
    val savedAmount: Double,
    val isOverspent: Boolean,
    val savingsRate: Double
)
