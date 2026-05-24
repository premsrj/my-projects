package com.example.foldercleaner.ui.extensions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foldercleaner.data.CleanupRepository
import com.example.foldercleaner.data.IgnoredExtensionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExtensionsViewModel(
    private val repository: CleanupRepository
) : ViewModel() {

    val extensions: StateFlow<List<IgnoredExtensionEntity>> = repository.ignoredExtensionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = emptyList()
    )

    fun addExtension(rawValue: String) {
        viewModelScope.launch {
            repository.addIgnoredExtension(rawValue)
        }
    }

    fun removeExtension(extension: String) {
        viewModelScope.launch {
            repository.removeIgnoredExtension(extension)
        }
    }
}

class ExtensionsViewModelFactory(
    private val repository: CleanupRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ExtensionsViewModel(repository) as T
    }
}
