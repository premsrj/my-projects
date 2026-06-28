package dev.prem.foodtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY name COLLATE NOCASE ASC")
    fun observeFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllFoods(): List<FoodEntity>

    @Query("SELECT name FROM foods")
    suspend fun getAllFoodNames(): List<String>

    @Query("SELECT * FROM foods WHERE id = :foodId")
    fun observeFood(foodId: Long): Flow<FoodEntity?>

    @Query("SELECT * FROM foods WHERE id = :foodId")
    suspend fun getFood(foodId: Long): FoodEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoods(foods: List<FoodEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodEntity): Long

    @Update
    suspend fun updateFood(food: FoodEntity)

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun countFoods(): Int
}
