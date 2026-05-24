package dev.prem.budgetwidget.navigation

object Destinations {
    const val HOME_ROUTE = "home"
    const val EXPENSE_ROUTE = "expense"
    const val EXPENSE_ID_ARG = "expenseId"
    const val EXPENSE_ROUTE_PATTERN = "$EXPENSE_ROUTE?$EXPENSE_ID_ARG={$EXPENSE_ID_ARG}"

    fun expenseRoute(expenseId: Long?): String {
        return if (expenseId == null) {
            EXPENSE_ROUTE
        } else {
            "$EXPENSE_ROUTE?$EXPENSE_ID_ARG=$expenseId"
        }
    }
}
