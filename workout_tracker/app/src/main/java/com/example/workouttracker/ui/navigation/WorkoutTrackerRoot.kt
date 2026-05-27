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
import java.util.UUID

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
                },
                onSupersetSelected = { orderedExerciseIds ->
                    val firstExerciseId = orderedExerciseIds.firstOrNull() ?: return@ExercisePickerRoute
                    val supersetGroupId = UUID.randomUUID().toString()
                    navController.navigate(
                        AppDestination.trackExercise(
                            exerciseId = firstExerciseId,
                            dateIso = selectedDate.toString(),
                            supersetExerciseIds = orderedExerciseIds,
                            supersetIndex = 0,
                            supersetGroupId = supersetGroupId,
                            supersetRound = 1
                        )
                    )
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
                },
                navArgument(AppDestination.SUPERSET_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(AppDestination.SUPERSET_INDEX_ARG) {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument(AppDestination.SUPERSET_GROUP_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(AppDestination.SUPERSET_ROUND_ARG) {
                    type = NavType.IntType
                    defaultValue = 1
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

            val supersetExerciseIds = backStackEntry
                .arguments
                ?.getString(AppDestination.SUPERSET_ARG)
                .orEmpty()
                .split(",")
                .mapNotNull { value -> value.toLongOrNull() }

            val suppliedSupersetIndex = backStackEntry
                .arguments
                ?.getInt(AppDestination.SUPERSET_INDEX_ARG)
                ?: -1

            val currentSupersetIndex = if (suppliedSupersetIndex >= 0) {
                suppliedSupersetIndex
            } else {
                supersetExerciseIds.indexOf(exerciseId)
            }

            val supersetGroupId = backStackEntry
                .arguments
                ?.getString(AppDestination.SUPERSET_GROUP_ARG)
                .orEmpty()

            val currentSupersetRound = backStackEntry
                .arguments
                ?.getInt(AppDestination.SUPERSET_ROUND_ARG)
                ?: 1

            TrackExerciseRoute(
                exerciseId = exerciseId,
                workoutDate = selectedDate,
                supersetExerciseIds = supersetExerciseIds,
                supersetIndex = currentSupersetIndex,
                supersetGroupId = supersetGroupId,
                supersetRound = currentSupersetRound,
                onSupersetAdvance = {
                    if (supersetExerciseIds.size >= 2 && currentSupersetIndex >= 0) {
                        val nextIndex = (currentSupersetIndex + 1) % supersetExerciseIds.size
                        val nextExerciseId = supersetExerciseIds[nextIndex]
                        val nextRound = if (nextIndex == 0) {
                            currentSupersetRound + 1
                        } else {
                            currentSupersetRound
                        }

                        navController.navigate(
                            AppDestination.trackExercise(
                                exerciseId = nextExerciseId,
                                dateIso = selectedDate.toString(),
                                supersetExerciseIds = supersetExerciseIds,
                                supersetIndex = nextIndex,
                                supersetGroupId = supersetGroupId,
                                supersetRound = nextRound
                            )
                        ) {
                            popUpTo(backStackEntry.destination.id) {
                                inclusive = true
                            }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
