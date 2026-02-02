package dev.nutting.kexplore.data.model

data class ResourceMetricsSnapshot(
    val timestamp: Long,
    val cpuMillicores: Long,
    val memoryBytes: Long,
)

data class ContainerMetrics(
    val containerName: String,
    val cpuMillicores: Long,
    val memoryBytes: Long,
)

data class PodMetricsData(
    val podName: String,
    val containers: List<ContainerMetrics>,
    val totalCpu: Long,
    val totalMemory: Long,
    val timestamp: Long,
)

data class NodeMetricsData(
    val nodeName: String,
    val cpuMillicores: Long,
    val memoryBytes: Long,
    val cpuCapacity: Long?,
    val memoryCapacity: Long?,
    val timestamp: Long,
)
