package dev.prem.foodtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meal_entries WHERE dateEpochDay = :dateEpochDay")
    fun observeEntriesForDate(dateEpochDay: Long): Flow<List<MealEntryEntity>>

    @Query("SELECT * FROM meal_entries WHERE dateEpochDay = :dateEpochDay AND mealType = :mealType ORDER BY id ASC")
    fun observeEntriesForMeal(dateEpochDay: Long, mealType: String): Flow<List<MealEntryEntity>>

    @Query(
        """
        SELECT
            COALESCE(SUM(consumedCalories), 0) AS calories,
            COALESCE(SUM(consumedProtein), 0) AS protein,
            COALESCE(SUM(consumedFat), 0) AS fat,
            COALESCE(SUM(consumedCarbs), 0) AS carbs,
            COALESCE(SUM(consumedFiber), 0) AS fiber
        FROM meal_entries
        WHERE dateEpochDay = :dateEpochDay
        """
    )
    fun observeDailyTotals(dateEpochDay: Long): Flow<NutrientTotalsRow>

    @Query(
        """
        SELECT
            mealType,
            COALESCE(SUM(consumedCalories), 0) AS calories,
            COALESCE(SUM(consumedProtein), 0) AS protein,
            COALESCE(SUM(consumedFat), 0) AS fat,
            COALESCE(SUM(consumedCarbs), 0) AS carbs,
            COALESCE(SUM(consumedFiber), 0) AS fiber
        FROM meal_entries
        WHERE dateEpochDay = :dateEpochDay
        GROUP BY mealType
        """
    )
    fun observeMealTotals(dateEpochDay: Long): Flow<List<MealTotalsRow>>

    @Query(
        """
        SELECT
            dateEpochDay,
            COALESCE(SUM(consumedCalories), 0) AS calories
        FROM meal_entries
        WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        GROUP BY dateEpochDay
        ORDER BY dateEpochDay ASC
        """
    )
    fun observeDailyCaloriesRange(startEpochDay: Long, endEpochDay: Long): Flow<List<DailyCaloriesRow>>

    @Query("SELECT * FROM meal_targets WHERE dateEpochDay = :dateEpochDay")
    fun observeMealTargets(dateEpochDay: Long): Flow<List<MealTargetEntity>>

    @Query("SELECT * FROM meal_targets WHERE dateEpochDay = :dateEpochDay AND mealType = :mealType")
    fun observeMealTarget(dateEpochDay: Long, mealType: String): Flow<MealTargetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMealTarget(mealTarget: MealTargetEntity)

    @Insert
    suspend fun insertMealEntry(entry: MealEntryEntity)

    @Query("SELECT * FROM meal_entries WHERE id = :entryId")
    suspend fun getMealEntryById(entryId: Long): MealEntryEntity?

    @Update
    suspend fun updateMealEntry(entry: MealEntryEntity)

    @Query("DELETE FROM meal_entries WHERE id = :entryId")
    suspend fun deleteMealEntryById(entryId: Long)

    @Query("DELETE FROM meal_entries WHERE dateEpochDay < :cutoffEpochDay")
    suspend fun deleteMealEntriesOlderThan(cutoffEpochDay: Long): Int

    @Query("DELETE FROM meal_targets WHERE dateEpochDay < :cutoffEpochDay")
    suspend fun deleteMealTargetsOlderThan(cutoffEpochDay: Long): Int
}
