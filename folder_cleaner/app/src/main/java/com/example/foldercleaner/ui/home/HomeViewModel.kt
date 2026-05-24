package com.example.foldercleaner.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foldercleaner.data.CleanupRepository
import com.example.foldercleaner.data.CleanupTrigger
import com.example.foldercleaner.data.SettingsStore
import com.example.foldercleaner.util.CleanupNotifier
import com.example.foldercleaner.util.SizeFormatter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: CleanupRepository,
    application: Application
) : AndroidViewModel(application) {

    val daysToKeep: StateFlow<Int> = repository.daysToKeepFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = SettingsStore.DEFAULT_DAYS_TO_KEEP
    )

    private val messageFlow = MutableSharedFlow<String>()
    val messages = messageFlow.asSharedFlow()

    fun saveDaysToKeep(rawValue: String) {
        val parsed = rawValue.toIntOrNull() ?: return
        viewModelScope.launch {
            repository.updateDaysToKeep(parsed)
        }
    }

    fun runCleanupNow() {
        viewModelScope.launch {
            val summary = repository.performCleanup(CleanupTrigger.Manual)
            CleanupNotifier.showCleanupResult(getApplication(), summary, CleanupTrigger.Manual)
            messageFlow.emit(
                "Cleanup done: deleted ${summary.deletedCount}, reclaimed ${SizeFormatter.formatBytes(summary.reclaimedBytes)}"
            )
        }
    }
}

class HomeViewModelFactory(
    private val repository: CleanupRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(repository, application) as T
    }
}
