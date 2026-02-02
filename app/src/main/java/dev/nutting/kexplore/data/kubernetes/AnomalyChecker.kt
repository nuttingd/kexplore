package dev.nutting.kexplore.data.kubernetes

import dev.nutting.kexplore.data.model.ResourceStatus
import dev.nutting.kexplore.data.model.ResourceType

data class TabAnomalies(
    val workloadsHasIssues: Boolean = false,
    val clusterHasIssues: Boolean = false,
    val totalIssueCount: Int = 0,
)

class AnomalyChecker(private val repository: KubernetesRepository) {

    suspend fun check(namespace: String): TabAnomalies {
        var workloadsIssues = 0
        var clusterIssues = 0

        try {
            val pods = repository.getResources(namespace, ResourceType.Pod)
            workloadsIssues += pods.count { it.status == ResourceStatus.Failed }
        } catch (_: Exception) {}

        try {
            val deployments = repository.getResources(namespace, ResourceType.Deployment)
            workloadsIssues += deployments.count { summary ->
                val ready = summary.readyCount ?: return@count false
                val parts = ready.split("/")
                parts.size == 2 && parts[0].trim() != parts[1].trim()
            }
        } catch (_: Exception) {}

        try {
            val nodes = repository.getResources("", ResourceType.Node)
            clusterIssues += nodes.count { it.status != ResourceStatus.Running }
        } catch (_: Exception) {}

        val total = workloadsIssues + clusterIssues
        return TabAnomalies(
            workloadsHasIssues = workloadsIssues > 0,
            clusterHasIssues = clusterIssues > 0,
            totalIssueCount = total,
        )
    }
}
