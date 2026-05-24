package dev.prem.budgetwidget.ui.main

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.prem.budgetwidget.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: DashboardUiState,
    onSetMonthlyLimit: (String) -> Boolean,
    onExpenseClick: (Long) -> Unit,
    onAddExpenseClick: () -> Unit
) {
    var showLimitDialog by rememberSaveable { mutableStateOf(false) }
    var limitInput by rememberSaveable { mutableStateOf("") }
    var limitHasError by rememberSaveable { mutableStateOf(false) }

    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text(text = "Set monthly limit") },
            text = {
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = {
                        limitInput = it
                        limitHasError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = limitHasError,
                    label = { Text("Monthly limit") },
                    supportingText = {
                        if (limitHasError) {
                            Text("Enter a valid non-negative number")
                        }
                    },
                    keyboardOptions = KeyboardOptions.Default
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val saved = onSetMonthlyLimit(limitInput)
                    if (saved) {
                        showLimitDialog = false
                    } else {
                        limitHasError = true
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Daily Budget Tracker") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpenseClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add expense")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Today", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${Formatters.formatCurrency(uiState.todayLeftToSpend)} left today (${Formatters.formatCurrency(uiState.todaySpent)} spent)",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.todayLeftToSpend < 0.0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "${Formatters.formatCurrency(uiState.todayLimit)} today's limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Month",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                limitInput = uiState.monthLimit.toString()
                                limitHasError = false
                                showLimitDialog = true
                            }
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit monthly limit")
                        }
                    }

                    MonthValueRow(label = "Limit", value = Formatters.formatCurrency(uiState.monthLimit))
                    MonthValueRow(label = "Days Left", value = uiState.daysLeftInMonth.toString())
                    MonthValueRow(
                        label = "Left",
                        value = Formatters.formatCurrency(uiState.monthLeft),
                        emphasizeNegative = uiState.monthLeft < 0.0
                    )
                    MonthValueRow(label = "Spent", value = Formatters.formatCurrency(uiState.monthSpent))
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(text = "Expenses", style = MaterialTheme.typography.titleMedium)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    ExpenseHeaderRow()

                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))

                    if (uiState.expenses.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No expenses yet for this month",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Tap + to add one",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(uiState.expenses, key = { it.id }) { expense ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onExpenseClick(expense.id) }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = Formatters.formatShortDate(expense.date),
                                        modifier = Modifier.weight(0.27f),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = Formatters.formatCurrency(expense.amount),
                                        modifier = Modifier.weight(0.28f),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.End,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = expense.description,
                                        modifier = Modifier.weight(0.45f),
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthValueRow(
    label: String,
    value: String,
    emphasizeNegative: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (emphasizeNegative) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun ExpenseHeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Date",
            modifier = Modifier.weight(0.27f),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Amount",
            modifier = Modifier.weight(0.28f),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Description",
            modifier = Modifier.weight(0.45f),
            fontWeight = FontWeight.SemiBold
        )
    }
}
