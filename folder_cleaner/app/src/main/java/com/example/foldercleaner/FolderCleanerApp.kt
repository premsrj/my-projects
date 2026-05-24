package com.example.foldercleaner

import android.app.Application

class FolderCleanerApp : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }
}
