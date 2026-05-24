package dev.prem.budgetwidget.ui.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Formatters {
    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance()
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM")

    fun formatCurrency(amount: Double): String {
        return currencyFormatter.format(amount)
    }

    fun formatShortDate(date: LocalDate): String {
        return date.format(dateFormatter)
    }
}
