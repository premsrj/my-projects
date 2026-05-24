package com.example.foldercleaner.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    daysToKeep: Int,
    hasStorageAccess: Boolean,
    hasNotificationPermission: Boolean,
    onDaysToKeepChanged: (String) -> Unit,
    onFoldersClick: () -> Unit,
    onIgnoredTypesClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onRunHistoryClick: () -> Unit,
    onBackupClick: () -> Unit,
    onCleanupClick: () -> Unit,
    onGrantStorageAccessClick: () -> Unit,
    onGrantNotificationPermissionClick: () -> Unit
) {
    var daysInput by rememberSaveable { mutableStateOf(daysToKeep.toString()) }

    LaunchedEffect(daysToKeep) {
        daysInput = daysToKeep.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Folder Cleaner",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Delete old files from selected folders while keeping your excluded file types safe.",
            style = MaterialTheme.typography.bodyLarge
        )

        OutlinedTextField(
            value = daysInput,
            onValueChange = { value ->
                if (value.all { it.isDigit() }) {
                    daysInput = value
                    if (value.isNotBlank()) {
                        onDaysToKeepChanged(value)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Days to keep file") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        if (!hasStorageAccess) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Storage access required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Grant all-files storage access so cleanup can manage files in selected folders.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onGrantStorageAccessClick) {
                        Text("Grant access")
                    }
                }
            }
        }

        if (!hasNotificationPermission) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Notifications recommended",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Enable notifications to see cleanup completion alerts.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onGrantNotificationPermissionClick) {
                        Text("Enable notifications")
                    }
                }
            }
        }

        NavigationOptionRow(
            title = "Folders to clean",
            onClick = onFoldersClick
        )

        NavigationOptionRow(
            title = "File types to ignore",
            onClick = onIgnoredTypesClick
        )

        NavigationOptionRow(
            title = "Run history",
            onClick = onRunHistoryClick
        )

        NavigationOptionRow(
            title = "Schedule controls",
            onClick = onScheduleClick
        )

        NavigationOptionRow(
            title = "Backup and migration",
            onClick = onBackupClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onCleanupClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clean Up")
        }
    }
}

@Composable
private fun NavigationOptionRow(
    title: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null
            )
        }
    }
}
