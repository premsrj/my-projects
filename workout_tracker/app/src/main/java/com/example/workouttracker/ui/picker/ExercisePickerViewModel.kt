package com.example.workouttracker.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.workouttracker.WorkoutTrackerApp
import com.example.workouttracker.data.CategoryEntity
import com.example.workouttracker.data.ExerciseType
import com.example.workouttracker.data.ExerciseWithCategory
import com.example.workouttracker.data.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExercisePickerUiState(
    val searchQuery: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val groups: List<CategoryExerciseGroup> = emptyList(),
    val message: String? = null
)

data class CategoryExerciseGroup(
    val category: CategoryEntity,
    val exercises: List<ExerciseWithCategory>
)

class ExercisePickerViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ExercisePickerUiState> = combine(
        searchQuery,
        repository.observeCategories(),
        repository.observeExercisesWithCategory(),
        message
    ) { query, categories, exercises, currentMessage ->
        val filtered = if (query.isBlank()) {
            exercises
        } else {
            exercises.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.categoryName.contains(query, ignoreCase = true)
            }
        }

        val grouped = categories.map { category ->
            CategoryExerciseGroup(
                category = category,
                exercises = filtered.filter { it.categoryId == category.id }
            )
        }.filter { query.isBlank() || it.exercises.isNotEmpty() }

        ExercisePickerUiState(
            searchQuery = query,
            categories = categories,
            groups = grouped,
            message = currentMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExercisePickerUiState())

    fun onSearchQueryChange(value: String) {
        searchQuery.value = value
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            val wasCreated = repository.addCategory(name)
            message.value = if (wasCreated) {
                "Category added"
            } else {
                "Could not add category"
            }
        }
    }

    fun addExercise(
        name: String,
        description: String,
        type: ExerciseType,
        categoryId: Long,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                message.value = "Exercise name is required"
                return@launch
            }
            val exerciseId = repository.addExercise(trimmedName, description, type, categoryId)
            message.value = "Exercise added"
            onSuccess(exerciseId)
        }
    }

    fun clearMessage() {
        message.update { null }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WorkoutTrackerApp
                ExercisePickerViewModel(app.appContainer.repository)
            }
        }
    }
}
