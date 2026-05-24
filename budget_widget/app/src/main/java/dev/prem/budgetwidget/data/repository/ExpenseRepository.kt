package dev.prem.budgetwidget.data.repository

import android.content.Context
import dev.prem.budgetwidget.data.local.ExpenseDao
import dev.prem.budgetwidget.data.local.ExpenseEntity
import dev.prem.budgetwidget.wear.BudgetDailyTileService
import dev.prem.budgetwidget.widget.BudgetDailyWidgetProvider
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val appContext: Context
) {
    fun observeCurrentMonthExpenses(today: LocalDate = LocalDate.now()): Flow<List<ExpenseEntity>> {
        val start = today.withDayOfMonth(1).toEpochDay()
        val end = today.withDayOfMonth(today.lengthOfMonth()).toEpochDay()
        return expenseDao.observeExpensesInRange(start, end)
    }

    suspend fun getExpense(id: Long): ExpenseEntity? {
        return expenseDao.getExpenseById(id)
    }

    suspend fun addExpense(date: LocalDate, amount: Double, description: String) {
        expenseDao.insert(
            ExpenseEntity(
                dateEpochDay = date.toEpochDay(),
                amount = amount,
                description = description.trim()
            )
        )
        BudgetDailyWidgetProvider.updateAllWidgets(appContext)
        BudgetDailyTileService.requestUpdate(appContext)
    }

    suspend fun updateExpense(id: Long, date: LocalDate, amount: Double, description: String) {
        expenseDao.update(
            ExpenseEntity(
                id = id,
                dateEpochDay = date.toEpochDay(),
                amount = amount,
                description = description.trim()
            )
        )
        BudgetDailyWidgetProvider.updateAllWidgets(appContext)
        BudgetDailyTileService.requestUpdate(appContext)
    }

    suspend fun deleteExpense(id: Long) {
        val expense = expenseDao.getExpenseById(id) ?: return
        expenseDao.delete(expense)
        BudgetDailyWidgetProvider.updateAllWidgets(appContext)
        BudgetDailyTileService.requestUpdate(appContext)
    }

    suspend fun getSpentOnDay(day: LocalDate): Double {
        val epochDay = day.toEpochDay()
        return expenseDao.sumAmountsInRange(epochDay, epochDay)
    }

    suspend fun getMonthSpentBeforeDay(day: LocalDate): Double {
        val startOfMonth = day.withDayOfMonth(1).toEpochDay()
        val dayBefore = day.toEpochDay() - 1
        if (dayBefore < startOfMonth) return 0.0
        return expenseDao.sumAmountsInRange(startOfMonth, dayBefore)
    }
}
