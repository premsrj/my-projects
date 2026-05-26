package com.example.foldercleaner.work

import android.content.Context
import com.example.foldercleaner.data.ScheduleConfig
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val PERIODIC_CLEANUP_WORK_NAME = "periodic_cleanup"
    private const val DAILY_SAFETY_CLEANUP_WORK_NAME = "daily_safety_cleanup"
    private const val LAUNCH_CATCHUP_WORK_NAME = "launch_catchup_cleanup"
    private const val DAILY_INTERVAL_DAYS = 1L

    fun scheduleOnAppLaunch(
        context: Context,
        config: ScheduleConfig = ScheduleConfig()
    ) {
        scheduleConfiguredPeriodicCleanup(
            context = context,
            config = config,
            policy = ExistingPeriodicWorkPolicy.KEEP
        )
        scheduleDailySafetyCleanup(context)
        scheduleLaunchCatchupCleanup(context)
    }

    fun schedulePeriodicCleanup(
        context: Context,
        config: ScheduleConfig = ScheduleConfig()
    ) {
        scheduleConfiguredPeriodicCleanup(
            context = context,
            config = config,
            policy = ExistingPeriodicWorkPolicy.UPDATE
        )
        scheduleDailySafetyCleanup(context)
        scheduleLaunchCatchupCleanup(context)
    }

    private fun scheduleConfiguredPeriodicCleanup(
        context: Context,
        config: ScheduleConfig,
        policy: ExistingPeriodicWorkPolicy
    ) {
        val constraints = Constraints.Builder()
            .setRequiresCharging(config.requiresCharging)
            .setRequiresDeviceIdle(config.requiresDeviceIdle)
            .build()

        val intervalDays = config.intervalDays.coerceAtLeast(1)
        val request = PeriodicWorkRequestBuilder<CleanupWorker>(intervalDays.toLong(), TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInputData(
                Data.Builder()
                    .putLong(CleanupWorker.KEY_MIN_GAP_MILLIS, TimeUnit.HOURS.toMillis(6))
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_CLEANUP_WORK_NAME,
            policy,
            request
        )
    }

    private fun scheduleDailySafetyCleanup(context: Context) {
        val request = PeriodicWorkRequestBuilder<CleanupWorker>(DAILY_INTERVAL_DAYS, TimeUnit.DAYS)
            .setInputData(
                Data.Builder()
                    .putLong(CleanupWorker.KEY_MIN_GAP_MILLIS, TimeUnit.HOURS.toMillis(6))
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_SAFETY_CLEANUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun scheduleLaunchCatchupCleanup(context: Context) {
        val request = OneTimeWorkRequestBuilder<CleanupWorker>()
            .setInputData(
                Data.Builder()
                    .putLong(CleanupWorker.KEY_MIN_GAP_MILLIS, TimeUnit.HOURS.toMillis(24))
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            LAUNCH_CATCHUP_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
