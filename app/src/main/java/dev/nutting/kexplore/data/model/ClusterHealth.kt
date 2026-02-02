package dev.nutting.kexplore.data.model

data class ClusterHealth(
    val failedPods: List<ResourceSummary> = emptyList(),
    val unhealthyDeployments: List<ResourceSummary> = emptyList(),
    val unhealthyNodes: List<ResourceSummary> = emptyList(),
    val warningEvents: List<ResourceSummary> = emptyList(),
) {
    val totalIssues: Int
        get() = failedPods.size + unhealthyDeployments.size + unhealthyNodes.size + warningEvents.size

    val isHealthy: Boolean
        get() = totalIssues == 0
}
