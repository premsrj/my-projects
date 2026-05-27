package com.example.workouttracker.data

enum class ExerciseType(
    val label: String,
    val usesWeight: Boolean,
    val usesReps: Boolean,
    val usesTime: Boolean,
    val usesDistance: Boolean
) {
    WEIGHT_REPS("Weight and reps", usesWeight = true, usesReps = true, usesTime = false, usesDistance = false),
    REPS_ONLY("Reps only", usesWeight = false, usesReps = true, usesTime = false, usesDistance = false),
    WEIGHT_TIME("Weight and time", usesWeight = true, usesReps = false, usesTime = true, usesDistance = false),
    TIME_ONLY("Time only", usesWeight = false, usesReps = false, usesTime = true, usesDistance = false),
    DISTANCE_TIME("Distance and time", usesWeight = false, usesReps = false, usesTime = true, usesDistance = true),
    DISTANCE_ONLY("Distance only", usesWeight = false, usesReps = false, usesTime = false, usesDistance = true)
}
