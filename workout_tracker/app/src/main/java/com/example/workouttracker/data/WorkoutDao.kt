package com.example.workouttracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE lower(name) = lower(:name))")
    suspend fun categoryExists(name: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query(
        """
        SELECT e.id, e.name, e.description, e.type, e.categoryId, e.weightIncrement, c.name AS categoryName
        FROM exercises e
        INNER JOIN categories c ON c.id = e.categoryId
        ORDER BY c.name COLLATE NOCASE, e.name COLLATE NOCASE
        """
    )
    fun observeExercisesWithCategory(): Flow<List<ExerciseWithCategory>>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    fun observeExercise(id: Long): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getExercise(id: Long): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Query(
        """
        SELECT COALESCE(MAX(sequenceInExercise), 0)
        FROM workout_sets
        WHERE exerciseId = :exerciseId AND performedAtMillis BETWEEN :dayStartMillis AND :dayEndMillis
        """
    )
    suspend fun getMaxSequenceForDay(
        exerciseId: Long,
        dayStartMillis: Long,
        dayEndMillis: Long
    ): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWorkoutSet(set: WorkoutSetEntity): Long

    @Query("SELECT * FROM workout_sets WHERE id = :setId LIMIT 1")
    suspend fun getWorkoutSetById(setId: Long): WorkoutSetEntity?

    @Update
    suspend fun updateWorkoutSet(set: WorkoutSetEntity)

    @Query(
        """
        SELECT s.*, e.name AS exerciseName, e.type AS exerciseType, c.name AS categoryName
        FROM workout_sets s
        INNER JOIN exercises e ON e.id = s.exerciseId
        INNER JOIN categories c ON c.id = e.categoryId
        WHERE s.performedAtMillis BETWEEN :startMillis AND :endMillis
        ORDER BY s.performedAtMillis DESC, s.id DESC
        """
    )
    fun observeSetsInRange(startMillis: Long, endMillis: Long): Flow<List<WorkoutSetWithExercise>>

    @Query(
        """
        SELECT s.*, e.name AS exerciseName, e.type AS exerciseType, c.name AS categoryName
        FROM workout_sets s
        INNER JOIN exercises e ON e.id = s.exerciseId
        INNER JOIN categories c ON c.id = e.categoryId
        WHERE s.exerciseId = :exerciseId
        ORDER BY s.performedAtMillis DESC, s.id DESC
        """
    )
    fun observeSetsForExercise(exerciseId: Long): Flow<List<WorkoutSetWithExercise>>

    @Query(
        """
        SELECT s.*, e.name AS exerciseName, e.type AS exerciseType, c.name AS categoryName
        FROM workout_sets s
        INNER JOIN exercises e ON e.id = s.exerciseId
        INNER JOIN categories c ON c.id = e.categoryId
        WHERE s.exerciseId = :exerciseId AND s.performedAtMillis BETWEEN :startMillis AND :endMillis
        ORDER BY s.performedAtMillis DESC, s.id DESC
        """
    )
    fun observeSetsForExerciseInRange(
        exerciseId: Long,
        startMillis: Long,
        endMillis: Long
    ): Flow<List<WorkoutSetWithExercise>>

    @Query(
        """
        SELECT workoutDate FROM (
            SELECT date(datetime(performedAtMillis / 1000, 'unixepoch', 'localtime')) AS workoutDate,
                   MAX(performedAtMillis) AS maxMillis
            FROM workout_sets
            WHERE exerciseId = :exerciseId
            GROUP BY workoutDate
            ORDER BY maxMillis DESC
        )
        """
    )
    suspend fun getWorkoutDatesForExercise(exerciseId: Long): List<String>

    @Query(
        """
        SELECT *
        FROM workout_sets
        WHERE exerciseId = :exerciseId
          AND date(datetime(performedAtMillis / 1000, 'unixepoch', 'localtime')) = :workoutDate
        ORDER BY performedAtMillis ASC, id ASC
        """
    )
    suspend fun getSetsForExerciseOnDate(exerciseId: Long, workoutDate: String): List<WorkoutSetEntity>

    @Query(
        """
        SELECT exerciseId, MIN(performedAtMillis) AS firstPerformedAt
        FROM workout_sets
        WHERE date(datetime(performedAtMillis / 1000, 'unixepoch', 'localtime')) = :workoutDate
        GROUP BY exerciseId
        ORDER BY firstPerformedAt ASC
        """
    )
    suspend fun getExerciseOrderForDate(workoutDate: String): List<ExerciseOrderRow>

    @Query(
        """
        SELECT DISTINCT date(datetime(performedAtMillis / 1000, 'unixepoch', 'localtime')) AS workoutDate
        FROM workout_sets
        ORDER BY workoutDate
        """
    )
    fun observeTrackedWorkoutDates(): Flow<List<String>>
}
