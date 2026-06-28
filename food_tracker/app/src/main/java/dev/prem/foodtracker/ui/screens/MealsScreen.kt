package dev.prem.foodtracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import dev.prem.foodtracker.data.db.MealEntryEntity
import dev.prem.foodtracker.data.model.MealType
import dev.prem.foodtracker.data.model.NutritionTotals
import dev.prem.foodtracker.data.repo.FoodTrackerRepository
import dev.prem.foodtracker.ui.appContainer
import dev.prem.foodtracker.ui.components.formatDouble
import dev.prem.foodtracker.ui.components.parseNonNegativeDouble
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val mealDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

data class MealCardState(
    val mealType: MealType,
    val entries: List<MealEntryEntity>,
    val totals: NutritionTotals,
    val targetCalories: Double?
)

data class MealsUiState(
    val selectedDate: LocalDate,
    val dailyCalorieTarget: Double?,
    val mealCards: List<MealCardState>
)

class MealsViewModel(
    private val repository: FoodTrackerRepository,
    private val selectedDate: LocalDate
) : ViewModel() {
    val uiState: StateFlow<MealsUiState> = combine(
        repository.observeEntriesForDate(selectedDate.toEpochDay()),
        repository.observeMealTotals(selectedDate.toEpochDay()),
        repository.observeMealTargets(selectedDate.toEpochDay()),
        repository.observeTargetProfile()
    ) { entries, totalsByMeal, mealTargets, profile ->
        val entriesByMeal = entries.groupBy { MealType.fromKey(it.mealType) }
        val targetsMap = mealTargets.associate { MealType.fromKey(it.mealType) to it.calorieTarget }

        val cards = MealType.ordered.map { meal ->
            MealCardState(
                mealType = meal,
                entries = entriesByMeal[meal].orEmpty(),
                totals = totalsByMeal[meal] ?: NutritionTotals(),
                targetCalories = targetsMap[meal]
            )
        }

        MealsUiState(
            selectedDate = selectedDate,
            dailyCalorieTarget = profile.calorieTarget,
            mealCards = cards
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MealsUiState(
            selectedDate = selectedDate,
            dailyCalorieTarget = null,
            mealCards = MealType.ordered.map {
                MealCardState(
                    mealType = it,
                    entries = emptyList(),
                    totals = NutritionTotals(),
                    targetCalories = null
                )
            }
        )
    )

    fun saveMealTargets(targets: Map<MealType, Double?>) {
        viewModelScope.launch {
            repository.saveMealTargets(
                dateEpochDay = selectedDate.toEpochDay(),
                targets = targets
            )
        }
    }

    fun updateEntryQuantity(entryId: Long, quantity: Double) {
        viewModelScope.launch {
            repository.updateMealEntryQuantity(entryId, quantity)
        }
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            repository.deleteMealEntry(entryId)
        }
    }

    companion object {
        fun factory(selectedDate: LocalDate): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = this.appContainer()
                MealsViewModel(container.repository, selectedDate)
            }
        }
    }
}

