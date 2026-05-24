package com.example.foldercleaner.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.foldercleaner.FolderCleanerApp
import com.example.foldercleaner.data.CleanupTrigger
import com.example.foldercleaner.util.CleanupNotifier

class CleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as FolderCleanerApp
            val summary = app.appContainer.cleanupRepository.performCleanup(CleanupTrigger.Automatic)
            CleanupNotifier.showCleanupResult(applicationContext, summary, CleanupTrigger.Automatic)
            Result.success(
                workDataOf(
                    KEY_SCANNED_COUNT to summary.scannedCount,
                    KEY_DELETED_COUNT to summary.deletedCount,
                    KEY_SKIPPED_COUNT to summary.skippedCount,
                    KEY_FAILED_COUNT to summary.failedCount,
                    KEY_RECLAIMED_BYTES to summary.reclaimedBytes
                )
            )
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_SCANNED_COUNT = "scanned_count"
        const val KEY_DELETED_COUNT = "deleted_count"
        const val KEY_SKIPPED_COUNT = "skipped_count"
        const val KEY_FAILED_COUNT = "failed_count"
        const val KEY_RECLAIMED_BYTES = "reclaimed_bytes"
    }
}
