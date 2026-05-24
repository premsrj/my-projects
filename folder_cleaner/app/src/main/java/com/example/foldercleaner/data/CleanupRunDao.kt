package com.example.foldercleaner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CleanupRunDao {
    @Query("SELECT * FROM cleanup_runs ORDER BY executedAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<CleanupRunEntity>>

    @Insert
    suspend fun insert(run: CleanupRunEntity)

    @Query(
        """
        DELETE FROM cleanup_runs
        WHERE id NOT IN (
            SELECT id FROM cleanup_runs
            ORDER BY executedAtMillis DESC, id DESC
            LIMIT :maxEntries
        )
        """
    )
    suspend fun pruneToLatest(maxEntries: Int)
}
