package dev.prem.foodtracker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.prem.foodtracker.FoodTrackerApp
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class DailyCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? FoodTrackerApp ?: return Result.failure()
        return try {
            // Keep only the latest 30 days including today.
            val cutoffEpochDay = LocalDate.now().minusDays(29).toEpochDay()
            app.appContainer.repository.cleanupDataOlderThan(cutoffEpochDay)
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "daily-tracked-meals-cleanup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyCleanupWorker>(1, TimeUnit.DAYS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
