package com.example.foldercleaner.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foldercleaner.data.CleanupRepository
import com.example.foldercleaner.data.ScheduleConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScheduleViewModel(
    private val repository: CleanupRepository
) : ViewModel() {
    val scheduleConfig: StateFlow<ScheduleConfig> = repository.scheduleConfigFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = ScheduleConfig()
    )

    private val messageFlow = MutableSharedFlow<String>()
    val messages = messageFlow.asSharedFlow()

    fun saveSchedule(
        intervalDaysRaw: String,
        requiresCharging: Boolean,
        requiresDeviceIdle: Boolean
    ) {
        val intervalDays = intervalDaysRaw.toIntOrNull()?.coerceAtLeast(1) ?: 1
        viewModelScope.launch {
            repository.updateScheduleConfig(
                ScheduleConfig(
                    intervalDays = intervalDays,
                    requiresCharging = requiresCharging,
                    requiresDeviceIdle = requiresDeviceIdle
                )
            )
            messageFlow.emit("Schedule updated")
        }
    }
}

class ScheduleViewModelFactory(
    private val repository: CleanupRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ScheduleViewModel(repository) as T
    }
}
