package com.example.foldercleaner.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foldercleaner.data.CleanupRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class BackupViewModel(
    private val repository: CleanupRepository
) : ViewModel() {
    private val messageFlow = MutableSharedFlow<String>()
    val messages = messageFlow.asSharedFlow()

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val success = repository.writeBackupToUri(uri)
            if (success) {
                messageFlow.emit("Backup exported")
            } else {
                messageFlow.emit("Failed to export backup")
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val summary = repository.importBackupFromUri(uri)
                if (summary == null) {
                    messageFlow.emit("Failed to import backup")
                    return@launch
                }
                messageFlow.emit(
                    "Backup imported: ${summary.folderCount} folders, ${summary.extensionCount} file types"
                )
            } catch (_: Throwable) {
                messageFlow.emit("Backup file format is invalid")
            }
        }
    }
}

class BackupViewModelFactory(
    private val repository: CleanupRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BackupViewModel(repository) as T
    }
}
