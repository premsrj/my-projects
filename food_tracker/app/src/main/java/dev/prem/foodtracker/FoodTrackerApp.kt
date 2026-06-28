package dev.prem.foodtracker

import android.app.Application
import dev.prem.foodtracker.work.DailyCleanupWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FoodTrackerApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        DailyCleanupWorker.schedule(this)

        appScope.launch {
            appContainer.foodSeeder.seedIfNeeded()
        }
    }
}
