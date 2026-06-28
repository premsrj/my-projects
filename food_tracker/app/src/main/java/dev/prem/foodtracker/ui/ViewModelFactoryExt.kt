package dev.prem.foodtracker.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import dev.prem.foodtracker.AppContainer
import dev.prem.foodtracker.FoodTrackerApp

fun CreationExtras.appContainer(): AppContainer {
    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FoodTrackerApp
    return app.appContainer
}
