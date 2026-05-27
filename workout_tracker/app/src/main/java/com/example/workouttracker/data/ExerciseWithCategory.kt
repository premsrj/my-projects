package com.example.workouttracker.data

data class ExerciseWithCategory(
    val id: Long,
    val name: String,
    val description: String,
    val type: ExerciseType,
    val categoryId: Long,
    val weightIncrement: Double,
    val categoryName: String
)
