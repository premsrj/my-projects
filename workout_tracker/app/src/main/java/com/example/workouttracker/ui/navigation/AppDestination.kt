package com.example.workouttracker.ui.navigation

object AppDestination {
    const val DATE_ARG = "date"
    const val SUPERSET_ARG = "superset"
    const val SUPERSET_INDEX_ARG = "supersetIndex"
    const val TODAY = "today?$DATE_ARG={$DATE_ARG}"
    const val EXERCISE_PICKER = "exercise_picker?$DATE_ARG={$DATE_ARG}"
    const val TRACK_EXERCISE =
        "track_exercise/{exerciseId}?$DATE_ARG={$DATE_ARG}&$SUPERSET_ARG={$SUPERSET_ARG}&$SUPERSET_INDEX_ARG={$SUPERSET_INDEX_ARG}"
    const val EXERCISE_ID_ARG = "exerciseId"

    fun today(dateIso: String): String = "today?$DATE_ARG=$dateIso"

    fun exercisePicker(dateIso: String): String = "exercise_picker?$DATE_ARG=$dateIso"

    fun trackExercise(
        exerciseId: Long,
        dateIso: String,
        supersetExerciseIds: List<Long> = emptyList(),
        supersetIndex: Int = -1
    ): String {
        val supersetValue = if (supersetExerciseIds.isEmpty()) {
            ""
        } else {
            supersetExerciseIds.joinToString(",")
        }

        return "track_exercise/$exerciseId?$DATE_ARG=$dateIso&$SUPERSET_ARG=$supersetValue&$SUPERSET_INDEX_ARG=$supersetIndex"
    }
}
