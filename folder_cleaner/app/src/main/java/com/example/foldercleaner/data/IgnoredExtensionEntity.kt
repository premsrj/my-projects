package com.example.foldercleaner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ignored_extensions")
data class IgnoredExtensionEntity(
    @PrimaryKey val extension: String
)
