package com.example.foldercleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.foldercleaner.ui.navigation.FolderCleanerRoot
import com.example.foldercleaner.ui.theme.FolderCleanerTheme
import com.example.foldercleaner.work.WorkScheduler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        val app = application as FolderCleanerApp
        lifecycleScope.launch {
            val scheduleConfig = app.appContainer.cleanupRepository.getScheduleConfig()
            WorkScheduler.schedulePeriodicCleanup(this@MainActivity, scheduleConfig)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as FolderCleanerApp

        setContent {
            FolderCleanerTheme {
                FolderCleanerRoot(appContainer = app.appContainer)
            }
        }
    }
}
