package com.example.foldercleaner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IgnoredExtensionDao {
    @Query("SELECT * FROM ignored_extensions ORDER BY extension ASC")
    fun observeAll(): Flow<List<IgnoredExtensionEntity>>

    @Query("SELECT * FROM ignored_extensions")
    suspend fun getAllOnce(): List<IgnoredExtensionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(extension: IgnoredExtensionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(extensions: List<IgnoredExtensionEntity>)

    @Query("DELETE FROM ignored_extensions")
    suspend fun clearAll()

    @Query("DELETE FROM ignored_extensions WHERE extension = :extension")
    suspend fun deleteByExtension(extension: String)
}
