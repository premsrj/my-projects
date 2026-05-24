package com.example.foldercleaner.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "cleanup_settings")

class SettingsStore(private val context: Context) {
    val daysToKeepFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DAYS_TO_KEEP_KEY] ?: DEFAULT_DAYS_TO_KEEP
    }

    val scheduleConfigFlow: Flow<ScheduleConfig> = context.dataStore.data.map { preferences ->
        ScheduleConfig(
            intervalDays = (preferences[SCHEDULE_INTERVAL_DAYS_KEY] ?: ScheduleConfig.DEFAULT_INTERVAL_DAYS)
                .coerceAtLeast(1),
            requiresCharging =
                preferences[SCHEDULE_REQUIRES_CHARGING_KEY] ?: ScheduleConfig.DEFAULT_REQUIRES_CHARGING,
            requiresDeviceIdle =
                preferences[SCHEDULE_REQUIRES_IDLE_KEY] ?: ScheduleConfig.DEFAULT_REQUIRES_DEVICE_IDLE
        )
    }

    suspend fun getDaysToKeep(): Int {
        return daysToKeepFlow.first()
    }

    suspend fun updateDaysToKeep(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[DAYS_TO_KEEP_KEY] = days.coerceAtLeast(0)
        }
    }

    suspend fun getScheduleConfig(): ScheduleConfig {
        return scheduleConfigFlow.first()
    }

    suspend fun updateScheduleConfig(config: ScheduleConfig) {
        context.dataStore.edit { preferences ->
            preferences[SCHEDULE_INTERVAL_DAYS_KEY] = config.intervalDays.coerceAtLeast(1)
            preferences[SCHEDULE_REQUIRES_CHARGING_KEY] = config.requiresCharging
            preferences[SCHEDULE_REQUIRES_IDLE_KEY] = config.requiresDeviceIdle
        }
    }

    companion object {
        private val DAYS_TO_KEEP_KEY = intPreferencesKey("days_to_keep")
        private val SCHEDULE_INTERVAL_DAYS_KEY = intPreferencesKey("schedule_interval_days")
        private val SCHEDULE_REQUIRES_CHARGING_KEY =
            booleanPreferencesKey("schedule_requires_charging")
        private val SCHEDULE_REQUIRES_IDLE_KEY = booleanPreferencesKey("schedule_requires_idle")
        const val DEFAULT_DAYS_TO_KEEP: Int = 30
    }
}
