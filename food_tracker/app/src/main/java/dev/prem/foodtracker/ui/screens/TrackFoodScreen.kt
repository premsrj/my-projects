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
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import dev.prem.foodtracker.data.db.MealEntryEntity
import dev.prem.foodtracker.data.model.MealType
import dev.prem.foodtracker.data.model.NutritionTotals
import dev.prem.foodtracker.data.repo.FoodTrackerRepository
import dev.prem.foodtracker.ui.appContainer
import dev.prem.foodtracker.ui.components.formatDouble
import dev.prem.foodtracker.ui.components.parseNonNegativeDouble
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrackFoodUiState(
    val selectedDate: LocalDate,
    val mealType: MealType,
    val food: FoodEntity? = null,
    val quantityInput: String = "",
    val mealTotals: NutritionTotals = NutritionTotals(),
    val mealTargetCalories: Double? = null,
    val errorMessage: String? = null,
    val saveCompleted: Boolean = false
)

class TrackFoodViewModel(
    private val repository: FoodTrackerRepository,
    private val selectedDate: LocalDate,
    private val mealType: MealType,
    private val foodId: Long
) : ViewModel() {
    private val quantityInput = MutableStateFlow("")
    private val error = MutableStateFlow<String?>(null)
    private val saveCompleted = MutableStateFlow(false)

    private val baseUiState = combine(
        repository.observeFood(foodId),
        repository.observeMealTotals(selectedDate.toEpochDay()),
        repository.observeMealTarget(selectedDate.toEpochDay(), mealType),
        quantityInput,
        error
    ) { food, totalsByMeal, mealTarget, quantity, errorMessage ->
        TrackFoodUiState(
            selectedDate = selectedDate,
            mealType = mealType,
            food = food,
            quantityInput = quantity,
            mealTotals = totalsByMeal[mealType] ?: NutritionTotals(),
            mealTargetCalories = mealTarget?.calorieTarget,
            errorMessage = errorMessage,
            saveCompleted = false
        )
    }

    val uiState: StateFlow<TrackFoodUiState> = combine(baseUiState, saveCompleted) { base, saved ->
        base.copy(saveCompleted = saved)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrackFoodUiState(
            selectedDate = selectedDate,
            mealType = mealType
        )
    )

    fun onQuantityChanged(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            quantityInput.value = value
            error.value = null
        }
    }

    fun saveEntry() {
        val state = uiState.value
        val food = state.food ?: return
        val quantity = parseNonNegativeDouble(state.quantityInput)
        if (quantity == null) {
            error.value = "Enter a non-negative quantity."
            return
        }

        val factor = if (food.servingSize <= 0.0) 0.0 else quantity / food.servingSize

        viewModelScope.launch {
            repository.addMealEntry(
                MealEntryEntity(
                    dateEpochDay = selectedDate.toEpochDay(),
                    mealType = mealType.key,
                    foodId = food.id,
                    foodName = food.name,
                    quantity = quantity,
                    unit = food.unit,
                    consumedCalories = food.calories * factor,
                    consumedProtein = food.protein * factor,
                    consumedFat = food.fat * factor,
                    consumedCarbs = food.carbs * factor,
                    consumedFiber = food.fiber * factor
                )
            )
            saveCompleted.value = true
        }
    }

    fun consumeSaveCompleted() {
        saveCompleted.value = false
    }

    companion object {
        fun factory(
            selectedDate: LocalDate,
            mealType: MealType,
            foodId: Long
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = this.appContainer()
                TrackFoodViewModel(container.repository, selectedDate, mealType, foodId)
            }
        }
    }
}

@Composable
fun TrackFoodRoute(
    selectedDate: LocalDate,
    mealType: MealType,
    foodId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val viewModel: TrackFoodViewModel = viewModel(
        key = "track-food-${selectedDate.toEpochDay()}-${mealType.key}-$foodId",
        factory = TrackFoodViewModel.factory(selectedDate, mealType, foodId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            viewModel.consumeSaveCompleted()
            onSaved()
        }
    }

    TrackFoodScreen(
        uiState = uiState,
        onBack = onBack,
        onQuantityChanged = viewModel::onQuantityChanged,
        onSave = viewModel::saveEntry
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackFoodScreen(
    uiState: TrackFoodUiState,
    onBack: () -> Unit,
    onQuantityChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    val food = uiState.food
    val quantity = parseNonNegativeDouble(uiState.quantityInput) ?: 0.0
    val factor = if (food == null || food.servingSize <= 0.0) 0.0 else quantity / food.servingSize
    val itemCalories = (food?.calories ?: 0.0) * factor
    val itemProtein = (food?.protein ?: 0.0) * factor
    val itemFat = (food?.fat ?: 0.0) * factor
    val itemCarbs = (food?.carbs ?: 0.0) * factor
    val itemFiber = (food?.fiber ?: 0.0) * factor
    val projectedCalories = uiState.mealTotals.calories + itemCalories
    val projectedProtein = uiState.mealTotals.protein + itemProtein
    val projectedFat = uiState.mealTotals.fat + itemFat
    val projectedCarbs = uiState.mealTotals.carbs + itemCarbs
    val projectedFiber = uiState.mealTotals.fiber + itemFiber

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Track for ${uiState.mealType.label}") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (food == null) {
                    Text("Food not found", color = MaterialTheme.colorScheme.error)
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = food.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("Standard serving: ${formatDouble(food.servingSize)} ${food.unit}")
                            Text("Calories: ${formatDouble(food.calories)}")
                            Text("Protein: ${formatDouble(food.protein)}")
                            Text("Fat: ${formatDouble(food.fat)}")
                            Text("Carbs: ${formatDouble(food.carbs)}")
                            Text("Fiber: ${formatDouble(food.fiber)}")
                            if (food.comments.isNotBlank()) {
                                Text(
                                    text = food.comments,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.quantityInput,
                    onValueChange = onQuantityChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Quantity eaten") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    placeholder = { Text("0") },
                    singleLine = true
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "This item totals",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("Calories: ${formatDouble(itemCalories)}")
                        Text("Protein: ${formatDouble(itemProtein)}")
                        Text("Fat: ${formatDouble(itemFat)}")
                        Text("Carbs: ${formatDouble(itemCarbs)}")
                        Text("Fiber: ${formatDouble(itemFiber)}")
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Meal totals",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (uiState.mealTargetCalories != null) {
                                "Calories: ${formatDouble(projectedCalories)} / ${formatDouble(uiState.mealTargetCalories)}"
                            } else {
                                "Calories: ${formatDouble(projectedCalories)}"
                            }
                        )
                        Text("Protein: ${formatDouble(projectedProtein)}")
                        Text("Fat: ${formatDouble(projectedFat)}")
                        Text("Carbs: ${formatDouble(projectedCarbs)}")
                        Text("Fiber: ${formatDouble(projectedFiber)}")
                        Text(
                            text = "Base tracked before this item: ${formatDouble(uiState.mealTotals.calories)} kcal",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                if (uiState.errorMessage != null) {
                    Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = food != null
                ) {
                    Text("Add to ${uiState.mealType.label}")
                }
            }
        }
    }
}
