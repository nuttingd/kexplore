package dev.nutting.kexplore.data.notification

import android.annotation.SuppressLint
import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dev.nutting.kexplore.KexploreApp
import dev.nutting.kexplore.data.kubernetes.KubernetesClientFactory
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.Node
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.WatcherException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.Closeable
import kotlin.random.Random

class ClusterWatchService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var client: KubernetesClient? = null
    private val watches = mutableListOf<Closeable>()
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    private var connectionId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = applicationContext as KexploreApp
        val notification = NotificationHelper.createServiceNotification(
            this, "cluster",
        ).build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NotificationHelper.SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, notification)
        }

        scope.launch {
            startWatching(app)
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission") // Service notification uses FOREGROUND_SERVICE permission
    private suspend fun startWatching(app: KexploreApp) {
        stopWatches()

        val activeId = app.connectionStore.getActiveConnectionId() ?: run {
            stopSelf()
            return
        }
        val connection = app.connectionStore.getConnection(activeId) ?: run {
            stopSelf()
            return
        }
        connectionId = activeId

        val enabledTypes = app.monitoringPreferences.enabledAlertTypes.first()
        val alertStore = AlertStateStore(this)

        try {
            val newClient = KubernetesClientFactory.createClient(connection)
            client = newClient

            // Update service notification with cluster name
            val updatedNotification = NotificationHelper.createServiceNotification(
                this, connection.name,
            ).build()
            NotificationManagerCompat.from(this).notify(
                NotificationHelper.SERVICE_NOTIFICATION_ID, updatedNotification,
            )

            // Watch pods
            if (AlertType.PodFailed in enabledTypes || AlertType.PodCrashLoop in enabledTypes) {
                val watch = newClient.pods().inAnyNamespace().watch(object : Watcher<Pod> {
                    override fun eventReceived(action: Watcher.Action, resource: Pod) {
                        if (action == Watcher.Action.MODIFIED || action == Watcher.Action.ADDED) {
                            checkPod(resource, activeId, alertStore, enabledTypes)
                        }
                    }
                    override fun onClose(cause: WatcherException?) {
                        if (cause != null) scheduleReconnect(app)
                    }
                })
                watches.add(watch)
            }

            // Watch nodes
            if (AlertType.NodeNotReady in enabledTypes) {
                val watch = newClient.nodes().watch(object : Watcher<Node> {
                    override fun eventReceived(action: Watcher.Action, resource: Node) {
                        if (action == Watcher.Action.MODIFIED || action == Watcher.Action.ADDED) {
                            checkNode(resource, activeId, alertStore)
                        }
                    }
                    override fun onClose(cause: WatcherException?) {
                        if (cause != null) scheduleReconnect(app)
                    }
                })
                watches.add(watch)
            }

            // Watch deployments
            if (AlertType.DeploymentDegraded in enabledTypes) {
                val watch = newClient.apps().deployments().inAnyNamespace().watch(object : Watcher<Deployment> {
                    override fun eventReceived(action: Watcher.Action, resource: Deployment) {
                        if (action == Watcher.Action.MODIFIED || action == Watcher.Action.ADDED) {
                            checkDeployment(resource, activeId, alertStore)
                        }
                    }
                    override fun onClose(cause: WatcherException?) {
                        if (cause != null) scheduleReconnect(app)
                    }
                })
                watches.add(watch)
            }
            reconnectAttempt = 0
        } catch (_: Exception) {
            scheduleReconnect(app)
        }
    }

    private fun checkPod(pod: Pod, connId: String, alertStore: AlertStateStore, enabledTypes: Set<AlertType>) {
        val name = pod.metadata.name
        val ns = pod.metadata.namespace

        if (AlertType.PodFailed in enabledTypes && pod.status?.phase == "Failed") {
            fireAlert(ClusterAlert(
                id = "pod-failed:$ns/$name",
                connectionId = connId,
                type = AlertType.PodFailed,
                resourceName = name,
                namespace = ns,
                message = "Pod $name in $ns is Failed",
            ), alertStore)
        }

        if (AlertType.PodCrashLoop in enabledTypes) {
            val crashLoop = pod.status?.containerStatuses?.any { cs ->
                cs.state?.waiting?.reason == "CrashLoopBackOff"
            } == true
            if (crashLoop) {
                fireAlert(ClusterAlert(
                    id = "pod-crashloop:$ns/$name",
                    connectionId = connId,
                    type = AlertType.PodCrashLoop,
                    resourceName = name,
                    namespace = ns,
                    message = "Pod $name in $ns is in CrashLoopBackOff",
                ), alertStore)
            }
        }
    }

    private fun checkNode(node: Node, connId: String, alertStore: AlertStateStore) {
        val name = node.metadata.name
        val ready = node.status?.conditions?.find { it.type == "Ready" }
        if (ready?.status != "True") {
            fireAlert(ClusterAlert(
                id = "node-notready:$name",
                connectionId = connId,
                type = AlertType.NodeNotReady,
                resourceName = name,
                namespace = null,
                message = "Node $name is not ready",
            ), alertStore)
        }
    }

    private fun checkDeployment(deploy: Deployment, connId: String, alertStore: AlertStateStore) {
        val name = deploy.metadata.name
        val ns = deploy.metadata.namespace
        val desired = deploy.spec?.replicas ?: 0
        val ready = deploy.status?.readyReplicas ?: 0
        if (desired > 0 && ready < desired) {
            fireAlert(ClusterAlert(
                id = "deploy-degraded:$ns/$name",
                connectionId = connId,
                type = AlertType.DeploymentDegraded,
                resourceName = name,
                namespace = ns,
                message = "Deployment $name in $ns: $ready/$desired replicas ready",
            ), alertStore)
        }
    }

    private fun fireAlert(alert: ClusterAlert, alertStore: AlertStateStore) {
        if (alertStore.hasBeenNotified(alert.connectionId, alert.id)) return
        alertStore.markNotified(alert.connectionId, alert.id)

        val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        if (canNotify) {
            val notification = NotificationHelper.createAlertNotification(this, alert)
            NotificationManagerCompat.from(this).notify(alert.id.hashCode(), notification.build())
        }
    }

    private fun scheduleReconnect(app: KexploreApp) {
        reconnectJob?.cancel()
        val delayMs = minOf(30_000L * (1 shl reconnectAttempt), 300_000L) + Random.nextLong(5_000)
        reconnectAttempt++
        reconnectJob = scope.launch {
            delay(delayMs)
            startWatching(app)
        }
    }

    private fun stopWatches() {
        reconnectJob?.cancel()
        watches.forEach { runCatching { it.close() } }
        watches.clear()
        client?.close()
        client = null
    }

    override fun onDestroy() {
        stopWatches()
        scope.cancel()
        super.onDestroy()
    }
}
