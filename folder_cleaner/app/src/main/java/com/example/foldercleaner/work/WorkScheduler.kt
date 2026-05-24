package com.example.foldercleaner.work

import android.content.Context
import com.example.foldercleaner.data.ScheduleConfig
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val PERIODIC_CLEANUP_WORK_NAME = "periodic_cleanup"

    fun schedulePeriodicCleanup(
        context: Context,
        config: ScheduleConfig = ScheduleConfig()
    ) {
        val constraints = Constraints.Builder()
            .setRequiresCharging(config.requiresCharging)
            .setRequiresDeviceIdle(config.requiresDeviceIdle)
            .build()

        val intervalDays = config.intervalDays.coerceAtLeast(1)
        val request = PeriodicWorkRequestBuilder<CleanupWorker>(intervalDays.toLong(), TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_CLEANUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
