package com.example.foldercleaner.ui.folders

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foldercleaner.data.CleanupRepository
import com.example.foldercleaner.data.SelectedFolderEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FoldersViewModel(
    private val repository: CleanupRepository,
    application: Application
) : AndroidViewModel(application) {

    val folders: StateFlow<List<SelectedFolderEntity>> = repository.selectedFoldersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = emptyList()
    )

    fun addFolder(uri: Uri) {
        val contentResolver = getApplication<Application>().contentResolver
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Ignore if permission was already persisted or cannot be persisted.
        }

        val displayName = DocumentFile.fromTreeUri(getApplication(), uri)?.name ?: uri.toString()

        viewModelScope.launch {
            repository.addFolder(uri.toString(), displayName)
        }
    }

    fun removeFolder(uri: String) {
        viewModelScope.launch {
            repository.removeFolder(uri)
        }
    }

    fun setFolderEnabled(uri: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.setFolderEnabled(uri, isEnabled)
        }
    }
}

class FoldersViewModelFactory(
    private val repository: CleanupRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return FoldersViewModel(repository, application) as T
    }
}
