package dev.prem.foodtracker.data.db

data class NutrientTotalsRow(
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val fiber: Double
)

data class MealTotalsRow(
    val mealType: String,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val fiber: Double
)

data class DailyCaloriesRow(
    val dateEpochDay: Long,
    val calories: Double
)
