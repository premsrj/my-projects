package com.example.workouttracker.data

import androidx.room.Embedded

data class WorkoutSetWithExercise(
    @Embedded val set: WorkoutSetEntity,
    val exerciseName: String,
    val exerciseType: ExerciseType,
    val categoryName: String
)
