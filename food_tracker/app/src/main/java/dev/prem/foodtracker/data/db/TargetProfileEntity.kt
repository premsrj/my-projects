package dev.prem.foodtracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "target_profile")
data class TargetProfileEntity(
    @PrimaryKey val profileId: Int = 1,
    val calorieTarget: Double? = null,
    val proteinTarget: Double? = null,
    val fatTarget: Double? = null,
    val carbsTarget: Double? = null,
    val fiberTarget: Double? = null
)
