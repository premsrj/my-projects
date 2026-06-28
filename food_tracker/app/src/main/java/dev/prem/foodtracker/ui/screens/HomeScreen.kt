package dev.prem.foodtracker.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
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
import dev.prem.foodtracker.data.model.FoodImportResult
import dev.prem.foodtracker.data.db.TargetProfileEntity
import dev.prem.foodtracker.data.model.NutritionTotals
import dev.prem.foodtracker.data.repo.FoodTrackerRepository
import dev.prem.foodtracker.ui.appContainer
import dev.prem.foodtracker.ui.components.DonutChart
import dev.prem.foodtracker.ui.components.ProgressStatus
import dev.prem.foodtracker.ui.components.formatDouble
import dev.prem.foodtracker.ui.components.parseNonNegativeDouble
import dev.prem.foodtracker.ui.components.progressStatusFor
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val weekDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EE")

data class MacroProgress(
    val label: String,
    val consumed: Double,
    val target: Double
)

data class DailyCaloriesPoint(
    val date: LocalDate,
    val calories: Double,
    val hasMeals: Boolean
)

data class HomeUiState(
    val selectedDate: LocalDate,
    val totals: NutritionTotals,
    val targets: TargetProfileEntity,
    val last7DaysCalories: List<DailyCaloriesPoint>,
    val averageCaloriesLast7Days: Double?,
    val averageDaysCount: Int
) {
    val calorieProgress: MacroProgress?
        get() = targets.calorieTarget?.let { target ->
            MacroProgress(label = "Calories", consumed = totals.calories, target = target)
        }

    val minorProgress: List<MacroProgress>
        get() = buildList {
            targets.proteinTarget?.let { add(MacroProgress("Protein", totals.protein, it)) }
            targets.fatTarget?.let { add(MacroProgress("Fat", totals.fat, it)) }
            targets.carbsTarget?.let { add(MacroProgress("Carbs", totals.carbs, it)) }
            targets.fiberTarget?.let { add(MacroProgress("Fiber", totals.fiber, it)) }
        }
}

class HomeViewModel(
    private val repository: FoodTrackerRepository,
    private val selectedDate: LocalDate
) : ViewModel() {
    private val rangeStartEpochDay = selectedDate.minusDays(6).toEpochDay()
    private val rangeEndEpochDay = selectedDate.toEpochDay()

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeTargetProfile(),
        repository.observeDailyTotals(selectedDate.toEpochDay()),
        repository.observeDailyCaloriesRange(rangeStartEpochDay, rangeEndEpochDay)
    ) { targets, totals, dailyCaloriesMap ->
        val weeklyPoints = (6 downTo 0).map { offset ->
            val date = selectedDate.minusDays(offset.toLong())
            val calories = dailyCaloriesMap[date.toEpochDay()]
            DailyCaloriesPoint(
                date = date,
                calories = calories ?: 0.0,
                hasMeals = calories != null
            )
        }

        val activeDays = weeklyPoints.count { it.hasMeals }
        val average = if (activeDays > 0) {
            weeklyPoints.filter { it.hasMeals }.sumOf { it.calories } / activeDays
        } else {
            null
        }

        HomeUiState(
            selectedDate = selectedDate,
            totals = totals,
            targets = targets,
            last7DaysCalories = weeklyPoints,
            averageCaloriesLast7Days = average,
            averageDaysCount = activeDays
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(
            selectedDate = selectedDate,
            totals = NutritionTotals(),
            targets = TargetProfileEntity(calorieTarget = 2000.0),
            last7DaysCalories = (6 downTo 0).map { offset ->
                DailyCaloriesPoint(
                    date = selectedDate.minusDays(offset.toLong()),
                    calories = 0.0,
                    hasMeals = false
                )
            },
            averageCaloriesLast7Days = null,
            averageDaysCount = 0
        )
    )

    fun saveTargets(
        calorie: Double?,
        protein: Double?,
        fat: Double?,
        carbs: Double?,
        fiber: Double?
    ) {
        viewModelScope.launch {
            repository.saveTargetProfile(
                TargetProfileEntity(
                    profileId = 1,
                    calorieTarget = calorie,
                    proteinTarget = protein,
                    fatTarget = fat,
                    carbsTarget = carbs,
                    fiberTarget = fiber
                )
            )
        }
    }

    suspend fun exportFoods(outputStream: OutputStream): Int {
        return repository.exportFoodsToCsv(outputStream)
    }

    suspend fun importFoods(inputStream: InputStream): FoodImportResult {
        return repository.importFoodsFromCsv(inputStream)
    }

    companion object {
        fun factory(selectedDate: LocalDate): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = this.appContainer()
                HomeViewModel(container.repository, selectedDate)
            }
        }
    }
}

