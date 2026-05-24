package com.example.foldercleaner.ui.navigation

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.foldercleaner.AppContainer
import com.example.foldercleaner.ui.backup.BackupScreen
import com.example.foldercleaner.ui.backup.BackupViewModel
import com.example.foldercleaner.ui.backup.BackupViewModelFactory
import com.example.foldercleaner.ui.extensions.ExtensionsScreen
import com.example.foldercleaner.ui.extensions.ExtensionsViewModel
import com.example.foldercleaner.ui.extensions.ExtensionsViewModelFactory
import com.example.foldercleaner.ui.history.RunHistoryScreen
import com.example.foldercleaner.ui.history.RunHistoryViewModel
import com.example.foldercleaner.ui.history.RunHistoryViewModelFactory
import com.example.foldercleaner.ui.folders.FoldersScreen
import com.example.foldercleaner.ui.folders.FoldersViewModel
import com.example.foldercleaner.ui.folders.FoldersViewModelFactory
import com.example.foldercleaner.ui.home.HomeScreen
import com.example.foldercleaner.ui.home.HomeViewModel
import com.example.foldercleaner.ui.home.HomeViewModelFactory
import com.example.foldercleaner.ui.schedule.ScheduleScreen
import com.example.foldercleaner.ui.schedule.ScheduleViewModel
import com.example.foldercleaner.ui.schedule.ScheduleViewModelFactory
import com.example.foldercleaner.util.NotificationPermissionHelper
import com.example.foldercleaner.util.StoragePermissionHelper

@Composable
fun FolderCleanerRoot(appContainer: AppContainer) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val app = LocalContext.current.applicationContext as Application
    var hasNotificationPermission by remember {
        mutableStateOf(NotificationPermissionHelper.hasPermission(app))
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        hasNotificationPermission = NotificationPermissionHelper.hasPermission(app)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.Home.route) {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory(
                        repository = appContainer.cleanupRepository,
                        application = app
                    )
                )
                val daysToKeep by viewModel.daysToKeep.collectAsState()

                LaunchedEffect(viewModel) {
                    viewModel.messages.collect { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                HomeScreen(
                    daysToKeep = daysToKeep,
                    hasStorageAccess = StoragePermissionHelper.hasStorageAccess(app),
                    hasNotificationPermission = hasNotificationPermission,
                    onDaysToKeepChanged = viewModel::saveDaysToKeep,
                    onFoldersClick = { navController.navigate(AppDestination.Folders.route) },
                    onIgnoredTypesClick = { navController.navigate(AppDestination.IgnoredTypes.route) },
                    onScheduleClick = { navController.navigate(AppDestination.Schedule.route) },
                    onRunHistoryClick = { navController.navigate(AppDestination.RunHistory.route) },
                    onBackupClick = { navController.navigate(AppDestination.Backup.route) },
                    onCleanupClick = viewModel::runCleanupNow,
                    onGrantStorageAccessClick = { StoragePermissionHelper.openStorageSettings(app) },
                    onGrantNotificationPermissionClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            hasNotificationPermission = true
                        }
                    }
                )
            }

            composable(AppDestination.Folders.route) {
                val viewModel: FoldersViewModel = viewModel(
                    factory = FoldersViewModelFactory(
                        repository = appContainer.cleanupRepository,
                        application = app
                    )
                )
                val folders by viewModel.folders.collectAsState()

                FoldersScreen(
                    folders = folders,
                    onBackClick = { navController.popBackStack() },
                    onAddFolder = viewModel::addFolder,
                    onToggleFolder = viewModel::setFolderEnabled,
                    onDeleteFolder = viewModel::removeFolder
                )
            }

            composable(AppDestination.IgnoredTypes.route) {
                val viewModel: ExtensionsViewModel = viewModel(
                    factory = ExtensionsViewModelFactory(
                        repository = appContainer.cleanupRepository
                    )
                )
                val ignoredExtensions by viewModel.extensions.collectAsState()

                ExtensionsScreen(
                    extensions = ignoredExtensions,
                    onBackClick = { navController.popBackStack() },
                    onAddExtension = viewModel::addExtension,
                    onDeleteExtension = viewModel::removeExtension
                )
            }

            composable(AppDestination.RunHistory.route) {
                val viewModel: RunHistoryViewModel = viewModel(
                    factory = RunHistoryViewModelFactory(
                        repository = appContainer.cleanupRepository
                    )
                )
                val runs by viewModel.runs.collectAsState()

                RunHistoryScreen(
                    runs = runs,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(AppDestination.Schedule.route) {
                val viewModel: ScheduleViewModel = viewModel(
                    factory = ScheduleViewModelFactory(
                        repository = appContainer.cleanupRepository
                    )
                )
                val scheduleConfig by viewModel.scheduleConfig.collectAsState()

                LaunchedEffect(viewModel) {
                    viewModel.messages.collect { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                ScheduleScreen(
                    scheduleConfig = scheduleConfig,
                    onBackClick = { navController.popBackStack() },
                    onSaveClick = viewModel::saveSchedule
                )
            }

            composable(AppDestination.Backup.route) {
                val viewModel: BackupViewModel = viewModel(
                    factory = BackupViewModelFactory(
                        repository = appContainer.cleanupRepository
                    )
                )

                LaunchedEffect(viewModel) {
                    viewModel.messages.collect { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                BackupScreen(
                    onBackClick = { navController.popBackStack() },
                    onExportUriSelected = viewModel::exportBackup,
                    onImportUriSelected = viewModel::importBackup
                )
            }
        }
    }
}
