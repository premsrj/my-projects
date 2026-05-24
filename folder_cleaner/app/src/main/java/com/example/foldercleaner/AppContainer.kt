package com.example.foldercleaner

import android.content.Context
import com.example.foldercleaner.data.AppDatabase
import com.example.foldercleaner.data.CleanupRepository
import com.example.foldercleaner.data.SettingsStore

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy {
        AppDatabase.create(appContext)
    }

    private val settingsStore: SettingsStore by lazy {
        SettingsStore(appContext)
    }

    val cleanupRepository: CleanupRepository by lazy {
        CleanupRepository(
            appContext = appContext,
            folderDao = database.selectedFolderDao(),
            ignoredExtensionDao = database.ignoredExtensionDao(),
            cleanupRunDao = database.cleanupRunDao(),
            settingsStore = settingsStore
        )
    }
}
