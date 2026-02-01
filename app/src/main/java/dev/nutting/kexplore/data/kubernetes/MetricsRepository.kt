package dev.nutting.kexplore.data.kubernetes

import android.util.Log
import dev.nutting.kexplore.data.model.ContainerMetrics
import dev.nutting.kexplore.data.model.NodeMetricsData
import dev.nutting.kexplore.data.model.PodMetricsData
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.client.KubernetesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MetricsRepository(private val client: KubernetesClient) {

    suspend fun isMetricsAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            client.top().nodes().metrics().items
            true
        } catch (e: Exception) {
            Log.w(TAG, "Metrics API not available", e)
            false
        }
    }

    suspend fun getPodMetrics(namespace: String, podName: String): PodMetricsData? =
        withContext(Dispatchers.IO) {
            try {
                val metrics = client.top().pods()
                    .metrics(namespace, podName)
                val now = System.currentTimeMillis()
                val containers = metrics.containers?.map { container ->
                    ContainerMetrics(
                        containerName = container.name,
                        cpuMillicores = quantityToMillicores(container.usage["cpu"]),
                        memoryBytes = quantityToBytes(container.usage["memory"]),
                    )
                } ?: emptyList()
                PodMetricsData(
                    podName = podName,
                    containers = containers,
                    totalCpu = containers.sumOf { it.cpuMillicores },
                    totalMemory = containers.sumOf { it.memoryBytes },
                    timestamp = now,
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch pod metrics", e)
                null
            }
        }

    suspend fun getNodeMetrics(nodeName: String): NodeMetricsData? =
        withContext(Dispatchers.IO) {
            try {
                val metricsList = client.top().nodes().metrics().items
                val nodeMetrics = metricsList.find { it.metadata.name == nodeName } ?: return@withContext null
                val now = System.currentTimeMillis()

                val node = client.nodes().withName(nodeName).get()
                val cpuCapacity = node?.status?.capacity?.get("cpu")?.let { quantityToMillicores(it) }
                val memoryCapacity = node?.status?.capacity?.get("memory")?.let { quantityToBytes(it) }

                NodeMetricsData(
                    nodeName = nodeName,
                    cpuMillicores = quantityToMillicores(nodeMetrics.usage["cpu"]),
                    memoryBytes = quantityToBytes(nodeMetrics.usage["memory"]),
                    cpuCapacity = cpuCapacity,
                    memoryCapacity = memoryCapacity,
                    timestamp = now,
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch node metrics", e)
                null
            }
        }

    companion object {
        private const val TAG = "MetricsRepository"

        fun quantityToMillicores(quantity: Quantity?): Long {
            if (quantity == null) return 0
            // getNumericalAmount() returns cores as BigDecimal (e.g. "250m" → 0.250, "1n" → 0.000000001)
            val cores = quantity.numericalAmount
            return cores.multiply(java.math.BigDecimal(1000)).toLong()
        }

        fun quantityToBytes(quantity: Quantity?): Long {
            if (quantity == null) return 0
            // getNumericalAmount() returns bytes as BigDecimal (e.g. "100Mi" → 104857600)
            return quantity.numericalAmount.toLong()
        }
    }
}
