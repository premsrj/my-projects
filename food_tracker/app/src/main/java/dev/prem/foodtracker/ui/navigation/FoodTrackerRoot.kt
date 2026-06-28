package dev.prem.foodtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.prem.foodtracker.FoodTrackerApp
import dev.prem.foodtracker.data.model.FoodImportResult
import dev.prem.foodtracker.data.model.MealType
import dev.prem.foodtracker.ui.screens.FoodEditorRoute
import dev.prem.foodtracker.ui.screens.FoodSearchRoute
import dev.prem.foodtracker.ui.screens.HomeRoute
import dev.prem.foodtracker.ui.screens.ImportResultRoute
import dev.prem.foodtracker.ui.screens.MealsRoute
import dev.prem.foodtracker.ui.screens.TrackFoodRoute
import java.time.LocalDate

@Composable
fun FoodTrackerRoot() {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as FoodTrackerApp).appContainer
    val navController = rememberNavController()
    var selectedHomeDateEpochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var latestImportResult by remember { mutableStateOf<FoodImportResult?>(appContainer.latestImportResult) }

    NavHost(
        navController = navController,
        startDestination = AppDestination.home()
    ) {
        composable(route = AppDestination.HOME) {
            val date = LocalDate.ofEpochDay(selectedHomeDateEpochDay)

            HomeRoute(
                selectedDate = date,
                onDateSelected = { newDate ->
                    selectedHomeDateEpochDay = newDate.toEpochDay()
                },
                onTrackFood = {
                    navController.navigate(AppDestination.meals(date.toEpochDay()))
                },
                onImportCompleted = { result ->
                    latestImportResult = result
                    appContainer.latestImportResult = result
                    navController.navigate(AppDestination.importResult())
                }
            )
        }

        composable(
            route = AppDestination.MEALS,
            arguments = listOf(navArgument(AppDestination.DATE_ARG) { type = NavType.LongType })
        ) { entry ->
            val dateEpochDay = entry.arguments?.getLong(AppDestination.DATE_ARG) ?: LocalDate.now().toEpochDay()
            val date = LocalDate.ofEpochDay(dateEpochDay)

            MealsRoute(
                selectedDate = date,
                onBack = { navController.popBackStack() },
                onMealSelected = { meal ->
                    navController.navigate(AppDestination.foodSearch(date.toEpochDay(), meal.key))
                }
            )
        }

        composable(
            route = AppDestination.FOOD_SEARCH,
            arguments = listOf(
                navArgument(AppDestination.DATE_ARG) { type = NavType.LongType },
                navArgument(AppDestination.MEAL_ARG) { type = NavType.StringType }
            )
        ) { entry ->
            val dateEpochDay = entry.arguments?.getLong(AppDestination.DATE_ARG) ?: LocalDate.now().toEpochDay()
            val date = LocalDate.ofEpochDay(dateEpochDay)
            val meal = MealType.fromKey(entry.arguments?.getString(AppDestination.MEAL_ARG).orEmpty())

            FoodSearchRoute(
                selectedDate = date,
                mealType = meal,
                onBack = { navController.popBackStack() },
                onFoodSelected = { foodId ->
                    navController.navigate(AppDestination.trackFood(date.toEpochDay(), meal.key, foodId))
                },
                onAddFood = {
                    navController.navigate(AppDestination.foodEditor())
                },
                onEditFood = { foodId ->
                    navController.navigate(AppDestination.foodEditor(foodId))
                }
            )
        }

        composable(
            route = AppDestination.TRACK_FOOD,
            arguments = listOf(
                navArgument(AppDestination.DATE_ARG) { type = NavType.LongType },
                navArgument(AppDestination.MEAL_ARG) { type = NavType.StringType },
                navArgument(AppDestination.FOOD_ARG) { type = NavType.LongType }
            )
        ) { entry ->
            val dateEpochDay = entry.arguments?.getLong(AppDestination.DATE_ARG) ?: LocalDate.now().toEpochDay()
            val date = LocalDate.ofEpochDay(dateEpochDay)
            val meal = MealType.fromKey(entry.arguments?.getString(AppDestination.MEAL_ARG).orEmpty())
            val foodId = entry.arguments?.getLong(AppDestination.FOOD_ARG) ?: -1L

            TrackFoodRoute(
                selectedDate = date,
                mealType = meal,
                foodId = foodId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = AppDestination.FOOD_EDITOR,
            arguments = listOf(
                navArgument(AppDestination.EDIT_FOOD_ARG) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { entry ->
            val foodId = entry.arguments?.getLong(AppDestination.EDIT_FOOD_ARG) ?: -1L

            FoodEditorRoute(
                foodId = foodId,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
            )
        }

        composable(route = AppDestination.IMPORT_RESULT) {
            ImportResultRoute(
                result = latestImportResult,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
