package dev.nutting.kexplore

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.nutting.kexplore.data.cache.ResourceCache
import dev.nutting.kexplore.data.connection.ConnectionStore
import dev.nutting.kexplore.data.notification.ClusterMonitorWorker
import dev.nutting.kexplore.data.notification.MonitoringPreferences
import dev.nutting.kexplore.data.notification.NotificationHelper
import dev.nutting.kexplore.data.portforward.PortForwardManager
import dev.nutting.kexplore.data.preferences.DisplayPreferences
import dev.nutting.kexplore.widget.WidgetRefreshWorker
import com.airbnb.android.showkase.annotation.ShowkaseRoot
import com.airbnb.android.showkase.annotation.ShowkaseRootModule
import java.util.concurrent.TimeUnit

@ShowkaseRoot
class KexploreShowkaseRoot : ShowkaseRootModule

class KexploreApp : Application() {

    lateinit var connectionStore: ConnectionStore
        private set

    lateinit var resourceCache: ResourceCache
        private set

    lateinit var monitoringPreferences: MonitoringPreferences
        private set

    lateinit var displayPreferences: DisplayPreferences
        private set

    lateinit var portForwardManager: PortForwardManager
        private set

    override fun onCreate() {
        super.onCreate()
        connectionStore = ConnectionStore(this)
        resourceCache = ResourceCache(this)
        monitoringPreferences = MonitoringPreferences(this)
        displayPreferences = DisplayPreferences(this)
        portForwardManager = PortForwardManager()
        NotificationHelper.createChannels(this)
        scheduleWidgetRefresh()
        scheduleClusterMonitor()
    }

    private fun scheduleClusterMonitor() {
        val request = PeriodicWorkRequestBuilder<ClusterMonitorWorker>(
            15, TimeUnit.MINUTES,
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ClusterMonitorWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun onTerminate() {
        portForwardManager.close()
        super.onTerminate()
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