@Composable
fun HomeRoute(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onTrackFood: () -> Unit,
    onImportCompleted: (FoodImportResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: HomeViewModel = viewModel(
        key = "home-$selectedDate",
        factory = HomeViewModel.factory(selectedDate)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isFileOperationInProgress by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            isFileOperationInProgress = true
            val exportedCount = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        viewModel.exportFoods(output)
                    }
                }
            }.getOrNull()
            isFileOperationInProgress = false

            if (exportedCount == null) {
                Toast.makeText(context, "Unable to export foods to selected file", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Exported $exportedCount foods", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            isFileOperationInProgress = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        viewModel.importFoods(input)
                    }
                }
            }.getOrNull()
            isFileOperationInProgress = false

            if (result == null) {
                Toast.makeText(context, "Unable to import foods from selected file", Toast.LENGTH_LONG).show()
            } else {
                onImportCompleted(result)
            }
        }
    }

    HomeScreen(
        uiState = uiState,
        onPreviousDate = { onDateSelected(selectedDate.minusDays(1)) },
        onNextDate = { onDateSelected(selectedDate.plusDays(1)) },
        onTrackFood = onTrackFood,
        onSaveTargets = viewModel::saveTargets,
        onExportFoods = {
            exportLauncher.launch("food_tracker_export_${LocalDate.now()}.csv")
        },
        onImportFoods = {
            importLauncher.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel"))
        },
        isFileOperationInProgress = isFileOperationInProgress
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onTrackFood: () -> Unit,
    onSaveTargets: (Double?, Double?, Double?, Double?, Double?) -> Unit,
    onExportFoods: () -> Unit,
    onImportFoods: () -> Unit,
    isFileOperationInProgress: Boolean
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showTargetsDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(text = "Food Tracker") },
                    actions = {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Set targets") },
                                onClick = {
                                    showMenu = false
                                    showTargetsDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export foods to CSV") },
                                onClick = {
                                    showMenu = false
                                    onExportFoods()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import foods from CSV") },
                                onClick = {
                                    showMenu = false
                                    onImportFoods()
                                }
                            )
                        }
                    }
                )
                if (isFileOperationInProgress) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onTrackFood,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Track food")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                DatePickerRow(
                    date = uiState.selectedDate,
                    onPreviousDate = onPreviousDate,
                    onNextDate = onNextDate
                )
            }

            item {
                val calorieProgress = uiState.calorieProgress
                if (calorieProgress != null) {
                    MajorMetricCard(progress = calorieProgress)
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Calorie target is not set",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Open menu > Set targets to configure daily calorie limit.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (uiState.minorProgress.isNotEmpty()) {
                items(items = uiState.minorProgress, key = { it.label }) { metric ->
                    MinorMetricCard(progress = metric)
                }
            }

            item {
                WeeklyCaloriesCard(
                    points = uiState.last7DaysCalories,
                    averageCalories = uiState.averageCaloriesLast7Days,
                    averageDaysCount = uiState.averageDaysCount
                )
            }
        }
    }

    if (showTargetsDialog) {
        TargetsDialog(
            current = uiState.targets,
            onDismiss = { showTargetsDialog = false },
            onSave = { calorie, protein, fat, carbs, fiber ->
                onSaveTargets(calorie, protein, fat, carbs, fiber)
                showTargetsDialog = false
            }
        )
    }
}

@Composable
private fun DatePickerRow(
    date: LocalDate,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousDate) {
                Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "Previous day")
            }

            Text(
                text = date.format(dateFormatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            IconButton(onClick = onNextDate) {
                Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Next day")
            }
        }
    }
}

@Composable
private fun MajorMetricCard(progress: MacroProgress) {
    val ratio = if (progress.target <= 0.0) 0f else (progress.consumed / progress.target).toFloat()
    val status = progressStatusFor(ratio)
    val statusLabel = when (status) {
        ProgressStatus.UNDER -> "Under target"
        ProgressStatus.NEAR -> "Near target"
        ProgressStatus.OVER -> "Over target"
    }
    val statusColor = when (status) {
        ProgressStatus.UNDER -> MaterialTheme.colorScheme.primary
        ProgressStatus.NEAR -> MaterialTheme.colorScheme.tertiary
        ProgressStatus.OVER -> MaterialTheme.colorScheme.error
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, statusColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = progress.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            DonutChart(
                progress = ratio,
                centerText = "${formatDouble(progress.consumed)} / ${formatDouble(progress.target)}",
                modifier = Modifier.size(220.dp),
                thicknessDp = 20f,
                status = status
            )

            Text(
                text = "Consumed ${formatDouble(progress.consumed)} of ${formatDouble(progress.target)}",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }
    }
}

