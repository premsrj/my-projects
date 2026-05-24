package dev.prem.budgetwidget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.prem.budgetwidget.navigation.BudgetNavGraph
import dev.prem.budgetwidget.navigation.Destinations
import dev.prem.budgetwidget.ui.theme.BudgetWidgetTheme

class MainActivity : ComponentActivity() {
    private var launchRoute by mutableStateOf(Destinations.HOME_ROUTE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        launchRoute = intent.toLaunchRoute()

        val appContainer = (application as BudgetTrackerApp).appContainer

        setContent {
            BudgetWidgetTheme {
                BudgetNavGraph(
                    appContainer = appContainer,
                    startDestination = launchRoute
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchRoute = intent.toLaunchRoute()
    }

    private fun Intent?.toLaunchRoute(): String {
        val isAddExpenseShortcutIntent = this?.dataString == SHORTCUT_ADD_EXPENSE_URI ||
            this?.action == ACTION_ADD_EXPENSE_SHORTCUT

        return if (isAddExpenseShortcutIntent) {
            Destinations.EXPENSE_ROUTE
        } else {
            Destinations.HOME_ROUTE
        }
    }

    companion object {
        const val ACTION_ADD_EXPENSE_SHORTCUT = "dev.prem.budgetwidget.action.ADD_EXPENSE_SHORTCUT"
        const val SHORTCUT_ADD_EXPENSE_URI = "budgetwidget://shortcut/add-expense"
    }
}
