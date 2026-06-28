package dev.prem.foodtracker.data.db

import androidx.room.Entity

@Entity(
    tableName = "meal_targets",
    primaryKeys = ["dateEpochDay", "mealType"]
)
data class MealTargetEntity(
    val dateEpochDay: Long,
    val mealType: String,
    val calorieTarget: Double?
)
