package com.example.foldercleaner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SelectedFolderDao {
    @Query("SELECT * FROM selected_folders ORDER BY displayName ASC")
    fun observeAll(): Flow<List<SelectedFolderEntity>>

    @Query("SELECT * FROM selected_folders ORDER BY displayName ASC")
    suspend fun getAllOnce(): List<SelectedFolderEntity>

    @Query("SELECT * FROM selected_folders WHERE isEnabled = 1")
    suspend fun getAllEnabledOnce(): List<SelectedFolderEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(folder: SelectedFolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(folders: List<SelectedFolderEntity>)

    @Query("UPDATE selected_folders SET isEnabled = :isEnabled WHERE uri = :uri")
    suspend fun updateEnabled(uri: String, isEnabled: Boolean)

    @Query("DELETE FROM selected_folders")
    suspend fun clearAll()

    @Query("DELETE FROM selected_folders WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)
}
