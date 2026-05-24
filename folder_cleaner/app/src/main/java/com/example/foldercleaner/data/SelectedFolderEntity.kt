package com.example.foldercleaner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "selected_folders")
data class SelectedFolderEntity(
    @PrimaryKey val uri: String,
    val displayName: String,
    val isEnabled: Boolean = true
)
