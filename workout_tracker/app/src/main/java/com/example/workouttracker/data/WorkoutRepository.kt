package com.example.workouttracker.data

import java.time.Instant
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
        "Back",
        "Biceps",
        "Calves",
        "Cardio",
        "Chest",
        "Core",
        "Forearms",
        "Lats",
        "Legs",
        "Shoulder",
        "Triceps"
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

    fun observePersonalRecordSetIds(): Flow<Set<Long>> {
        return dao.observePersonalRecordSetIds().map { it.toSet() }
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
        workoutDate: LocalDate = LocalDate.now(zoneId),
        supersetGroupId: String? = null,
        supersetRound: Int? = null,
        supersetPosition: Int? = null
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
                supersetGroupId = supersetGroupId,
                supersetRound = supersetRound,
                supersetPosition = supersetPosition,
                weight = weight,
                reps = reps,
                durationSeconds = durationSeconds,
                distance = distance,
                comment = comment?.trim()?.takeIf { it.isNotEmpty() }
            )
        )

        recomputePersonalRecordsForExercise(exerciseId)
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

    suspend fun recomputeAllPersonalRecords() {
        val exerciseIds = dao.getExerciseIdsWithLoggedSets()
        exerciseIds.forEach { exerciseId ->
            recomputePersonalRecordsForExercise(exerciseId)
        }
    }

    private suspend fun recomputePersonalRecordsForExercise(exerciseId: Long) {
        val exercise = dao.getExercise(exerciseId) ?: return

        dao.clearPersonalRecordFlagsForExercise(exerciseId)

        if (!exercise.type.usesWeight || !exercise.type.usesReps) {
            return
        }

        val sets = dao.getWorkoutSetsForExerciseChronological(exerciseId)
            .filter { it.weight != null && it.reps != null }

        if (sets.isEmpty()) {
            return
        }

        val setsByDate = sets.groupBy { set ->
            Instant.ofEpochMilli(set.performedAtMillis)
                .atZone(zoneId)
                .toLocalDate()
        }

        var bestSoFar = Double.NEGATIVE_INFINITY

        setsByDate.keys.sorted().forEach { date ->
            val dayBest = setsByDate[date]
                .orEmpty()
                .map { set ->
                    val oneRepMax = calculateOneRepMax(
                        weight = set.weight ?: 0.0,
                        reps = set.reps ?: 0
                    )
                    set to oneRepMax
                }
                .sortedWith(
                    compareByDescending<Pair<WorkoutSetEntity, Double>> { it.second }
                        .thenBy { it.first.performedAtMillis }
                        .thenBy { it.first.id }
                )
                .firstOrNull()
                ?: return@forEach

            if (dayBest.second > bestSoFar) {
                bestSoFar = dayBest.second
                dao.markSetAsPersonalRecord(dayBest.first.id, dayBest.second)
            }
        }
    }

    private fun calculateOneRepMax(weight: Double, reps: Int): Double {
        return weight * (1.0 + reps / 30.0)
    }
}

data class LastWorkoutInfo(
    val date: String,
    val exerciseOrder: Int?,
    val sets: List<WorkoutSetEntity>
)
