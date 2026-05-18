package com.teju.expensetracker

data class Expense(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val amount: Double,
    val category: String,
    val date: String,
    val day: String,
    val month: String,
    val year: String
)