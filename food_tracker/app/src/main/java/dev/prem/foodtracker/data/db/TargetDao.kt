package dev.prem.foodtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TargetDao {
    @Query("SELECT * FROM target_profile WHERE profileId = 1")
    fun observeTargetProfile(): Flow<TargetProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTargetProfile(profile: TargetProfileEntity)
}