@Composable
fun MealsRoute(
    selectedDate: LocalDate,
    onBack: () -> Unit,
    onMealSelected: (MealType) -> Unit
) {
    val viewModel: MealsViewModel = viewModel(
        key = "meals-$selectedDate",
        factory = MealsViewModel.factory(selectedDate)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MealsScreen(
        uiState = uiState,
        onBack = onBack,
        onMealSelected = onMealSelected,
        onSaveMealTargets = viewModel::saveMealTargets,
        onUpdateEntryQuantity = viewModel::updateEntryQuantity,
        onDeleteEntry = viewModel::deleteEntry
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealsScreen(
    uiState: MealsUiState,
    onBack: () -> Unit,
    onMealSelected: (MealType) -> Unit,
    onSaveMealTargets: (Map<MealType, Double?>) -> Unit,
    onUpdateEntryQuantity: (Long, Double) -> Unit,
    onDeleteEntry: (Long) -> Unit
) {
    var showPlannerDialog by rememberSaveable { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<MealEntryEntity?>(null) }
    var deletingEntry by remember { mutableStateOf<MealEntryEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Meals • ${uiState.selectedDate.format(mealDateFormatter)}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showPlannerDialog = true }) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = "Target planner")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = uiState.mealCards, key = { it.mealType.key }) { mealCard ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMealSelected(mealCard.mealType) }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = mealCard.mealType.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (mealCard.targetCalories != null) {
                                        "${formatDouble(mealCard.totals.calories)} / ${formatDouble(mealCard.targetCalories)} kcal"
                                    } else {
                                        "${formatDouble(mealCard.totals.calories)} kcal"
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "P ${formatDouble(mealCard.totals.protein)} | F ${formatDouble(mealCard.totals.fat)} | C ${formatDouble(mealCard.totals.carbs)} | Fi ${formatDouble(mealCard.totals.fiber)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (mealCard.entries.isEmpty()) {
                            Text(
                                text = "No foods tracked yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            mealCard.entries.forEach { entry ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.foodName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${formatDouble(entry.quantity)} ${entry.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = { editingEntry = entry }) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit tracked item")
                                        }
                                        IconButton(onClick = { deletingEntry = entry }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete tracked item")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPlannerDialog) {
        MealTargetPlannerDialog(
            currentTargets = uiState.mealCards.associate { it.mealType to it.targetCalories },
            dailyCalorieTarget = uiState.dailyCalorieTarget,
            onDismiss = { showPlannerDialog = false },
            onSave = { targets ->
                onSaveMealTargets(targets)
                showPlannerDialog = false
            }
        )
    }

    val entryToEdit = editingEntry
    if (entryToEdit != null) {
        EditTrackedEntryDialog(
            entry = entryToEdit,
            onDismiss = { editingEntry = null },
            onSave = { quantity ->
                onUpdateEntryQuantity(entryToEdit.id, quantity)
                editingEntry = null
            }
        )
    }

    val entryToDelete = deletingEntry
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { deletingEntry = null },
            title = { Text("Delete tracked food") },
            text = {
                Text("Delete ${entryToDelete.foodName} from ${MealType.fromKey(entryToDelete.mealType).label}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteEntry(entryToDelete.id)
                        deletingEntry = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingEntry = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MealTargetPlannerDialog(
    currentTargets: Map<MealType, Double?>,
    dailyCalorieTarget: Double?,
    onDismiss: () -> Unit,
    onSave: (Map<MealType, Double?>) -> Unit
) {
    val targetFields = remember(currentTargets) {
        MealType.ordered.associateWith { meal ->
            mutableStateOf(currentTargets[meal]?.let(::formatDouble).orEmpty())
        }
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val hasInvalidField = MealType.ordered.any { meal ->
        val value = targetFields.getValue(meal).value
        value.isNotBlank() && parseNonNegativeDouble(value) == null
    }

    val liveAllocated = MealType.ordered.sumOf { meal ->
        val value = targetFields.getValue(meal).value
        if (value.isBlank()) 0.0 else parseNonNegativeDouble(value) ?: 0.0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Meal Target Planner") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MealType.ordered.forEach { meal ->
                    OutlinedTextField(
                        value = targetFields.getValue(meal).value,
                        onValueChange = { incoming ->
                            if (incoming.isEmpty() || incoming.matches(Regex("^\\d*\\.?\\d*$"))) {
                                targetFields.getValue(meal).value = incoming
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(meal.label) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true
                    )
                }

                if (dailyCalorieTarget != null) {
                    Text(
                        text = "Allocated: ${formatDouble(liveAllocated)} of ${formatDouble(dailyCalorieTarget)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Daily calorie target is not set.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (errorMessage != null) {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (hasInvalidField) {
                        errorMessage = "All meal targets must be non-negative numbers."
                        return@TextButton
                    }

                    val targets = MealType.ordered.associateWith { meal ->
                        val value = targetFields.getValue(meal).value
                        if (value.isBlank()) null else parseNonNegativeDouble(value)
                    }
                    onSave(targets)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditTrackedEntryDialog(
    entry: MealEntryEntity,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var quantityText by remember(entry.id) { mutableStateOf(formatDouble(entry.quantity)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val parsedQuantity = parseNonNegativeDouble(quantityText)
    val multiplier = if (entry.quantity <= 0.0 || parsedQuantity == null) 0.0 else parsedQuantity / entry.quantity

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit tracked quantity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.foodName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { incoming ->
                        if (incoming.isEmpty() || incoming.matches(Regex("^\\d*\\.?\\d*$"))) {
                            quantityText = incoming
                            errorMessage = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Quantity (${entry.unit})") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true
                )

                Text("Calories: ${formatDouble(entry.consumedCalories * multiplier)}")
                Text("Protein: ${formatDouble(entry.consumedProtein * multiplier)}")
                Text("Fat: ${formatDouble(entry.consumedFat * multiplier)}")
                Text("Carbs: ${formatDouble(entry.consumedCarbs * multiplier)}")
                Text("Fiber: ${formatDouble(entry.consumedFiber * multiplier)}")

                if (errorMessage != null) {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val quantity = parseNonNegativeDouble(quantityText)
                    if (quantity == null) {
                        errorMessage = "Enter a non-negative number."
                        return@TextButton
                    }
                    onSave(quantity)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
