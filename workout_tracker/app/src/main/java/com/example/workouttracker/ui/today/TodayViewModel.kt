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
    val entries: List<TodayExerciseEntry> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val trackedDates: Set<LocalDate> = emptySet(),
    val personalRecordSetIds: Set<Long> = emptySet(),
    val isRecomputingPrs: Boolean = false
)

data class TodayExerciseEntry(
    val exerciseId: Long,
    val exerciseName: String,
    val categoryName: String,
    val type: ExerciseType,
    val sets: List<WorkoutSetWithExercise>
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
            val grouped = allSets
                .groupBy { it.set.exerciseId }
                .values
                .map { sets ->
                    val first = sets.first()
                    TodayExerciseEntry(
                        exerciseId = first.set.exerciseId,
                        exerciseName = first.exerciseName,
                        categoryName = first.categoryName,
                        type = first.exerciseType,
                        sets = sets.sortedWith(
                            compareBy<WorkoutSetWithExercise> { it.set.sequenceInExercise }
                                .thenBy { it.set.performedAtMillis }
                        )
                    )
                }
                .sortedBy { it.exerciseName.lowercase() }

            TodayUiState(
                entries = grouped,
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
