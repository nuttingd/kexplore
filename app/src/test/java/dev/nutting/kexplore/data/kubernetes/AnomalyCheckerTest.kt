package dev.nutting.kexplore.data.kubernetes

import dev.nutting.kexplore.data.model.ResourceStatus
import dev.nutting.kexplore.data.model.ResourceSummary
import dev.nutting.kexplore.data.model.ResourceType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnomalyCheckerTest {

    @Test
    fun `no issues when all resources healthy`() = runBlocking {
        val repo = FakeKubernetesRepository(
            pods = listOf(
                fakeSummary("pod-1", ResourceType.Pod, ResourceStatus.Running),
            ),
            deployments = listOf(
                fakeSummary("deploy-1", ResourceType.Deployment, ResourceStatus.Running, readyCount = "3/3"),
            ),
            nodes = listOf(
                fakeSummary("node-1", ResourceType.Node, ResourceStatus.Running),
            ),
        )
        val checker = AnomalyChecker(repo)
        val result = checker.check("default")

        assertFalse(result.workloadsHasIssues)
        assertFalse(result.clusterHasIssues)
        assertEquals(0, result.totalIssueCount)
    }

    @Test
    fun `detects failed pods`() = runBlocking {
        val repo = FakeKubernetesRepository(
            pods = listOf(
                fakeSummary("pod-1", ResourceType.Pod, ResourceStatus.Running),
                fakeSummary("pod-2", ResourceType.Pod, ResourceStatus.Failed),
                fakeSummary("pod-3", ResourceType.Pod, ResourceStatus.Failed),
            ),
            deployments = emptyList(),
            nodes = listOf(
                fakeSummary("node-1", ResourceType.Node, ResourceStatus.Running),
            ),
        )
        val checker = AnomalyChecker(repo)
        val result = checker.check("default")

        assertTrue(result.workloadsHasIssues)
        assertFalse(result.clusterHasIssues)
        assertEquals(2, result.totalIssueCount)
    }

    @Test
    fun `detects unhealthy deployments`() = runBlocking {
        val repo = FakeKubernetesRepository(
            pods = emptyList(),
            deployments = listOf(
                fakeSummary("deploy-1", ResourceType.Deployment, ResourceStatus.Running, readyCount = "2/3"),
                fakeSummary("deploy-2", ResourceType.Deployment, ResourceStatus.Running, readyCount = "3/3"),
            ),
            nodes = listOf(
                fakeSummary("node-1", ResourceType.Node, ResourceStatus.Running),
            ),
        )
        val checker = AnomalyChecker(repo)
        val result = checker.check("default")

        assertTrue(result.workloadsHasIssues)
        assertEquals(1, result.totalIssueCount)
    }

    @Test
    fun `detects unhealthy nodes`() = runBlocking {
        val repo = FakeKubernetesRepository(
            pods = emptyList(),
            deployments = emptyList(),
            nodes = listOf(
                fakeSummary("node-1", ResourceType.Node, ResourceStatus.Running),
                fakeSummary("node-2", ResourceType.Node, ResourceStatus.Unknown),
            ),
        )
        val checker = AnomalyChecker(repo)
        val result = checker.check("default")

        assertFalse(result.workloadsHasIssues)
        assertTrue(result.clusterHasIssues)
        assertEquals(1, result.totalIssueCount)
    }

    @Test
    fun `combined issues`() = runBlocking {
        val repo = FakeKubernetesRepository(
            pods = listOf(
                fakeSummary("pod-1", ResourceType.Pod, ResourceStatus.Failed),
            ),
            deployments = listOf(
                fakeSummary("deploy-1", ResourceType.Deployment, ResourceStatus.Running, readyCount = "0/1"),
            ),
            nodes = listOf(
                fakeSummary("node-1", ResourceType.Node, ResourceStatus.Unknown),
            ),
        )
        val checker = AnomalyChecker(repo)
        val result = checker.check("default")

        assertTrue(result.workloadsHasIssues)
        assertTrue(result.clusterHasIssues)
        assertEquals(3, result.totalIssueCount)
    }

    private fun fakeSummary(
        name: String,
        kind: ResourceType,
        status: ResourceStatus,
        readyCount: String? = null,
    ) = ResourceSummary(
        name = name,
        namespace = "default",
        kind = kind,
        status = status,
        age = "1h",
        readyCount = readyCount,
    )

    /**
     * Minimal fake that returns pre-configured resource lists.
     */
    private class FakeKubernetesRepository(
        private val pods: List<ResourceSummary>,
        private val deployments: List<ResourceSummary>,
        private val nodes: List<ResourceSummary>,
    ) : KubernetesRepository(createNoOpClient()) {

        override suspend fun getResources(namespace: String, type: ResourceType): List<ResourceSummary> =
            when (type) {
                ResourceType.Pod -> pods
                ResourceType.Deployment -> deployments
                ResourceType.Node -> nodes
                else -> emptyList()
            }

        companion object {
            private fun createNoOpClient(): io.fabric8.kubernetes.client.KubernetesClient {
                // Use a mock-like client that is never actually called
                return io.fabric8.kubernetes.client.KubernetesClientBuilder()
                    .withConfig(
                        io.fabric8.kubernetes.client.ConfigBuilder()
                            .withMasterUrl("https://localhost:6443")
                            .build()
                    )
                    .build()
            }
        }
    }
}
