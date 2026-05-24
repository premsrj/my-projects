package dev.prem.budgetwidget

import android.app.Application
import android.content.Context
import dev.prem.budgetwidget.data.local.AppDatabase
import dev.prem.budgetwidget.data.preferences.BudgetPreferences
import dev.prem.budgetwidget.data.repository.ExpenseRepository

class BudgetTrackerApp : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(applicationContext)
    }
}

class AppContainer(context: Context) {
    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepository(
            expenseDao = database.expenseDao(),
            appContext = context.applicationContext
        )
    }

    val budgetPreferences: BudgetPreferences by lazy {
        BudgetPreferences(context)
    }
}
