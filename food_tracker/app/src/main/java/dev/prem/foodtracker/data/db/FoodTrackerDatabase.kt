package dev.prem.foodtracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FoodEntity::class,
        TargetProfileEntity::class,
        MealTargetEntity::class,
        MealEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FoodTrackerDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun targetDao(): TargetDao
    abstract fun mealDao(): MealDao
}
