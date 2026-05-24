package com.example.financecalculators

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.financecalculators.calculators.HomeScreen
import com.example.financecalculators.calculators.InflationCalculatorScreen
import com.example.financecalculators.calculators.InvestmentCalculatorScreen
import com.example.financecalculators.calculators.SwpCalculatorScreen
import com.example.financecalculators.navigation.AppDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentDestination = AppDestination.fromRoute(currentRoute)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text(currentDestination.title) })
        },
        bottomBar = {
            NavigationBar {
                AppDestination.bottomNavDestinations.forEach { destination ->
                    val selected = destination.route == currentRoute
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title
                            )
                        },
                        label = { Text(destination.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.Home.route) {
                HomeScreen(
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(AppDestination.Swp.route) { SwpCalculatorScreen() }
            composable(AppDestination.Inflation.route) { InflationCalculatorScreen() }
            composable(AppDestination.Investment.route) { InvestmentCalculatorScreen() }
        }
    }
}
