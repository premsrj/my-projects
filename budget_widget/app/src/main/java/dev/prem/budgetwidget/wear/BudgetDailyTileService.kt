package dev.prem.budgetwidget.wear

import android.content.Context
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.prem.budgetwidget.AppContainer
import java.text.NumberFormat
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class BudgetDailyTileService : TileService() {

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        val tile = buildTile()
        return Futures.immediateFuture(tile)
    }

    override fun onResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> {
        val resources = ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
        return Futures.immediateFuture(resources)
    }

    private fun buildTile(): TileBuilders.Tile {
        val appContainer = AppContainer(applicationContext)
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

        val currency = NumberFormat.getCurrencyInstance()
        val lineOne = "${currency.format(todayLeft)} Left"
        val lineTwo = "${currency.format(todayLimit)} Limit"

        val lineOneText = LayoutElementBuilders.Text.Builder()
            .setText(lineOne)
            .setMaxLines(1)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(
                        DimensionBuilders.SpProp.Builder().setValue(18f).build()
                    )
                    .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                    .setColor(
                        ColorBuilders.ColorProp.Builder().setArgb(0xFFFFFFFF.toInt()).build()
                    )
                    .build()
            )
            .build()

        val lineTwoText = LayoutElementBuilders.Text.Builder()
            .setText(lineTwo)
            .setMaxLines(1)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setTop(DimensionBuilders.DpProp.Builder().setValue(4f).build())
                            .build()
                    )
                    .build()
            )
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(
                        DimensionBuilders.SpProp.Builder().setValue(14f).build()
                    )
                    .setColor(
                        ColorBuilders.ColorProp.Builder().setArgb(0xFFFFFFFF.toInt()).build()
                    )
                    .build()
            )
            .build()

        val root = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setWidth(DimensionBuilders.ExpandedDimensionProp.Builder().build())
            .setHeight(DimensionBuilders.ExpandedDimensionProp.Builder().build())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setStart(DimensionBuilders.DpProp.Builder().setValue(12f).build())
                            .setEnd(DimensionBuilders.DpProp.Builder().setValue(12f).build())
                            .setTop(DimensionBuilders.DpProp.Builder().setValue(10f).build())
                            .setBottom(DimensionBuilders.DpProp.Builder().setValue(10f).build())
                            .build()
                    )
                    .build()
            )
            .addContent(lineOneText)
            .addContent(lineTwoText)
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(root)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    companion object {
        private const val RESOURCES_VERSION = "1"

        fun requestUpdate(context: Context) {
            getUpdater(context).requestUpdate(BudgetDailyTileService::class.java)
        }
    }
}
