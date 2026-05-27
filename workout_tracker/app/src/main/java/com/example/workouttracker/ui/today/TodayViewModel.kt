package com.example.workouttracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.workouttracker.WorkoutTrackerApp
import com.example.workouttracker.data.ExerciseType
import com.example.workouttracker.data.WorkoutRepository
import com.example.workouttracker.data.WorkoutSetWithExercise
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodayUiState(
    val timelineItems: List<TodayTimelineItem> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val trackedDates: Set<LocalDate> = emptySet(),
    val personalRecordSetIds: Set<Long> = emptySet(),
    val isRecomputingPrs: Boolean = false
)

sealed interface TodayTimelineItem {
    val startedAtMillis: Long

    data class NonSupersetExerciseBlock(
        val entry: TodayExerciseEntry,
        override val startedAtMillis: Long
    ) : TodayTimelineItem

    data class SupersetBlock(
        val groupId: String,
        val rounds: List<TodaySupersetRoundEntry>,
        override val startedAtMillis: Long
    ) : TodayTimelineItem
}

data class TodaySupersetRoundEntry(
    val round: Int,
    val exercises: List<TodayExerciseEntry>
)

data class TodayExerciseEntry(
    val exerciseId: Long,
    val exerciseName: String,
    val categoryName: String,
    val type: ExerciseType,
    val sets: List<WorkoutSetWithExercise>
)

private data class NonSupersetBlockBuilder(
    val entry: TodayExerciseEntry,
    val startedAtMillis: Long
)

class TodayViewModel(
    private val selectedDate: LocalDate,
    private val repository: WorkoutRepository
) : ViewModel() {
    private val isRecomputingPrs = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val snackbarMessage: StateFlow<String?> = message.asStateFlow()

    val uiState: StateFlow<TodayUiState> = combine(
        repository.observeSetsForDate(selectedDate),
        repository.observeTrackedWorkoutDates(),
        repository.observePersonalRecordSetIds(),
        isRecomputingPrs
    ) { allSets, trackedDates, personalRecordSetIds, recomputeInProgress ->
            val chronologicalSets = allSets.sortedBy { it.set.performedAtMillis }
            val supersetBuckets = linkedMapOf<String, MutableList<WorkoutSetWithExercise>>()
            val nonSupersetSets = mutableListOf<WorkoutSetWithExercise>()

            chronologicalSets.forEach { set ->
                val groupId = set.set.supersetGroupId
                val round = set.set.supersetRound
                if (!groupId.isNullOrBlank() && round != null) {
                    supersetBuckets.getOrPut(groupId) { mutableListOf() }.add(set)
                } else {
                    nonSupersetSets += set
                }
            }

            val nonSupersetExerciseBlocks = nonSupersetSets
                .groupBy { it.set.exerciseId }
                .values
                .map { setsForExercise ->
                    val orderedSets = setsForExercise.sortedWith(
                        compareBy<WorkoutSetWithExercise> { it.set.sequenceInExercise }
                            .thenBy { it.set.performedAtMillis }
                            .thenBy { it.set.id }
                    )
                    val first = orderedSets.first()

                    NonSupersetBlockBuilder(
                        entry = TodayExerciseEntry(
                            exerciseId = first.set.exerciseId,
                            exerciseName = first.exerciseName,
                            categoryName = first.categoryName,
                            type = first.exerciseType,
                            sets = orderedSets
                        ),
                        startedAtMillis = orderedSets.minOfOrNull { it.set.performedAtMillis } ?: 0L
                    )
                }

            val supersetBlocks = supersetBuckets.map { (groupId, groupedSets) ->
                val rounds = groupedSets
                    .groupBy { it.set.supersetRound ?: Int.MAX_VALUE }
                    .entries
                    .sortedBy { it.key }
                    .map { (round, setsInRound) ->
                        val exercises = setsInRound
                            .groupBy { it.set.exerciseId }
                            .values
                            .map { setsForExercise ->
                                val orderedSets = setsForExercise.sortedWith(
                                    compareBy<WorkoutSetWithExercise> { it.set.supersetPosition ?: Int.MAX_VALUE }
                                        .thenBy { it.set.performedAtMillis }
                                        .thenBy { it.set.id }
                                )
                                val first = orderedSets.first()

                                TodayExerciseEntry(
                                    exerciseId = first.set.exerciseId,
                                    exerciseName = first.exerciseName,
                                    categoryName = first.categoryName,
                                    type = first.exerciseType,
                                    sets = orderedSets
                                )
                            }
                            .sortedWith(
                                compareBy<TodayExerciseEntry> {
                                    it.sets.minOfOrNull { set -> set.set.supersetPosition ?: Int.MAX_VALUE }
                                        ?: Int.MAX_VALUE
                                }.thenBy {
                                    it.sets.minOfOrNull { set -> set.set.performedAtMillis } ?: Long.MAX_VALUE
                                }
                            )

                        TodaySupersetRoundEntry(
                            round = round,
                            exercises = exercises
                        )
                    }

                TodayTimelineItem.SupersetBlock(
                    groupId = groupId,
                    rounds = rounds,
                    startedAtMillis = groupedSets.minOfOrNull { it.set.performedAtMillis } ?: 0L
                )
            }

            val timelineItems = (
                supersetBlocks + nonSupersetExerciseBlocks.map { block ->
                    TodayTimelineItem.NonSupersetExerciseBlock(
                        entry = block.entry,
                        startedAtMillis = block.startedAtMillis
                    )
                }
                )
                .sortedBy { it.startedAtMillis }

            TodayUiState(
                timelineItems = timelineItems,
                selectedDate = selectedDate,
                trackedDates = trackedDates,
                personalRecordSetIds = personalRecordSetIds,
                isRecomputingPrs = recomputeInProgress
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun recomputePersonalRecords() {
        if (isRecomputingPrs.value) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            isRecomputingPrs.value = true
            runCatching {
                repository.recomputeAllPersonalRecords()
            }.onSuccess {
                message.value = "PR recompute completed"
            }.onFailure {
                message.value = "Failed to recompute PRs"
            }
            isRecomputingPrs.value = false
        }
    }

    fun clearMessage() {
        message.update { null }
    }

    fun deleteSets(setIds: Collection<Long>) {
        if (setIds.isEmpty()) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.deleteWorkoutSets(setIds)
            }.onSuccess {
                message.value = "Deleted selected workout(s)"
            }.onFailure {
                message.value = "Failed to delete selected workout(s)"
            }
        }
    }

    companion object {
        fun factory(selectedDate: LocalDate): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WorkoutTrackerApp
                TodayViewModel(
                    selectedDate = selectedDate,
                    repository = app.appContainer.repository
                )
            }
        }
    }
}
