package dev.nutting.kexplore.data.portforward

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import dev.nutting.kexplore.KexploreApp
import dev.nutting.kexplore.MainActivity
import dev.nutting.kexplore.R
import dev.nutting.kexplore.data.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PortForwardService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification(0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification.build())
        }

        startMonitoring()
        return START_STICKY
    }

    @SuppressLint("MissingPermission") // Service notification uses FOREGROUND_SERVICE permission
    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            val app = applicationContext as KexploreApp
            val manager = app.portForwardManager
            while (isActive) {
                val count = manager.activeCount
                if (count == 0) {
                    stopSelf()
                    break
                }
                val notification = buildNotification(count)
                NotificationManagerCompat.from(this@PortForwardService)
                    .notify(NOTIFICATION_ID, notification.build())
                delay(2_000)
            }
        }
    }

    private fun buildNotification(activeCount: Int): NotificationCompat.Builder {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val text = if (activeCount == 0) "Starting..." else "$activeCount active forward${if (activeCount != 1) "s" else ""}"

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_SERVICE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Port Forwarding")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        val app = applicationContext as KexploreApp
        app.portForwardManager.stopAll()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1002
    }
}
