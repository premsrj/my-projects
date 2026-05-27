package com.example.workouttracker.ui.navigation

object AppDestination {
    const val DATE_ARG = "date"
    const val TODAY = "today?$DATE_ARG={$DATE_ARG}"
    const val EXERCISE_PICKER = "exercise_picker?$DATE_ARG={$DATE_ARG}"
    const val TRACK_EXERCISE = "track_exercise/{exerciseId}?$DATE_ARG={$DATE_ARG}"
    const val EXERCISE_ID_ARG = "exerciseId"

    fun today(dateIso: String): String = "today?$DATE_ARG=$dateIso"

    fun exercisePicker(dateIso: String): String = "exercise_picker?$DATE_ARG=$dateIso"

    fun trackExercise(exerciseId: Long, dateIso: String): String =
        "track_exercise/$exerciseId?$DATE_ARG=$dateIso"
}
