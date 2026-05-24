package dev.prem.budgetwidget.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.prem.budgetwidget.data.preferences.BudgetPreferences
import dev.prem.budgetwidget.data.repository.ExpenseRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ExpenseRowUi(
    val id: Long,
    val date: LocalDate,
    val amount: Double,
    val description: String
)

data class DashboardUiState(
    val todayLeftToSpend: Double = 0.0,
    val todayLimit: Double = 0.0,
    val todaySpent: Double = 0.0,
    val monthLimit: Double = 0.0,
    val monthLeft: Double = 0.0,
    val monthSpent: Double = 0.0,
    val daysLeftInMonth: Int = 1,
    val expenses: List<ExpenseRowUi> = emptyList()
)

class MainViewModel(
    repository: ExpenseRepository,
    private val budgetPreferences: BudgetPreferences
) : ViewModel() {
    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeCurrentMonthExpenses(),
        budgetPreferences.monthlyLimit
    ) { expenses, monthlyLimit ->
        val today = LocalDate.now()
        val todayEpochDay = today.toEpochDay()
        val daysLeft = (today.lengthOfMonth() - today.dayOfMonth + 1).coerceAtLeast(1)

        val todaySpent = expenses
            .filter { it.dateEpochDay == todayEpochDay }
            .sumOf { it.amount }

        val monthSpentBeforeToday = expenses
            .filter { it.dateEpochDay < todayEpochDay }
            .sumOf { it.amount }

        val todayLimit = budgetPreferences.getOrCreateDailyLimit(
            todayEpochDay = todayEpochDay,
            monthlyLimit = monthlyLimit,
            monthSpentBeforeToday = monthSpentBeforeToday,
            daysLeftInMonth = daysLeft
        )

        val todayLeftToSpend = todayLimit - todaySpent
        val monthSpent = expenses.sumOf { it.amount }
        val monthLeft = monthlyLimit - monthSpent

        DashboardUiState(
            todayLeftToSpend = todayLeftToSpend,
            todayLimit = todayLimit,
            todaySpent = todaySpent,
            monthLimit = monthlyLimit,
            monthLeft = monthLeft,
            monthSpent = monthSpent,
            daysLeftInMonth = daysLeft,
            expenses = expenses.map {
                ExpenseRowUi(
                    id = it.id,
                    date = LocalDate.ofEpochDay(it.dateEpochDay),
                    amount = it.amount,
                    description = it.description
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun saveMonthlyLimit(rawValue: String): Boolean {
        val parsed = rawValue.trim().toDoubleOrNull() ?: return false
        if (parsed < 0.0) return false

        budgetPreferences.setMonthlyLimit(parsed)
        return true
    }

    companion object {
        fun factory(
            repository: ExpenseRepository,
            budgetPreferences: BudgetPreferences
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                        return MainViewModel(repository, budgetPreferences) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
                }
            }
        }
    }
}
