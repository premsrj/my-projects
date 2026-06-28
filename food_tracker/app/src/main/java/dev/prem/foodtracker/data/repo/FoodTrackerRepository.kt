package dev.prem.foodtracker.data.repo

import dev.prem.foodtracker.data.db.FoodDao
import dev.prem.foodtracker.data.db.FoodEntity
import dev.prem.foodtracker.data.db.MealDao
import dev.prem.foodtracker.data.db.MealEntryEntity
import dev.prem.foodtracker.data.db.MealTargetEntity
import dev.prem.foodtracker.data.db.TargetDao
import dev.prem.foodtracker.data.db.TargetProfileEntity
import dev.prem.foodtracker.data.model.FoodImportResult
import dev.prem.foodtracker.data.model.MealType
import dev.prem.foodtracker.data.model.NutritionTotals
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

class FoodTrackerRepository(
    private val foodDao: FoodDao,
    private val mealDao: MealDao,
    private val targetDao: TargetDao
) {
    fun observeFoods(): Flow<List<FoodEntity>> = foodDao.observeFoods()

    fun observeFood(foodId: Long): Flow<FoodEntity?> = foodDao.observeFood(foodId)

    suspend fun getFood(foodId: Long): FoodEntity? = foodDao.getFood(foodId)

    suspend fun searchFoods(query: String): List<FoodEntity> {
        val allFoods = foodDao.getAllFoods()
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) return allFoods.sortedBy { it.name.lowercase() }

        val tokens = normalizedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }

        return allFoods
            .mapNotNull { food ->
                val text = (food.name + " " + food.comments).lowercase()
                val allTokensMatch = tokens.all { token -> text.contains(token) }
                if (!allTokensMatch) return@mapNotNull null

                var score = 0
                if (food.name.lowercase() == normalizedQuery) score += 2000
                if (food.name.lowercase().contains(normalizedQuery)) score += 1000

                tokens.forEach { token ->
                    score += when {
                        food.name.lowercase().startsWith(token) -> 60
                        food.name.lowercase().contains(token) -> 30
                        text.contains(token) -> 10
                        else -> 0
                    }
                }

                food to score
            }
            .sortedWith(compareByDescending<Pair<FoodEntity, Int>> { it.second }.thenBy { it.first.name.lowercase() })
            .map { it.first }
    }

    suspend fun saveFood(food: FoodEntity) {
        val normalized = food.copy(
            name = food.name.trim(),
            unit = food.unit.trim(),
            comments = food.comments.trim(),
            servingSize = max(food.servingSize, 0.0),
            calories = max(food.calories, 0.0),
            protein = max(food.protein, 0.0),
            fat = max(food.fat, 0.0),
            carbs = max(food.carbs, 0.0),
            fiber = max(food.fiber, 0.0)
        )

        if (normalized.id == 0L) {
            foodDao.insertFood(normalized)
        } else {
            foodDao.updateFood(normalized)
        }
    }

    fun observeTargetProfile(): Flow<TargetProfileEntity> {
        return targetDao.observeTargetProfile().map { it ?: TargetProfileEntity() }
    }

    suspend fun saveTargetProfile(profile: TargetProfileEntity) {
        targetDao.upsertTargetProfile(profile)
    }

    fun observeEntriesForDate(dateEpochDay: Long): Flow<List<MealEntryEntity>> {
        return mealDao.observeEntriesForDate(dateEpochDay)
    }

    fun observeEntriesForMeal(dateEpochDay: Long, mealType: MealType): Flow<List<MealEntryEntity>> {
        return mealDao.observeEntriesForMeal(dateEpochDay, mealType.key)
    }

    fun observeMealTargets(dateEpochDay: Long): Flow<List<MealTargetEntity>> {
        return mealDao.observeMealTargets(dateEpochDay)
    }

    fun observeMealTarget(dateEpochDay: Long, mealType: MealType): Flow<MealTargetEntity?> {
        return mealDao.observeMealTarget(dateEpochDay, mealType.key)
    }

    fun observeDailyTotals(dateEpochDay: Long): Flow<NutritionTotals> {
        return mealDao.observeDailyTotals(dateEpochDay).map { row ->
            NutritionTotals(
                calories = row.calories,
                protein = row.protein,
                fat = row.fat,
                carbs = row.carbs,
                fiber = row.fiber
            )
        }
    }

    fun observeDailyCaloriesRange(startEpochDay: Long, endEpochDay: Long): Flow<Map<Long, Double>> {
        return mealDao.observeDailyCaloriesRange(startEpochDay, endEpochDay).map { rows ->
            rows.associate { row -> row.dateEpochDay to row.calories }
        }
    }

    fun observeMealTotals(dateEpochDay: Long): Flow<Map<MealType, NutritionTotals>> {
        return mealDao.observeMealTotals(dateEpochDay).map { rows ->
            rows.associate { row ->
                MealType.fromKey(row.mealType) to NutritionTotals(
                    calories = row.calories,
                    protein = row.protein,
                    fat = row.fat,
                    carbs = row.carbs,
                    fiber = row.fiber
                )
            }
        }
    }

    suspend fun saveMealTarget(dateEpochDay: Long, mealType: MealType, calorieTarget: Double?) {
        mealDao.upsertMealTarget(
            MealTargetEntity(
                dateEpochDay = dateEpochDay,
                mealType = mealType.key,
                calorieTarget = calorieTarget?.let { max(it, 0.0) }
            )
        )
    }

    suspend fun saveMealTargets(dateEpochDay: Long, targets: Map<MealType, Double?>) {
        targets.forEach { (mealType, targetValue) ->
            saveMealTarget(
                dateEpochDay = dateEpochDay,
                mealType = mealType,
                calorieTarget = targetValue
            )
        }
    }

    suspend fun addMealEntry(entry: MealEntryEntity) {
        mealDao.insertMealEntry(entry)
    }

    suspend fun updateMealEntryQuantity(entryId: Long, quantity: Double) {
        val existing = mealDao.getMealEntryById(entryId) ?: return
        val safeQuantity = max(quantity, 0.0)

        val food = foodDao.getFood(existing.foodId)
        val updated = if (food != null && food.servingSize > 0.0) {
            val factor = safeQuantity / food.servingSize
            existing.copy(
                quantity = safeQuantity,
                consumedCalories = food.calories * factor,
                consumedProtein = food.protein * factor,
                consumedFat = food.fat * factor,
                consumedCarbs = food.carbs * factor,
                consumedFiber = food.fiber * factor,
                unit = food.unit,
                foodName = food.name
            )
        } else {
            val multiplier = if (abs(existing.quantity) < 1e-9) 0.0 else safeQuantity / existing.quantity
            existing.copy(
                quantity = safeQuantity,
                consumedCalories = existing.consumedCalories * multiplier,
                consumedProtein = existing.consumedProtein * multiplier,
                consumedFat = existing.consumedFat * multiplier,
                consumedCarbs = existing.consumedCarbs * multiplier,
                consumedFiber = existing.consumedFiber * multiplier
            )
        }

        mealDao.updateMealEntry(updated)
    }

    suspend fun deleteMealEntry(entryId: Long) {
        mealDao.deleteMealEntryById(entryId)
    }

    suspend fun cleanupDataOlderThan(cutoffEpochDay: Long): Int {
        val removedEntries = mealDao.deleteMealEntriesOlderThan(cutoffEpochDay)
        val removedTargets = mealDao.deleteMealTargetsOlderThan(cutoffEpochDay)
        return removedEntries + removedTargets
    }

    suspend fun exportFoodsToCsv(outputStream: OutputStream): Int = withContext(Dispatchers.IO) {
        val foods = foodDao.getAllFoods().sortedBy { it.name.lowercase() }
        outputStream.bufferedWriter().use { writer ->
            writer.appendLine("Food,Servings,Unit,Calories,Protein,Fat,Carbs,Fiber,Comments")
            foods.forEach { food ->
                writer.appendLine(
                    listOf(
                        csvEscape(food.name),
                        formatCsvNumber(food.servingSize),
                        csvEscape(food.unit),
                        formatCsvNumber(food.calories),
                        formatCsvNumber(food.protein),
                        formatCsvNumber(food.fat),
                        formatCsvNumber(food.carbs),
                        formatCsvNumber(food.fiber),
                        csvEscape(food.comments)
                    ).joinToString(",")
                )
            }
            writer.flush()
        }
        foods.size
    }

    suspend fun importFoodsFromCsv(inputStream: InputStream): FoodImportResult = withContext(Dispatchers.IO) {
        val rows = inputStream.bufferedReader().readLines().filter { it.isNotBlank() }
        if (rows.isEmpty()) {
            return@withContext FoodImportResult(importedCount = 0, failedNames = emptyList(), duplicateNames = emptyList())
        }

        val existingNames = foodDao.getAllFoodNames()
            .map { it.trim().lowercase() }
            .toMutableSet()

        val duplicates = linkedSetOf<String>()
        val failed = linkedSetOf<String>()
        val toInsert = mutableListOf<FoodEntity>()

        val headerColumns = parseCsvLine(rows.first())
        val headerLookup = headerColumns
            .mapIndexed { index, name -> normalizeHeader(name) to index }
            .toMap()

        val hasHeader = isLikelyHeader(headerLookup)
        val dataRows = if (hasHeader) rows.drop(1) else rows

        dataRows.forEachIndexed { index, row ->
            val rowNumber = index + if (hasHeader) 2 else 1
            val values = parseCsvLine(row)

            fun valueFor(vararg keys: String): String {
                val idx = keys.firstNotNullOfOrNull { headerLookup[normalizeHeader(it)] }
                if (idx != null && idx in values.indices) return values[idx].trim()

                // Fall back to legacy column order when headers are missing.
                return when {
                    "food" in keys || "name" in keys -> values.getOrNull(0).orEmpty().trim()
                    "servings" in keys || "serving size" in keys || "serving" in keys -> values.getOrNull(1).orEmpty().trim()
                    "unit" in keys || "units" in keys -> values.getOrNull(2).orEmpty().trim()
                    "calories" in keys -> values.getOrNull(3).orEmpty().trim()
                    "protein" in keys -> values.getOrNull(4).orEmpty().trim()
                    "fat" in keys -> values.getOrNull(5).orEmpty().trim()
                    "carbs" in keys || "carbohydrates" in keys -> values.getOrNull(6).orEmpty().trim()
                    "fiber" in keys || "fibre" in keys -> values.getOrNull(7).orEmpty().trim()
                    "comments" in keys || "comment" in keys || "notes" in keys -> values.getOrNull(8).orEmpty().trim()
                    else -> ""
                }
            }

            val name = valueFor("food", "name").trim()
            if (name.isBlank()) {
                failed += "<missing name at row $rowNumber>"
                return@forEachIndexed
            }

            val normalizedName = name.lowercase()
            if (normalizedName in existingNames) {
                duplicates += name
                return@forEachIndexed
            }

            val calories = valueFor("calories").toDoubleOrNull()
            val protein = valueFor("protein").toDoubleOrNull()
            val fat = valueFor("fat").toDoubleOrNull()
            val carbs = valueFor("carbs", "carbohydrates").toDoubleOrNull()

            if (calories == null || protein == null || fat == null || carbs == null) {
                failed += name
                return@forEachIndexed
            }

            val servingSize = valueFor("servings", "serving size", "serving").toDoubleOrNull() ?: 100.0
            val unit = valueFor("unit", "units").ifBlank { "g" }
            val fiber = valueFor("fiber", "fibre").toDoubleOrNull() ?: 0.0
            val comments = valueFor("comments", "comment", "notes")

            toInsert += FoodEntity(
                name = name,
                servingSize = max(servingSize, 0.0),
                unit = unit,
                calories = max(calories, 0.0),
                protein = max(protein, 0.0),
                fat = max(fat, 0.0),
                carbs = max(carbs, 0.0),
                fiber = max(fiber, 0.0),
                comments = comments,
                isPreloaded = false
            )

            existingNames += normalizedName
        }

        if (toInsert.isNotEmpty()) {
            foodDao.insertFoods(toInsert)
        }

        FoodImportResult(
            importedCount = toInsert.size,
            failedNames = failed.toList(),
            duplicateNames = duplicates.toList()
        )
    }

    private fun normalizeHeader(value: String): String {
        return value.trim().lowercase().replace("_", " ")
    }

    private fun isLikelyHeader(headerLookup: Map<String, Int>): Boolean {
        return headerLookup.containsKey("food") || headerLookup.containsKey("name")
    }

    private fun parseCsvLine(line: String): List<String> {
        return line.split(CSV_SPLIT_REGEX).map { token -> token.trim().trim('"') }
    }

    private fun csvEscape(value: String): String {
        if (value.isEmpty()) return ""
        val escaped = value.replace("\"", "\"\"")
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private fun formatCsvNumber(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
    }

    companion object {
        private val CSV_SPLIT_REGEX = Regex(""",(?=(?:[^"]*"[^"]*")*[^"]*$)""")
    }
}
