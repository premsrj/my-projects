package dev.prem.budgetwidget.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dev.prem.budgetwidget.wear.BudgetDailyTileService
import dev.prem.budgetwidget.widget.BudgetDailyWidgetProvider
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BudgetPreferences(context: Context) {
    private val appContext = context.applicationContext

    private val sharedPreferences: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _monthlyLimit = MutableStateFlow(readMonthlyLimit())
    val monthlyLimit: StateFlow<Double> = _monthlyLimit.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_MONTHLY_LIMIT) {
            _monthlyLimit.value = readMonthlyLimit()
        }
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setMonthlyLimit(limit: Double) {
        sharedPreferences.edit()
            .putString(KEY_MONTHLY_LIMIT, limit.toString())
            .remove(KEY_DAILY_LIMIT_EPOCH_DAY)
            .remove(KEY_DAILY_LIMIT_VALUE)
            .remove(KEY_DAILY_LIMIT_MONTHLY_LIMIT)
            .remove(KEY_DAILY_LIMIT_MONTH_SPENT_BEFORE_TODAY)
            .remove(KEY_DAILY_LIMIT_DAYS_LEFT_IN_MONTH)
            .apply()
        _monthlyLimit.value = limit
        BudgetDailyWidgetProvider.updateAllWidgets(appContext)
        BudgetDailyTileService.requestUpdate(appContext)
    }

    fun getMonthlyLimit(): Double = _monthlyLimit.value

    fun getOrCreateDailyLimit(
        todayEpochDay: Long,
        monthlyLimit: Double,
        monthSpentBeforeToday: Double,
        daysLeftInMonth: Int
    ): Double {
        val storedEpochDay = sharedPreferences.getLong(KEY_DAILY_LIMIT_EPOCH_DAY, Long.MIN_VALUE)
        val storedLimit = sharedPreferences.getString(KEY_DAILY_LIMIT_VALUE, null)?.toDoubleOrNull()
        val storedMonthlyLimit = sharedPreferences
            .getString(KEY_DAILY_LIMIT_MONTHLY_LIMIT, null)
            ?.toDoubleOrNull()
        val storedMonthSpentBeforeToday = sharedPreferences
            .getString(KEY_DAILY_LIMIT_MONTH_SPENT_BEFORE_TODAY, null)
            ?.toDoubleOrNull()
        val storedDaysLeft = sharedPreferences.getInt(KEY_DAILY_LIMIT_DAYS_LEFT_IN_MONTH, -1)

        val cacheIsValid = storedEpochDay == todayEpochDay &&
            storedLimit != null &&
            storedMonthlyLimit != null &&
            storedMonthSpentBeforeToday != null &&
            storedDaysLeft == daysLeftInMonth &&
            almostEqual(storedMonthlyLimit, monthlyLimit) &&
            almostEqual(storedMonthSpentBeforeToday, monthSpentBeforeToday)

        if (cacheIsValid) {
            return storedLimit!!
        }

        val monthLeftAtStartOfDay = monthlyLimit - monthSpentBeforeToday
        val safeDaysLeft = daysLeftInMonth.coerceAtLeast(1)
        val dailyLimit = monthLeftAtStartOfDay / safeDaysLeft.toDouble()

        sharedPreferences.edit()
            .putLong(KEY_DAILY_LIMIT_EPOCH_DAY, todayEpochDay)
            .putString(KEY_DAILY_LIMIT_VALUE, dailyLimit.toString())
            .putString(KEY_DAILY_LIMIT_MONTHLY_LIMIT, monthlyLimit.toString())
            .putString(
                KEY_DAILY_LIMIT_MONTH_SPENT_BEFORE_TODAY,
                monthSpentBeforeToday.toString()
            )
            .putInt(KEY_DAILY_LIMIT_DAYS_LEFT_IN_MONTH, safeDaysLeft)
            .apply()

        return dailyLimit
    }

    private fun almostEqual(a: Double, b: Double): Boolean {
        return abs(a - b) < 0.000001
    }

    private fun readMonthlyLimit(): Double {
        return sharedPreferences
            .getString(KEY_MONTHLY_LIMIT, null)
            ?.toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: 0.0
    }

    companion object {
        private const val PREFS_NAME = "budget_preferences"
        private const val KEY_MONTHLY_LIMIT = "monthly_limit"
        private const val KEY_DAILY_LIMIT_EPOCH_DAY = "daily_limit_epoch_day"
        private const val KEY_DAILY_LIMIT_VALUE = "daily_limit_value"
        private const val KEY_DAILY_LIMIT_MONTHLY_LIMIT = "daily_limit_monthly_limit"
        private const val KEY_DAILY_LIMIT_MONTH_SPENT_BEFORE_TODAY =
            "daily_limit_month_spent_before_today"
        private const val KEY_DAILY_LIMIT_DAYS_LEFT_IN_MONTH = "daily_limit_days_left_in_month"
    }
}