@Composable
private fun MinorMetricCard(progress: MacroProgress) {
    val ratio = if (progress.target <= 0.0) 0f else (progress.consumed / progress.target).toFloat()
    val status = progressStatusFor(ratio)
    val statusColor = when (status) {
        ProgressStatus.UNDER -> MaterialTheme.colorScheme.primary
        ProgressStatus.NEAR -> MaterialTheme.colorScheme.tertiary
        ProgressStatus.OVER -> MaterialTheme.colorScheme.error
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DonutChart(
                progress = ratio,
                centerText = "${(ratio * 100).toInt()}%",
                modifier = Modifier.size(120.dp),
                status = status
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = progress.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = "${formatDouble(progress.consumed)} / ${formatDouble(progress.target)}")
                Text(
                    text = when (status) {
                        ProgressStatus.UNDER -> "Under target"
                        ProgressStatus.NEAR -> "Near target"
                        ProgressStatus.OVER -> "Over target"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun WeeklyCaloriesCard(
    points: List<DailyCaloriesPoint>,
    averageCalories: Double?,
    averageDaysCount: Int
) {
    val maxCalories = (points.maxOfOrNull { it.calories } ?: 0.0).coerceAtLeast(1.0)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "7-Day Calories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = if (averageCalories != null) {
                    "Average (days with meals): ${formatDouble(averageCalories)} kcal across $averageDaysCount day(s)"
                } else {
                    "Average (days with meals): No tracked meals in this period"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEach { point ->
                    val ratio = (point.calories / maxCalories).coerceIn(0.0, 1.0)
                    val barHeight = if (point.hasMeals) {
                        (ratio * 88.0).coerceAtLeast(8.0)
                    } else {
                        3.0
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(94.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height(barHeight.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (point.hasMeals) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                            )
                        }

                        Text(
                            text = point.date.format(weekDayFormatter),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetsDialog(
    current: TargetProfileEntity,
    onDismiss: () -> Unit,
    onSave: (Double?, Double?, Double?, Double?, Double?) -> Unit
) {
    var caloriesText by remember { mutableStateOf(current.calorieTarget?.let(::formatDouble).orEmpty()) }
    var proteinText by remember { mutableStateOf(current.proteinTarget?.let(::formatDouble).orEmpty()) }
    var fatText by remember { mutableStateOf(current.fatTarget?.let(::formatDouble).orEmpty()) }
    var carbsText by remember { mutableStateOf(current.carbsTarget?.let(::formatDouble).orEmpty()) }
    var fiberText by remember { mutableStateOf(current.fiberTarget?.let(::formatDouble).orEmpty()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily Targets") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TargetInputField(label = "Calories", value = caloriesText, onValueChange = { caloriesText = it })
                TargetInputField(label = "Protein", value = proteinText, onValueChange = { proteinText = it })
                TargetInputField(label = "Fat", value = fatText, onValueChange = { fatText = it })
                TargetInputField(label = "Carbs", value = carbsText, onValueChange = { carbsText = it })
                TargetInputField(label = "Fiber", value = fiberText, onValueChange = { fiberText = it })

                if (errorMessage != null) {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = listOf(caloriesText, proteinText, fatText, carbsText, fiberText).map { raw ->
                        if (raw.isBlank()) {
                            Result.success<Double?>(null)
                        } else {
                            parseNonNegativeDouble(raw)?.let { Result.success<Double?>(it) }
                                ?: Result.failure(IllegalArgumentException("invalid"))
                        }
                    }

                    if (parsed.any { it.isFailure }) {
                        errorMessage = "Targets must be non-negative numbers. Leave blank to disable a target."
                        return@TextButton
                    }

                    onSave(
                        parsed[0].getOrNull(),
                        parsed[1].getOrNull(),
                        parsed[2].getOrNull(),
                        parsed[3].getOrNull(),
                        parsed[4].getOrNull()
                    )
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
private fun TargetInputField(
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
