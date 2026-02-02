package dev.nutting.kexplore.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dev.nutting.kexplore.MainActivity
import dev.nutting.kexplore.R

object NotificationHelper {

    const val CHANNEL_ALERTS = "cluster_alerts"
    const val CHANNEL_SERVICE = "monitoring_service"
    const val SERVICE_NOTIFICATION_ID = 1001

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val alertChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "Cluster Alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications for cluster health issues"
        }

        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            "Monitoring Service",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Background cluster monitoring"
        }

        manager.createNotificationChannels(listOf(alertChannel, serviceChannel))
    }

    fun createAlertNotification(
        context: Context,
        alert: ClusterAlert,
    ): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, alert.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = when (alert.type) {
            AlertType.PodCrashLoop -> "Pod CrashLoopBackOff"
            AlertType.PodFailed -> "Pod Failed"
            AlertType.NodeNotReady -> "Node Not Ready"
            AlertType.DeploymentDegraded -> "Deployment Degraded"
        }

        return NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(alert.message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
    }

    fun createServiceNotification(
        context: Context,
        clusterName: String,
    ): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Monitoring $clusterName")
            .setContentText("Watching for cluster health issues")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }
}
