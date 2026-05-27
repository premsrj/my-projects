package com.example.workouttracker.ui.picker

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.data.CategoryEntity
import com.example.workouttracker.data.ExerciseType
import kotlinx.coroutines.launch

@Composable
fun ExercisePickerRoute(
    onBack: () -> Unit,
    onExerciseSelected: (Long) -> Unit,
    onSupersetSelected: (List<Long>) -> Unit
) {
    val viewModel: ExercisePickerViewModel = viewModel(factory = ExercisePickerViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    ExercisePickerScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onAddCategory = viewModel::addCategory,
        onExerciseSelected = onExerciseSelected,
        onSupersetSelected = onSupersetSelected,
        onAddExercise = { name, description, type, categoryId ->
            viewModel.addExercise(
                name = name,
                description = description,
                type = type,
                categoryId = categoryId,
                onSuccess = onExerciseSelected
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(
    uiState: ExercisePickerUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAddCategory: (String) -> Unit,
    onAddExercise: (String, String, ExerciseType, Long) -> Unit,
    onExerciseSelected: (Long) -> Unit,
    onSupersetSelected: (List<Long>) -> Unit
) {
    var showCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showExerciseDialog by rememberSaveable { mutableStateOf(false) }
    var isSupersetMode by rememberSaveable { mutableStateOf(false) }
    val selectedSupersetOrder = remember { mutableStateListOf<Long>() }
    val coroutineScope = rememberCoroutineScope()

    val exerciseNameById = remember(uiState.groups) {
        uiState.groups
            .flatMap { it.exercises }
            .associate { exercise -> exercise.id to exercise.name }
    }

    fun toggleSupersetSelection(exerciseId: Long) {
        if (selectedSupersetOrder.contains(exerciseId)) {
            selectedSupersetOrder.remove(exerciseId)
        } else {
            selectedSupersetOrder.add(exerciseId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Track Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCategoryDialog = true }) {
                        Icon(imageVector = Icons.Default.Create, contentDescription = "Add category")
                    }
                    IconButton(onClick = { showExerciseDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add exercise")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                label = { Text(text = "Search exercises") }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        isSupersetMode = !isSupersetMode
                        if (!isSupersetMode) {
                            selectedSupersetOrder.clear()
                        }
                    }
                ) {
                    Text(
                        text = if (isSupersetMode) {
                            "Superset mode: On"
                        } else {
                            "Superset mode: Off"
                        }
                    )
                }

                if (isSupersetMode) {
                    TextButton(
                        onClick = {
                            if (selectedSupersetOrder.size < 2) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Select at least 2 exercises")
                                }
                            } else {
                                val orderedIds = selectedSupersetOrder.toList()
                                isSupersetMode = false
                                selectedSupersetOrder.clear()
                                onSupersetSelected(orderedIds)
                            }
                        }
                    ) {
                        Text(text = "Start superset")
                    }
                }
            }

            if (isSupersetMode && selectedSupersetOrder.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Superset order",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        selectedSupersetOrder.forEachIndexed { index, exerciseId ->
                            val exerciseName = exerciseNameById[exerciseId] ?: "Exercise $exerciseId"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "${index + 1}. $exerciseName")
                                Row {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val value = selectedSupersetOrder.removeAt(index)
                                                selectedSupersetOrder.add(index - 1, value)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move up"
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < selectedSupersetOrder.lastIndex) {
                                                val value = selectedSupersetOrder.removeAt(index)
                                                selectedSupersetOrder.add(index + 1, value)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move down"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.groups, key = { it.category.id }) { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = group.category.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (group.exercises.isEmpty()) {
                                Text(
                                    text = "No exercises yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                group.exercises.forEach { exercise ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isSupersetMode) {
                                                    toggleSupersetSelection(exercise.id)
                                                } else {
                                                    onExerciseSelected(exercise.id)
                                                }
                                            }
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FitnessCenter,
                                            contentDescription = null
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = exercise.name,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = exercise.type.label,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (exercise.description.isNotBlank()) {
                                                Text(
                                                    text = exercise.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (isSupersetMode && selectedSupersetOrder.contains(exercise.id)) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected for superset",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
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

    if (showCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showCategoryDialog = false },
            onSave = {
                onAddCategory(it)
                showCategoryDialog = false
            }
        )
    }

    if (showExerciseDialog) {
        AddExerciseDialog(
            categories = uiState.categories,
            onDismiss = { showExerciseDialog = false },
            onSave = { name, description, type, categoryId ->
                onAddExercise(name, description, type, categoryId)
                showExerciseDialog = false
            }
        )
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add category") },
        text = {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                singleLine = true,
                label = { Text(text = "Category name") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(categoryName) }) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
private fun AddExerciseDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, ExerciseType, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ExerciseType.WEIGHT_REPS) }
    var selectedCategoryId by remember(categories) { mutableStateOf(categories.firstOrNull()?.id ?: 0L) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = "Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(text = "Description") },
                    minLines = 2
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showTypeMenu = true }) {
                        Text(text = "Type: ${selectedType.label}")
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        ExerciseType.entries.forEach { type ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    selectedType = type
                                    showTypeMenu = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val categoryName = categories.firstOrNull { it.id == selectedCategoryId }?.name
                        ?: "No category"
                    TextButton(onClick = { showCategoryMenu = true }) {
                        Text(text = "Category: $categoryName")
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        categories.forEach { category ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedCategoryId > 0L) {
                        onSave(name, description, selectedType, selectedCategoryId)
                    }
                }
            ) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}
