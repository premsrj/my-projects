package dev.prem.budgetwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.prem.budgetwidget.AppContainer
import dev.prem.budgetwidget.MainActivity
import dev.prem.budgetwidget.R
import java.text.NumberFormat
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class BudgetDailyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH_WIDGET,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "dev.prem.budgetwidget.action.REFRESH_WIDGET"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, BudgetDailyWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
            if (appWidgetIds.isEmpty()) return
            updateWidgets(context, appWidgetManager, appWidgetIds)
        }

        private fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val appContext = context.applicationContext
            val appContainer = AppContainer(appContext)
            val repository = appContainer.expenseRepository
            val preferences = appContainer.budgetPreferences

            val today = LocalDate.now()
            val todayEpochDay = today.toEpochDay()
            val daysLeftInMonth = (today.lengthOfMonth() - today.dayOfMonth + 1).coerceAtLeast(1)
            val monthlyLimit = preferences.getMonthlyLimit()

            val (todaySpent, monthSpentBeforeToday) = runBlocking {
                withContext(Dispatchers.IO) {
                    val spentToday = repository.getSpentOnDay(today)
                    val spentBeforeToday = repository.getMonthSpentBeforeDay(today)
                    spentToday to spentBeforeToday
                }
            }

            val todayLimit = preferences.getOrCreateDailyLimit(
                todayEpochDay = todayEpochDay,
                monthlyLimit = monthlyLimit,
                monthSpentBeforeToday = monthSpentBeforeToday,
                daysLeftInMonth = daysLeftInMonth
            )
            val todayLeft = todayLimit - todaySpent

            val formatter = NumberFormat.getCurrencyInstance()
            val lineOne = "${formatter.format(todayLeft)} Left"
            val lineTwo = "${formatter.format(todayLimit)} Limit"

            val launchIntent = Intent(appContext, MainActivity::class.java)
            val launchPendingIntent = PendingIntent.getActivity(
                appContext,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            appWidgetIds.forEach { widgetId ->
                val views = RemoteViews(appContext.packageName, R.layout.widget_daily_budget)
                views.setTextViewText(R.id.widgetLineOne, lineOne)
                views.setTextViewText(R.id.widgetLineTwo, lineTwo)
                views.setOnClickPendingIntent(R.id.widgetRoot, launchPendingIntent)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
