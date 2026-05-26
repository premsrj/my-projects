package com.example.foldercleaner.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.foldercleaner.FolderCleanerApp
import com.example.foldercleaner.data.CleanupTrigger
import com.example.foldercleaner.util.CleanupNotifier
import java.util.concurrent.TimeUnit

class CleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as FolderCleanerApp
            val minGapMillis = inputData.getLong(KEY_MIN_GAP_MILLIS, DEFAULT_MIN_GAP_MILLIS)
            val summary = app.appContainer.cleanupRepository.performAutomaticCleanupIfDue(minGapMillis)

            if (summary == null) {
                return Result.success(
                    workDataOf(
                        KEY_SKIPPED_DUE_TO_RECENT_RUN to true
                    )
                )
            }

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
        const val KEY_MIN_GAP_MILLIS = "min_gap_millis"
        const val KEY_SKIPPED_DUE_TO_RECENT_RUN = "skipped_due_to_recent_run"

        private val DEFAULT_MIN_GAP_MILLIS = TimeUnit.HOURS.toMillis(6)
    }
}
