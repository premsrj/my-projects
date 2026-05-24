package com.example.foldercleaner.data

data class ScheduleConfig(
    val intervalDays: Int = DEFAULT_INTERVAL_DAYS,
    val requiresCharging: Boolean = DEFAULT_REQUIRES_CHARGING,
    val requiresDeviceIdle: Boolean = DEFAULT_REQUIRES_DEVICE_IDLE
) {
    companion object {
        const val DEFAULT_INTERVAL_DAYS = 1
        const val DEFAULT_REQUIRES_CHARGING = true
        const val DEFAULT_REQUIRES_DEVICE_IDLE = true
    }
}
