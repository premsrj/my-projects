package dev.prem.budgetwidget.ui.addedit

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AddEditExpenseRoute(
    viewModel: AddEditExpenseViewModel,
    onDone: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AddEditExpenseEvent.Saved,
                AddEditExpenseEvent.Deleted -> onDone()
                is AddEditExpenseEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    AddEditExpenseScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onDateChanged = viewModel::onDateChanged,
        onAmountChanged = viewModel::onAmountChanged,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onSave = viewModel::saveExpense,
        onDelete = viewModel::deleteExpense,
        onBack = onDone
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditExpenseScreen(
    uiState: AddEditExpenseUiState,
    snackbarHostState: SnackbarHostState,
    onDateChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val hasValidDate = runCatching {
        LocalDate.parse(uiState.dateInput, DateTimeFormatter.ISO_LOCAL_DATE)
    }.isSuccess
    val hasValidAmount = uiState.amountInput.trim().toDoubleOrNull()?.let { it > 0.0 } == true
    val hasDescription = uiState.descriptionInput.trim().isNotEmpty()
    val canSave = hasValidDate && hasValidAmount && hasDescription && !uiState.isSaving

    val openSystemDatePicker: () -> Unit = {
        val initialDate = runCatching {
            LocalDate.parse(uiState.dateInput, DateTimeFormatter.ISO_LOCAL_DATE)
        }.getOrDefault(LocalDate.now())

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE)
                onDateChanged(selectedDate)
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isExistingExpense) "Edit expense" else "Add expense")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.isExistingExpense) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete expense"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Enter the transaction details",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = uiState.dateInput,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openSystemDatePicker() },
                label = { Text("Date") },
                placeholder = { Text("Tap to select a date") },
                singleLine = true,
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = openSystemDatePicker) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = "Select date"
                        )
                    }
                }
            )

            OutlinedTextField(
                value = uiState.amountInput,
                onValueChange = onAmountChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Amount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            OutlinedTextField(
                value = uiState.descriptionInput,
                onValueChange = onDescriptionChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description") }
            )

            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(if (uiState.isSaving) "Saving..." else "Save")
            }
        }
    }
}
