package dev.prem.budgetwidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.prem.budgetwidget.navigation.BudgetNavGraph
import dev.prem.budgetwidget.ui.theme.BudgetWidgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as BudgetTrackerApp).appContainer

        setContent {
            BudgetWidgetTheme {
                BudgetNavGraph(appContainer = appContainer)
            }
        }
    }
}
