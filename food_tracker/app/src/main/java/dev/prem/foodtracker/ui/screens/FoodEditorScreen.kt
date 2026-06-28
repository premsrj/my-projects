package dev.prem.foodtracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.prem.foodtracker.data.db.FoodEntity
import dev.prem.foodtracker.data.repo.FoodTrackerRepository
import dev.prem.foodtracker.ui.appContainer
import dev.prem.foodtracker.ui.components.parseNonNegativeDouble
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FoodEditorViewModel(
    private val repository: FoodTrackerRepository,
    private val foodId: Long
) : ViewModel() {
    private val _uiState = MutableStateFlow(FoodEditorUiState())
    val uiState: StateFlow<FoodEditorUiState> = _uiState.asStateFlow()

    private var isPreloaded: Boolean = false

    init {
        if (foodId > 0L) {
            viewModelScope.launch {
                val food = repository.getFood(foodId) ?: return@launch
                isPreloaded = food.isPreloaded
                _uiState.value = FoodEditorUiState(
                    name = food.name,
                    servingSize = food.servingSize.toString(),
                    unit = food.unit,
                    calories = food.calories.toString(),
                    protein = food.protein.toString(),
                    fat = food.fat.toString(),
                    carbs = food.carbs.toString(),
                    fiber = food.fiber.toString(),
                    comments = food.comments
                )
            }
        }
    }

    fun updateName(value: String) = update { copy(name = value) }
    fun updateServingSize(value: String) = update { copy(servingSize = value) }
    fun updateUnit(value: String) = update { copy(unit = value) }
    fun updateCalories(value: String) = update { copy(calories = value) }
    fun updateProtein(value: String) = update { copy(protein = value) }
    fun updateFat(value: String) = update { copy(fat = value) }
    fun updateCarbs(value: String) = update { copy(carbs = value) }
    fun updateFiber(value: String) = update { copy(fiber = value) }
    fun updateComments(value: String) = update { copy(comments = value) }

    fun save() {
        val snapshot = _uiState.value

        val servingSize = parseNonNegativeDouble(snapshot.servingSize)
        val calories = parseNonNegativeDouble(snapshot.calories)
        val protein = parseNonNegativeDouble(snapshot.protein)
        val fat = parseNonNegativeDouble(snapshot.fat)
        val carbs = parseNonNegativeDouble(snapshot.carbs)
        val fiber = parseNonNegativeDouble(snapshot.fiber)

        if (
            snapshot.name.isBlank() ||
            snapshot.unit.isBlank() ||
            servingSize == null ||
            calories == null ||
            protein == null ||
            fat == null ||
            carbs == null ||
            fiber == null
        ) {
            update {
                copy(errorMessage = "Fill all numeric fields with non-negative values. Name and unit are required.")
            }
            return
        }

        viewModelScope.launch {
            repository.saveFood(
                FoodEntity(
                    id = if (foodId > 0L) foodId else 0,
                    name = snapshot.name,
                    servingSize = servingSize,
                    unit = snapshot.unit,
                    calories = calories,
                    protein = protein,
                    fat = fat,
                    carbs = carbs,
                    fiber = fiber,
                    comments = snapshot.comments,
                    isPreloaded = isPreloaded
                )
            )

            update { copy(errorMessage = null, saveCompleted = true) }
        }
    }

    fun consumeSaveCompleted() {
        update { copy(saveCompleted = false) }
    }

    private fun update(transform: FoodEditorUiState.() -> FoodEditorUiState) {
        _uiState.value = _uiState.value.transform()
    }

    companion object {
        fun factory(foodId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = this.appContainer()
                FoodEditorViewModel(container.repository, foodId)
            }
        }
    }
}

data class FoodEditorUiState(
    val name: String = "",
    val servingSize: String = "",
    val unit: String = "",
    val calories: String = "",
    val protein: String = "",
    val fat: String = "",
    val carbs: String = "",
    val fiber: String = "",
    val comments: String = "",
    val errorMessage: String? = null,
    val saveCompleted: Boolean = false
)

@Composable
fun FoodEditorRoute(
    foodId: Long,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val viewModel: FoodEditorViewModel = viewModel(
        key = "food-editor-$foodId",
        factory = FoodEditorViewModel.factory(foodId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            viewModel.consumeSaveCompleted()
            onDone()
        }
    }

    FoodEditorScreen(
        foodId = foodId,
        uiState = uiState,
        onBack = onBack,
        onSave = viewModel::save,
        onNameChanged = viewModel::updateName,
        onServingSizeChanged = viewModel::updateServingSize,
        onUnitChanged = viewModel::updateUnit,
        onCaloriesChanged = viewModel::updateCalories,
        onProteinChanged = viewModel::updateProtein,
        onFatChanged = viewModel::updateFat,
        onCarbsChanged = viewModel::updateCarbs,
        onFiberChanged = viewModel::updateFiber,
        onCommentsChanged = viewModel::updateComments
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEditorScreen(
    foodId: Long,
    uiState: FoodEditorUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onNameChanged: (String) -> Unit,
    onServingSizeChanged: (String) -> Unit,
    onUnitChanged: (String) -> Unit,
    onCaloriesChanged: (String) -> Unit,
    onProteinChanged: (String) -> Unit,
    onFatChanged: (String) -> Unit,
    onCarbsChanged: (String) -> Unit,
    onFiberChanged: (String) -> Unit,
    onCommentsChanged: (String) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (foodId > 0L) "Edit food" else "Add food") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Food name") },
                    singleLine = true
                )
            }

            item { NumericField("Serving size", uiState.servingSize, onServingSizeChanged) }
            item {
                OutlinedTextField(
                    value = uiState.unit,
                    onValueChange = onUnitChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Unit") },
                    singleLine = true
                )
            }

            item { NumericField("Calories", uiState.calories, onCaloriesChanged) }
            item { NumericField("Protein", uiState.protein, onProteinChanged) }
            item { NumericField("Fat", uiState.fat, onFatChanged) }
            item { NumericField("Carbs", uiState.carbs, onCarbsChanged) }
            item { NumericField("Fiber", uiState.fiber, onFiberChanged) }

            item {
                OutlinedTextField(
                    value = uiState.comments,
                    onValueChange = onCommentsChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Comments") },
                    minLines = 2
                )
            }

            item {
                if (uiState.errorMessage != null) {
                    Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun NumericField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { incoming ->
            if (incoming.isEmpty() || incoming.matches(Regex("^\\d*\\.?\\d*$"))) {
                onValueChange(incoming)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}
