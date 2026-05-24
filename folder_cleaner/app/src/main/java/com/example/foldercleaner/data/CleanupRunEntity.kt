package com.example.foldercleaner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cleanup_runs")
data class CleanupRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val executedAtMillis: Long,
    val trigger: String,
    val scannedCount: Int,
    val deletedCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val reclaimedBytes: Long
)
