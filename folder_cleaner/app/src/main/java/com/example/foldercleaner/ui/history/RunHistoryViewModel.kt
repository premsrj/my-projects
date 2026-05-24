package com.example.foldercleaner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foldercleaner.data.CleanupRepository
import com.example.foldercleaner.data.CleanupRunEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class RunHistoryViewModel(
    repository: CleanupRepository
) : ViewModel() {
    val runs: StateFlow<List<CleanupRunEntity>> = repository.cleanupRunsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = emptyList()
    )
}

class RunHistoryViewModelFactory(
    private val repository: CleanupRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RunHistoryViewModel(repository) as T
    }
}
