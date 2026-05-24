package dev.prem.budgetwidget.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query(
        "SELECT * FROM expenses " +
            "WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay " +
            "ORDER BY dateEpochDay DESC, id DESC"
    )
    fun observeExpensesInRange(
        startEpochDay: Long,
        endEpochDay: Long
    ): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM expenses " +
            "WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay"
    )
    suspend fun sumAmountsInRange(startEpochDay: Long, endEpochDay: Long): Double

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)
}
