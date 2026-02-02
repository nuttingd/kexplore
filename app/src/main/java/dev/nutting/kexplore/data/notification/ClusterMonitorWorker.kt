package dev.nutting.kexplore.data.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.nutting.kexplore.KexploreApp
import dev.nutting.kexplore.data.kubernetes.KubernetesClientFactory
import kotlinx.coroutines.flow.first

class ClusterMonitorWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = context.applicationContext as KexploreApp
        val prefs = app.monitoringPreferences

        // Check if monitoring is enabled
        if (!prefs.monitoringEnabled.first()) return Result.success()

        val connectionStore = app.connectionStore
        val activeId = connectionStore.getActiveConnectionId() ?: return Result.success()
        val connection = connectionStore.getConnection(activeId) ?: return Result.success()

        val alertStore = AlertStateStore(context)
        alertStore.clearOlderThan(AlertStateStore.COOLDOWN_MILLIS)

        val enabledTypes = prefs.enabledAlertTypes.first()

        try {
            val client = KubernetesClientFactory.createClient(connection)
            client.use { k8s ->
                val alerts = mutableListOf<ClusterAlert>()

                // Check pods
                if (AlertType.PodFailed in enabledTypes || AlertType.PodCrashLoop in enabledTypes) {
                    val pods = k8s.pods().inAnyNamespace().list().items
                    for (pod in pods) {
                        val name = pod.metadata.name
                        val ns = pod.metadata.namespace
                        val phase = pod.status?.phase

                        if (AlertType.PodFailed in enabledTypes && phase == "Failed") {
                            alerts.add(ClusterAlert(
                                id = "pod-failed:$ns/$name",
                                connectionId = activeId,
                                type = AlertType.PodFailed,
                                resourceName = name,
                                namespace = ns,
                                message = "Pod $name in $ns is Failed",
                            ))
                        }

                        if (AlertType.PodCrashLoop in enabledTypes) {
                            val crashLoop = pod.status?.containerStatuses?.any { cs ->
                                cs.state?.waiting?.reason == "CrashLoopBackOff"
                            } == true
                            if (crashLoop) {
                                alerts.add(ClusterAlert(
                                    id = "pod-crashloop:$ns/$name",
                                    connectionId = activeId,
                                    type = AlertType.PodCrashLoop,
                                    resourceName = name,
                                    namespace = ns,
                                    message = "Pod $name in $ns is in CrashLoopBackOff",
                                ))
                            }
                        }
                    }
                }

                // Check nodes
                if (AlertType.NodeNotReady in enabledTypes) {
                    val nodes = k8s.nodes().list().items
                    for (node in nodes) {
                        val name = node.metadata.name
                        val ready = node.status?.conditions?.find { it.type == "Ready" }
                        if (ready?.status != "True") {
                            alerts.add(ClusterAlert(
                                id = "node-notready:$name",
                                connectionId = activeId,
                                type = AlertType.NodeNotReady,
                                resourceName = name,
                                namespace = null,
                                message = "Node $name is not ready",
                            ))
                        }
                    }
                }

                // Check deployments
                if (AlertType.DeploymentDegraded in enabledTypes) {
                    val deployments = k8s.apps().deployments().inAnyNamespace().list().items
                    for (deploy in deployments) {
                        val name = deploy.metadata.name
                        val ns = deploy.metadata.namespace
                        val desired = deploy.spec?.replicas ?: 0
                        val ready = deploy.status?.readyReplicas ?: 0
                        if (desired > 0 && ready < desired) {
                            alerts.add(ClusterAlert(
                                id = "deploy-degraded:$ns/$name",
                                connectionId = activeId,
                                type = AlertType.DeploymentDegraded,
                                resourceName = name,
                                namespace = ns,
                                message = "Deployment $name in $ns: $ready/$desired replicas ready",
                            ))
                        }
                    }
                }

                // Fire notifications for new alerts
                val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                } else true

                if (canNotify) {
                    val notificationManager = NotificationManagerCompat.from(context)
                    for (alert in alerts) {
                        if (!alertStore.hasBeenNotified(alert.connectionId, alert.id)) {
                            alertStore.markNotified(alert.connectionId, alert.id)
                            val notification = NotificationHelper.createAlertNotification(context, alert)
                            notificationManager.notify(alert.id.hashCode(), notification.build())
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Silently fail — worker will retry on next schedule
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "kexplore_cluster_monitor"
    }
}
