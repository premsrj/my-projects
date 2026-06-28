package dev.prem.foodtracker.data.repo

import android.content.Context
import dev.prem.foodtracker.data.db.FoodDao
import dev.prem.foodtracker.data.db.FoodEntity
import kotlin.math.max

class FoodSeeder(
    private val context: Context,
    private val foodDao: FoodDao
) {
    suspend fun seedIfNeeded() {
        if (foodDao.countFoods() > 0) return

        val csvRows = context.assets.open("foods_seed.csv").bufferedReader().use { reader ->
            reader.readLines()
        }

        if (csvRows.size <= 1) return

        val foods = csvRows.drop(1)
            .filter { it.isNotBlank() }
            .mapNotNull(::parseFood)

        if (foods.isNotEmpty()) {
            foodDao.insertFoods(foods)
        }
    }

    private fun parseFood(line: String): FoodEntity? {
        val values = line.split(CSV_SPLIT_REGEX).map { token ->
            token.trim().trim('"')
        }

        if (values.size < 9) return null

        return FoodEntity(
            name = values[0],
            servingSize = parseNonNegativeDouble(values.getOrNull(1)),
            unit = values.getOrNull(2).orEmpty(),
            calories = parseNonNegativeDouble(values.getOrNull(3)),
            protein = parseNonNegativeDouble(values.getOrNull(4)),
            fat = parseNonNegativeDouble(values.getOrNull(5)),
            carbs = parseNonNegativeDouble(values.getOrNull(6)),
            fiber = parseNonNegativeDouble(values.getOrNull(7)),
            comments = values.getOrNull(8).orEmpty(),
            isPreloaded = true
        )
    }

    private fun parseNonNegativeDouble(raw: String?): Double {
        return max(raw?.toDoubleOrNull() ?: 0.0, 0.0)
    }

    companion object {
        private val CSV_SPLIT_REGEX = Regex(""",(?=(?:[^"]*"[^"]*")*[^"]*$)""")
    }
}
