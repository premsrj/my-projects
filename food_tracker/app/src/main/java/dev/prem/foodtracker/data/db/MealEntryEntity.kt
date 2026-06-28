package dev.prem.foodtracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_entries")
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val mealType: String,
    val foodId: Long,
    val foodName: String,
    val quantity: Double,
    val unit: String,
    val consumedCalories: Double,
    val consumedProtein: Double,
    val consumedFat: Double,
    val consumedCarbs: Double,
    val consumedFiber: Double
)
