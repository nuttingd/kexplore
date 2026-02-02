package dev.nutting.kexplore

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.nutting.kexplore.data.cache.ResourceCache
import dev.nutting.kexplore.data.connection.ConnectionStore
import dev.nutting.kexplore.widget.WidgetRefreshWorker
import java.util.concurrent.TimeUnit

class KexploreApp : Application() {

    lateinit var connectionStore: ConnectionStore
        private set

    lateinit var resourceCache: ResourceCache
        private set

    override fun onCreate() {
        super.onCreate()
        connectionStore = ConnectionStore(this)
        resourceCache = ResourceCache(this)
        scheduleWidgetRefresh()
    }

    private fun scheduleWidgetRefresh() {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            15, TimeUnit.MINUTES,
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WidgetRefreshWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
