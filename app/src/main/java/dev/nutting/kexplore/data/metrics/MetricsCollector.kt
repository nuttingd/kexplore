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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MetricsCollector(
    private val metricsRepository: MetricsRepository,
    private val scope: CoroutineScope,
    private val pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
) {
    private val _snapshots = MutableStateFlow<List<ResourceMetricsSnapshot>>(emptyList())
    val snapshots: StateFlow<List<ResourceMetricsSnapshot>> = _snapshots.asStateFlow()

    private val _nodeCapacity = MutableStateFlow<NodeMetricsData?>(null)
    val nodeCapacity: StateFlow<NodeMetricsData?> = _nodeCapacity.asStateFlow()

    private val _metricsAvailable = MutableStateFlow<Boolean?>(null)
    val metricsAvailable: StateFlow<Boolean?> = _metricsAvailable.asStateFlow()

    private val maxSamples = windowSlots(pollIntervalMs)
    private val bufferMutex = Mutex()
    private val buffer = ArrayDeque<ResourceMetricsSnapshot>(maxSamples)
    private var pollingJob: Job? = null

    fun startPodMetrics(namespace: String, podName: String) {
        stop()
        clearBuffer()
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
                delay(pollIntervalMs)
            }
        }
    }

    fun startNodeMetrics(nodeName: String) {
        stop()
        clearBuffer()
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
                delay(pollIntervalMs)
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun clearBuffer() {
        // Use runBlocking-free clear since stop() already cancelled the polling job
        buffer.clear()
        _snapshots.value = emptyList()
        _metricsAvailable.value = null
    }

    private suspend fun addSnapshot(snapshot: ResourceMetricsSnapshot) {
        bufferMutex.withLock {
            if (buffer.size >= maxSamples) {
                buffer.removeFirst()
            }
            buffer.addLast(snapshot)
            _snapshots.value = buffer.toList()
        }
    }

    companion object {
        const val DEFAULT_POLL_INTERVAL_MS = 2_000L
        const val WINDOW_DURATION_MS = 60_000L

        fun windowSlots(intervalMs: Long): Int = (WINDOW_DURATION_MS / intervalMs).toInt()
    }
}
