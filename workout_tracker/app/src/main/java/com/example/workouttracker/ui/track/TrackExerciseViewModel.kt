package com.example.workouttracker.ui.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.workouttracker.WorkoutTrackerApp
import com.example.workouttracker.data.ExerciseEntity
import com.example.workouttracker.data.ExerciseType
import com.example.workouttracker.data.LastWorkoutInfo
import com.example.workouttracker.data.WorkoutRepository
import com.example.workouttracker.data.WorkoutSetEntity
import com.example.workouttracker.data.WorkoutSetWithExercise
import java.time.LocalDate
import java.util.Locale
import kotlin.math.max
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChartMetric(val label: String) {
    ONE_REP_MAX("1RM"),
    VOLUME("Volume"),
    MAX_WEIGHT("Max weight")
}

class TrackExerciseViewModel(
    private val exerciseId: Long,
    private val workoutDate: LocalDate,
    private val repository: WorkoutRepository
) : ViewModel() {
    private val decimalRegex = Regex("^\\d*(\\.\\d{0,2})?$")
    private val intRegex = Regex("^\\d*$")

    val exercise: StateFlow<ExerciseEntity?> = repository.observeExercise(exerciseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val todaySets: StateFlow<List<WorkoutSetWithExercise>> =
        repository.observeSetsForExerciseOnDate(exerciseId, workoutDate)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val historySets: StateFlow<List<WorkoutSetWithExercise>> =
        repository.observeHistoryForExercise(exerciseId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val personalRecordSetIds: StateFlow<Set<Long>> =
        repository.observePersonalRecordSetIds()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _lastWorkout = MutableStateFlow<LastWorkoutInfo?>(null)
    val lastWorkout: StateFlow<LastWorkoutInfo?> = _lastWorkout.asStateFlow()

    private val _weightInput = MutableStateFlow("")
    val weightInput: StateFlow<String> = _weightInput.asStateFlow()

    private val _repsInput = MutableStateFlow("")
    val repsInput: StateFlow<String> = _repsInput.asStateFlow()

    private val _durationInput = MutableStateFlow("")
    val durationInput: StateFlow<String> = _durationInput.asStateFlow()

    private val _distanceInput = MutableStateFlow("")
    val distanceInput: StateFlow<String> = _distanceInput.asStateFlow()

    private val _commentInput = MutableStateFlow("")
    val commentInput: StateFlow<String> = _commentInput.asStateFlow()

    private val _chartMetric = MutableStateFlow(ChartMetric.ONE_REP_MAX)
    val chartMetric: StateFlow<ChartMetric> = _chartMetric.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _setTrackedSignal = MutableStateFlow(0L)
    val setTrackedSignal: StateFlow<Long> = _setTrackedSignal.asStateFlow()

    init {
        initializeInputsAndLastWorkout()
    }

    fun onWeightChange(value: String) {
        if (decimalRegex.matches(value)) {
            _weightInput.value = value
        }
    }

    fun onRepsChange(value: String) {
        if (intRegex.matches(value)) {
            _repsInput.value = value
        }
    }

    fun onDurationChange(value: String) {
        if (intRegex.matches(value)) {
            _durationInput.value = value
        }
    }

    fun onDistanceChange(value: String) {
        if (decimalRegex.matches(value)) {
            _distanceInput.value = value
        }
    }

    fun onCommentChange(value: String) {
        _commentInput.value = value
    }

    fun adjustWeight(increase: Boolean) {
        val step = exercise.value?.weightIncrement ?: 5.0
        val current = _weightInput.value.toDoubleOrNull() ?: 0.0
        val next = if (increase) current + step else (current - step).coerceAtLeast(0.0)
        _weightInput.value = formatDecimalInput(next)
    }

    fun adjustReps(increase: Boolean) {
        val current = _repsInput.value.toIntOrNull() ?: 0
        val next = if (increase) current + 1 else max(0, current - 1)
        _repsInput.value = next.toString()
    }

    fun setChartMetric(metric: ChartMetric) {
        _chartMetric.value = metric
    }

    fun trackSet() {
        val currentExercise = exercise.value ?: return

        val weight = if (currentExercise.type.usesWeight) {
            parseRequiredDecimal(_weightInput.value, "Weight") ?: return
        } else {
            null
        }

        val reps = if (currentExercise.type.usesReps) {
            parseRequiredInt(_repsInput.value, "Reps") ?: return
        } else {
            null
        }

        val duration = if (currentExercise.type.usesTime) {
            parseRequiredInt(_durationInput.value, "Duration") ?: return
        } else {
            null
        }

        val distance = if (currentExercise.type.usesDistance) {
            parseRequiredDecimal(_distanceInput.value, "Distance") ?: return
        } else {
            null
        }

        viewModelScope.launch {
            repository.trackSet(
                exerciseId = exerciseId,
                weight = weight,
                reps = reps,
                durationSeconds = duration,
                distance = distance,
                comment = _commentInput.value,
                workoutDate = workoutDate
            )

            _durationInput.value = ""
            _distanceInput.value = ""
            _commentInput.value = ""
            refreshLastWorkout()
            _setTrackedSignal.value = _setTrackedSignal.value + 1L
        }
    }

    fun updateSetComment(setId: Long, comment: String) {
        viewModelScope.launch {
            repository.updateSetComment(setId, comment)
            _message.value = "Comment updated"
        }
    }

    fun clearSetComment(setId: Long) {
        viewModelScope.launch {
            repository.clearSetComment(setId)
            _message.value = "Comment removed"
        }
    }

    fun updateWeightIncrement(newIncrement: String) {
        val parsed = newIncrement.toDoubleOrNull()
        if (parsed == null || parsed <= 0.0 || parsed > 500.0) {
            _message.value = "Enter a valid increment"
            return
        }

        viewModelScope.launch {
            repository.updateWeightIncrement(exerciseId, parsed)
            _message.value = "Weight increment updated"
        }
    }

    fun clearMessage() {
        _message.update { null }
    }

    private fun parseRequiredDecimal(value: String, fieldName: String): Double? {
        val parsed = value.toDoubleOrNull()
        if (parsed == null) {
            _message.value = "$fieldName is required"
            return null
        }
        return parsed
    }

    private fun parseRequiredInt(value: String, fieldName: String): Int? {
        val parsed = value.toIntOrNull()
        if (parsed == null) {
            _message.value = "$fieldName is required"
            return null
        }
        return parsed
    }

    private fun refreshLastWorkout() {
        viewModelScope.launch {
            _lastWorkout.value = repository.getLastWorkout(exerciseId, referenceDate = workoutDate)
        }
    }

    private fun initializeInputsAndLastWorkout() {
        viewModelScope.launch {
            val todaySnapshot = repository.observeSetsForExerciseOnDate(exerciseId, workoutDate).first()
            val previousWorkout = repository.getLastWorkout(exerciseId, referenceDate = workoutDate)
            _lastWorkout.value = previousWorkout

            if (todaySnapshot.isEmpty()) {
                seedPrimaryInputsFromLastWorkout(previousWorkout)
            }
        }
    }

    private fun seedPrimaryInputsFromLastWorkout(lastWorkoutInfo: LastWorkoutInfo?) {
        val firstSet = lastWorkoutInfo?.sets?.firstOrNull() ?: return
        seedWeightAndReps(firstSet)
    }

    private fun seedWeightAndReps(firstSet: WorkoutSetEntity) {
        if (_weightInput.value.isBlank()) {
            firstSet.weight?.let { _weightInput.value = formatDecimalInput(it) }
        }
        if (_repsInput.value.isBlank()) {
            firstSet.reps?.let { _repsInput.value = it.toString() }
        }
    }

    private fun formatDecimalInput(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
            .trimEnd('0')
            .trimEnd('.')
    }

    companion object {
        fun factory(exerciseId: Long, workoutDate: LocalDate): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WorkoutTrackerApp
                TrackExerciseViewModel(
                    exerciseId = exerciseId,
                    workoutDate = workoutDate,
                    repository = app.appContainer.repository
                )
            }
        }
    }
}
