package com.example.workouttracker.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutRepository(
    private val dao: WorkoutDao,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    private val defaultCategories = listOf(
        "back",
        "biceps",
        "calves",
        "cardio",
        "chest",
        "core",
        "forearms",
        "lats",
        "legs",
        "shoulder",
        "triceps"
    )

    fun observeCategories(): Flow<List<CategoryEntity>> = dao.observeCategories()

    fun observeExercisesWithCategory(): Flow<List<ExerciseWithCategory>> = dao.observeExercisesWithCategory()

    fun observeExercise(exerciseId: Long): Flow<ExerciseEntity?> = dao.observeExercise(exerciseId)

    fun observeTodaySets(): Flow<List<WorkoutSetWithExercise>> {
        return observeSetsForDate(LocalDate.now(zoneId))
    }

    fun observeSetsForDate(date: LocalDate): Flow<List<WorkoutSetWithExercise>> {
        val (startMillis, endMillis) = dayRange(date)
        return dao.observeSetsInRange(startMillis, endMillis)
    }

    fun observeTodaySetsForExercise(exerciseId: Long): Flow<List<WorkoutSetWithExercise>> {
        return observeSetsForExerciseOnDate(exerciseId, LocalDate.now(zoneId))
    }

    fun observeSetsForExerciseOnDate(
        exerciseId: Long,
        date: LocalDate
    ): Flow<List<WorkoutSetWithExercise>> {
        val (startMillis, endMillis) = dayRange(date)
        return dao.observeSetsForExerciseInRange(exerciseId, startMillis, endMillis)
    }

    fun observeHistoryForExercise(exerciseId: Long): Flow<List<WorkoutSetWithExercise>> =
        dao.observeSetsForExercise(exerciseId)

    fun observeTrackedWorkoutDates(): Flow<Set<LocalDate>> {
        return dao.observeTrackedWorkoutDates().map { dates ->
            dates.mapNotNull { dateValue ->
                runCatching { LocalDate.parse(dateValue) }.getOrNull()
            }.toSet()
        }
    }

    suspend fun ensureDefaultCategories() {
        defaultCategories.forEach { categoryName ->
            if (!dao.categoryExists(categoryName)) {
                dao.insertCategory(CategoryEntity(name = categoryName, isDefault = true))
            }
        }
    }

    suspend fun addCategory(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return false
        }
        if (dao.categoryExists(trimmed)) {
            return false
        }
        dao.insertCategory(CategoryEntity(name = trimmed, isDefault = false))
        return true
    }

    suspend fun addExercise(
        name: String,
        description: String,
        type: ExerciseType,
        categoryId: Long
    ): Long {
        return dao.insertExercise(
            ExerciseEntity(
                name = name.trim(),
                description = description.trim(),
                type = type,
                categoryId = categoryId
            )
        )
    }

    suspend fun updateWeightIncrement(exerciseId: Long, increment: Double) {
        val exercise = dao.getExercise(exerciseId) ?: return
        dao.updateExercise(exercise.copy(weightIncrement = increment))
    }

    suspend fun trackSet(
        exerciseId: Long,
        weight: Double?,
        reps: Int?,
        durationSeconds: Int?,
        distance: Double?,
        comment: String?,
        workoutDate: LocalDate = LocalDate.now(zoneId)
    ) {
        val currentLocalTime = LocalTime.now(zoneId)
        val performedAtMillis = workoutDate
            .atTime(currentLocalTime)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val (startMillis, endMillis) = dayRange(workoutDate)
        val nextSequence = dao.getMaxSequenceForDay(exerciseId, startMillis, endMillis) + 1
        dao.insertWorkoutSet(
            WorkoutSetEntity(
                exerciseId = exerciseId,
                performedAtMillis = performedAtMillis,
                sequenceInExercise = nextSequence,
                weight = weight,
                reps = reps,
                durationSeconds = durationSeconds,
                distance = distance,
                comment = comment?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
    }

    suspend fun updateSetComment(setId: Long, comment: String) {
        val workoutSet = dao.getWorkoutSetById(setId) ?: return
        dao.updateWorkoutSet(
            workoutSet.copy(
                comment = comment.trim().takeIf { it.isNotEmpty() }
            )
        )
    }

    suspend fun clearSetComment(setId: Long) {
        val workoutSet = dao.getWorkoutSetById(setId) ?: return
        dao.updateWorkoutSet(workoutSet.copy(comment = null))
    }

    suspend fun getLastWorkout(
        exerciseId: Long,
        referenceDate: LocalDate = LocalDate.now(zoneId)
    ): LastWorkoutInfo? {
        val workoutDates = dao.getWorkoutDatesForExercise(exerciseId)
        if (workoutDates.isEmpty()) {
            return null
        }

        val lastWorkoutDate = workoutDates.firstOrNull { dateValue ->
            runCatching { LocalDate.parse(dateValue) }
                .getOrNull()
                ?.isBefore(referenceDate) == true
        } ?: return null

        val sets = dao.getSetsForExerciseOnDate(exerciseId, lastWorkoutDate)
        if (sets.isEmpty()) {
            return null
        }

        val orderedExercises = dao.getExerciseOrderForDate(lastWorkoutDate)
        val orderIndex = orderedExercises.indexOfFirst { it.exerciseId == exerciseId }
            .takeIf { it >= 0 }
            ?.plus(1)

        return LastWorkoutInfo(
            date = lastWorkoutDate,
            exerciseOrder = orderIndex,
            sets = sets
        )
    }

    private fun dayRange(date: LocalDate): Pair<Long, Long> {
        val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
        return start to end
    }
}

data class LastWorkoutInfo(
    val date: String,
    val exerciseOrder: Int?,
    val sets: List<WorkoutSetEntity>
)
