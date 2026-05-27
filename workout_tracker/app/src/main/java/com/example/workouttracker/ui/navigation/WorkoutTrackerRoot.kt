package com.example.workouttracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.workouttracker.ui.picker.ExercisePickerRoute
import com.example.workouttracker.ui.today.TodayRoute
import com.example.workouttracker.ui.track.TrackExerciseRoute
import java.time.LocalDate

@Composable
fun WorkoutTrackerRoot() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppDestination.today(LocalDate.now().toString())
    ) {
        composable(
            route = AppDestination.TODAY,
            arguments = listOf(
                navArgument(AppDestination.DATE_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val selectedDate = backStackEntry
                .arguments
                ?.getString(AppDestination.DATE_ARG)
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: LocalDate.now()

            TodayRoute(
                selectedDate = selectedDate,
                onTrackExerciseClick = {
                    navController.navigate(AppDestination.exercisePicker(selectedDate.toString()))
                },
                onExerciseClick = { exerciseId ->
                    navController.navigate(AppDestination.trackExercise(exerciseId, selectedDate.toString()))
                },
                onDateSelected = { date ->
                    navController.navigate(AppDestination.today(date.toString())) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = AppDestination.EXERCISE_PICKER,
            arguments = listOf(
                navArgument(AppDestination.DATE_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val selectedDate = backStackEntry
                .arguments
                ?.getString(AppDestination.DATE_ARG)
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: LocalDate.now()

            ExercisePickerRoute(
                onBack = { navController.popBackStack() },
                onExerciseSelected = { exerciseId ->
                    navController.navigate(AppDestination.trackExercise(exerciseId, selectedDate.toString()))
                }
            )
        }

        composable(
            route = AppDestination.TRACK_EXERCISE,
            arguments = listOf(
                navArgument(AppDestination.EXERCISE_ID_ARG) { type = NavType.LongType },
                navArgument(AppDestination.DATE_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val exerciseId =
                backStackEntry.arguments?.getLong(AppDestination.EXERCISE_ID_ARG) ?: return@composable

            val selectedDate = backStackEntry
                .arguments
                ?.getString(AppDestination.DATE_ARG)
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: LocalDate.now()

            TrackExerciseRoute(
                exerciseId = exerciseId,
                workoutDate = selectedDate,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
