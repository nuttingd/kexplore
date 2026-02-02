package dev.nutting.kexplore.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.nutting.kexplore.KexploreApp
import dev.nutting.kexplore.data.kubernetes.KubernetesClientFactory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WidgetRefreshWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val connectionStore = (context.applicationContext as KexploreApp).connectionStore
        val activeId = connectionStore.getActiveConnectionId() ?: return Result.success()
        val connection = connectionStore.getConnection(activeId) ?: return Result.success()

        val state = try {
            val client = KubernetesClientFactory.createClient(connection)
            client.use { k8s ->
                val pods = k8s.pods().inAnyNamespace().list().items
                val nodes = k8s.nodes().list().items

                val totalPods = pods.size
                val runningPods = pods.count { pod ->
                    pod.status?.phase == "Running"
                }
                val failedPods = pods.count { pod ->
                    pod.status?.phase == "Failed"
                }
                val totalNodes = nodes.size
                val readyNodes = nodes.count { node ->
                    node.status?.conditions?.any { it.type == "Ready" && it.status == "True" } == true
                }

                WidgetState(
                    clusterName = connection.name,
                    totalPods = totalPods,
                    runningPods = runningPods,
                    failedPods = failedPods,
                    totalNodes = totalNodes,
                    readyNodes = readyNodes,
                    lastUpdated = System.currentTimeMillis(),
                )
            }
        } catch (e: Exception) {
            WidgetState(
                clusterName = connection.name,
                lastUpdated = System.currentTimeMillis(),
                error = e.message ?: "Failed to fetch cluster status",
            )
        }

        val prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_WIDGET_STATE, Json.encodeToString(state)).apply()

        ClusterStatusWidget().updateAll(context)

        return Result.success()
    }

    companion object {
        const val WIDGET_PREFS = "kexplore_widget"
        const val KEY_WIDGET_STATE = "widget_state"
        const val WORK_NAME = "kexplore_widget_refresh"
    }
}
