package dev.prem.foodtracker

import android.content.Context
import androidx.room.Room
import dev.prem.foodtracker.data.db.FoodTrackerDatabase
import dev.prem.foodtracker.data.model.FoodImportResult
import dev.prem.foodtracker.data.repo.FoodSeeder
import dev.prem.foodtracker.data.repo.FoodTrackerRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: FoodTrackerDatabase = Room.databaseBuilder(
        appContext,
        FoodTrackerDatabase::class.java,
        "food_tracker.db"
    ).build()

    val repository: FoodTrackerRepository = FoodTrackerRepository(
        foodDao = database.foodDao(),
        mealDao = database.mealDao(),
        targetDao = database.targetDao()
    )

    val foodSeeder: FoodSeeder = FoodSeeder(
        context = appContext,
        foodDao = database.foodDao()
    )

    var latestImportResult: FoodImportResult? = null
}
