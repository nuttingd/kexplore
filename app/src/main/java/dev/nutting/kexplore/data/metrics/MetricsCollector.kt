package dev.nutting.kexplore.data.metrics

import dev.nutting.kexplore.data.kubernetes.MetricsRepository
import dev.nutting.kexplore.data.model.NodeMetricsData
import dev.nutting.kexplore.data.model.ResourceMetricsSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MetricsCollector(
    private val metricsRepository: MetricsRepository,
    private val scope: CoroutineScope,
) {
    private val _snapshots = MutableStateFlow<List<ResourceMetricsSnapshot>>(emptyList())
    val snapshots: StateFlow<List<ResourceMetricsSnapshot>> = _snapshots.asStateFlow()

    private val _nodeCapacity = MutableStateFlow<NodeMetricsData?>(null)
    val nodeCapacity: StateFlow<NodeMetricsData?> = _nodeCapacity.asStateFlow()

    private val _metricsAvailable = MutableStateFlow<Boolean?>(null)
    val metricsAvailable: StateFlow<Boolean?> = _metricsAvailable.asStateFlow()

    private val buffer = ArrayDeque<ResourceMetricsSnapshot>(MAX_SAMPLES)
    private var pollingJob: Job? = null

    fun startPodMetrics(namespace: String, podName: String) {
        stop()
        buffer.clear()
        _snapshots.value = emptyList()
        _metricsAvailable.value = null

        pollingJob = scope.launch {
            val available = metricsRepository.isMetricsAvailable()
            _metricsAvailable.value = available
            if (!available) return@launch

            while (true) {
                val data = metricsRepository.getPodMetrics(namespace, podName)
                if (data != null) {
                    addSnapshot(ResourceMetricsSnapshot(
                        timestamp = data.timestamp,
                        cpuMillicores = data.totalCpu,
                        memoryBytes = data.totalMemory,
                    ))
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun startNodeMetrics(nodeName: String) {
        stop()
        buffer.clear()
        _snapshots.value = emptyList()
        _metricsAvailable.value = null

        pollingJob = scope.launch {
            val available = metricsRepository.isMetricsAvailable()
            _metricsAvailable.value = available
            if (!available) return@launch

            while (true) {
                val data = metricsRepository.getNodeMetrics(nodeName)
                if (data != null) {
                    _nodeCapacity.value = data
                    addSnapshot(ResourceMetricsSnapshot(
                        timestamp = data.timestamp,
                        cpuMillicores = data.cpuMillicores,
                        memoryBytes = data.memoryBytes,
                    ))
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun addSnapshot(snapshot: ResourceMetricsSnapshot) {
        if (buffer.size >= MAX_SAMPLES) {
            buffer.removeFirst()
        }
        buffer.addLast(snapshot)
        _snapshots.value = buffer.toList()
    }

    companion object {
        const val POLL_INTERVAL_MS = 5_000L
        const val MAX_SAMPLES = 60 // 5 minutes at 5s intervals
    }
}
