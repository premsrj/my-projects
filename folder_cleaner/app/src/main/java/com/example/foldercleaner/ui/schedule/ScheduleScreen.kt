package com.example.foldercleaner.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.example.foldercleaner.data.ScheduleConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    scheduleConfig: ScheduleConfig,
    onBackClick: () -> Unit,
    onSaveClick: (String, Boolean, Boolean) -> Unit
) {
    var intervalDays by rememberSaveable { mutableStateOf(scheduleConfig.intervalDays.toString()) }
    var requiresCharging by rememberSaveable { mutableStateOf(scheduleConfig.requiresCharging) }
    var requiresDeviceIdle by rememberSaveable { mutableStateOf(scheduleConfig.requiresDeviceIdle) }

    LaunchedEffect(scheduleConfig) {
        intervalDays = scheduleConfig.intervalDays.toString()
        requiresCharging = scheduleConfig.requiresCharging
        requiresDeviceIdle = scheduleConfig.requiresDeviceIdle
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule controls") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Control when background cleanup runs.")

            OutlinedTextField(
                value = intervalDays,
                onValueChange = { value ->
                    if (value.all { it.isDigit() }) {
                        intervalDays = value
                    }
                },
                label = { Text("Run every (days)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            SettingSwitchRow(
                title = "Require charging",
                checked = requiresCharging,
                onCheckedChange = { requiresCharging = it }
            )

            SettingSwitchRow(
                title = "Require device idle",
                checked = requiresDeviceIdle,
                onCheckedChange = { requiresDeviceIdle = it }
            )

            Button(
                onClick = { onSaveClick(intervalDays, requiresCharging, requiresDeviceIdle) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save schedule")
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
