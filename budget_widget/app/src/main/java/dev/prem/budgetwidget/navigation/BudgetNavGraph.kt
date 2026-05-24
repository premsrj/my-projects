package dev.prem.budgetwidget.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.prem.budgetwidget.AppContainer
import dev.prem.budgetwidget.ui.addedit.AddEditExpenseRoute
import dev.prem.budgetwidget.ui.addedit.AddEditExpenseViewModel
import dev.prem.budgetwidget.ui.main.MainScreen
import dev.prem.budgetwidget.ui.main.MainViewModel

@Composable
fun BudgetNavGraph(appContainer: AppContainer) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.HOME_ROUTE
    ) {
        composable(route = Destinations.HOME_ROUTE) {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.factory(
                    repository = appContainer.expenseRepository,
                    budgetPreferences = appContainer.budgetPreferences
                )
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MainScreen(
                uiState = uiState,
                onSetMonthlyLimit = viewModel::saveMonthlyLimit,
                onExpenseClick = { id ->
                    navController.navigate(Destinations.expenseRoute(id))
                },
                onAddExpenseClick = {
                    navController.navigate(Destinations.expenseRoute(null))
                }
            )
        }

        composable(
            route = Destinations.EXPENSE_ROUTE_PATTERN,
            arguments = listOf(
                navArgument(Destinations.EXPENSE_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val viewModel: AddEditExpenseViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = AddEditExpenseViewModel.factory(appContainer.expenseRepository)
            )

            AddEditExpenseRoute(
                viewModel = viewModel,
                onDone = {
                    val navigatedBack = navController.popBackStack()
                    if (!navigatedBack) {
                        navController.navigate(Destinations.HOME_ROUTE) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
