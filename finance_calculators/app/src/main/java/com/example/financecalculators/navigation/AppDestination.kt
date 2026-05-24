package com.example.financecalculators.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : AppDestination("home", "Home", Icons.Filled.Home)
    data object Swp : AppDestination("swp", "SWP", Icons.Filled.AccountBalanceWallet)
    data object Inflation : AppDestination("inflation", "Inflation", Icons.AutoMirrored.Filled.ShowChart)
    data object Investment : AppDestination("investment", "Investment", Icons.Filled.PieChart)

    companion object {
        val bottomNavDestinations = listOf(Home, Swp, Inflation, Investment)

        fun fromRoute(route: String?): AppDestination {
            return bottomNavDestinations.firstOrNull { it.route == route } ?: Home
        }
    }
}
