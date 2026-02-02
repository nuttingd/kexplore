package dev.nutting.kexplore.data.kubernetes

import dev.nutting.kexplore.data.model.DependencyNode
import dev.nutting.kexplore.data.model.ResourceStatus
import dev.nutting.kexplore.data.model.ResourceType
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DaemonSet
import io.fabric8.kubernetes.api.model.apps.ReplicaSet
import io.fabric8.kubernetes.api.model.apps.StatefulSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DependencyResolver(private val repository: KubernetesRepository) {

    suspend fun resolveDependencies(
        namespace: String,
        type: ResourceType,
        name: String,
    ): DependencyNode = withContext(Dispatchers.IO) {
        when (type) {
            ResourceType.Deployment -> resolveDeployment(namespace, name)
            ResourceType.StatefulSet -> resolveStatefulSet(namespace, name)
            ResourceType.DaemonSet -> resolveDaemonSet(namespace, name)
            ResourceType.ReplicaSet -> resolveReplicaSet(namespace, name)
            ResourceType.Service -> resolveService(namespace, name)
            else -> DependencyNode(
                name = name,
                namespace = namespace,
                kind = type.displayName,
                status = ResourceStatus.Unknown,
            )
        }
    }

    private suspend fun resolveDeployment(namespace: String, name: String): DependencyNode {
        val deployments = repository.getResourcesRaw(namespace, ResourceType.Deployment)
        val deployment = deployments.filterIsInstance<Deployment>().find { it.metadata.name == name }
            ?: return DependencyNode(name, namespace, "Deployment", ResourceStatus.Unknown)

        val replicaSets = repository.getResourcesRaw(namespace, ResourceType.ReplicaSet)
            .filterIsInstance<ReplicaSet>()
            .filter { rs ->
                rs.metadata.ownerReferences?.any {
                    it.kind == "Deployment" && it.name == name
                } == true
            }

        val rsChildren = replicaSets.map { rs -> resolveReplicaSet(namespace, rs.metadata.name) }

        // Find services targeting this deployment's pods
        val serviceChildren = findServicesForLabels(namespace, deployment.spec?.selector?.matchLabels ?: emptyMap())

        val status = deploymentStatus(deployment)
        val readyCount = "${deployment.status?.readyReplicas ?: 0}/${deployment.spec?.replicas ?: 0}"

        return DependencyNode(
            name = name,
            namespace = namespace,
            kind = "Deployment",
            status = status,
            readyCount = readyCount,
            children = rsChildren + serviceChildren,
        )
    }

    private suspend fun resolveStatefulSet(namespace: String, name: String): DependencyNode {
        val statefulSets = repository.getResourcesRaw(namespace, ResourceType.StatefulSet)
        val sts = statefulSets.filterIsInstance<StatefulSet>().find { it.metadata.name == name }
            ?: return DependencyNode(name, namespace, "StatefulSet", ResourceStatus.Unknown)

        val pods = findPodsOwnedBy(namespace, "StatefulSet", name)
        val serviceChildren = findServicesForLabels(namespace, sts.spec?.selector?.matchLabels ?: emptyMap())

        val status = if ((sts.status?.readyReplicas ?: 0) == (sts.spec?.replicas ?: 0))
            ResourceStatus.Running else ResourceStatus.Pending
        val readyCount = "${sts.status?.readyReplicas ?: 0}/${sts.spec?.replicas ?: 0}"

        return DependencyNode(
            name = name,
            namespace = namespace,
            kind = "StatefulSet",
            status = status,
            readyCount = readyCount,
            children = pods + serviceChildren,
        )
    }

    private suspend fun resolveDaemonSet(namespace: String, name: String): DependencyNode {
        val daemonSets = repository.getResourcesRaw(namespace, ResourceType.DaemonSet)
        val ds = daemonSets.filterIsInstance<DaemonSet>().find { it.metadata.name == name }
            ?: return DependencyNode(name, namespace, "DaemonSet", ResourceStatus.Unknown)

        val pods = findPodsOwnedBy(namespace, "DaemonSet", name)

        val ready = ds.status?.numberReady ?: 0
        val desired = ds.status?.desiredNumberScheduled ?: 0
        val status = if (ready == desired) ResourceStatus.Running else ResourceStatus.Pending

        return DependencyNode(
            name = name,
            namespace = namespace,
            kind = "DaemonSet",
            status = status,
            readyCount = "$ready/$desired",
            children = pods,
        )
    }

    private suspend fun resolveReplicaSet(namespace: String, name: String): DependencyNode {
        val replicaSets = repository.getResourcesRaw(namespace, ResourceType.ReplicaSet)
        val rs = replicaSets.filterIsInstance<ReplicaSet>().find { it.metadata.name == name }
            ?: return DependencyNode(name, namespace, "ReplicaSet", ResourceStatus.Unknown)

        val pods = findPodsOwnedBy(namespace, "ReplicaSet", name)

        val ready = rs.status?.readyReplicas ?: 0
        val desired = rs.spec?.replicas ?: 0
        val status = if (ready == desired) ResourceStatus.Running else ResourceStatus.Pending

        return DependencyNode(
            name = name,
            namespace = namespace,
            kind = "ReplicaSet",
            status = status,
            readyCount = "$ready/$desired",
            children = pods,
        )
    }

    private suspend fun resolveService(namespace: String, name: String): DependencyNode {
        val services = repository.getResourcesRaw(namespace, ResourceType.Service)
        val svc = services.filterIsInstance<io.fabric8.kubernetes.api.model.Service>()
            .find { it.metadata.name == name }
            ?: return DependencyNode(name, namespace, "Service", ResourceStatus.Unknown)

        // Find ingresses that reference this service
        val ingresses = try {
            repository.getResourcesRaw(namespace, ResourceType.Ingress)
                .filterIsInstance<io.fabric8.kubernetes.api.model.networking.v1.Ingress>()
                .filter { ingress ->
                    ingress.spec?.rules?.any { rule ->
                        rule.http?.paths?.any { path ->
                            path.backend?.service?.name == name
                        } == true
                    } == true
                }
                .map { ingress ->
                    DependencyNode(
                        name = ingress.metadata.name,
                        namespace = namespace,
                        kind = "Ingress",
                        status = ResourceStatus.Running,
                    )
                }
        } catch (_: Exception) { emptyList() }

        return DependencyNode(
            name = name,
            namespace = namespace,
            kind = "Service",
            status = ResourceStatus.Running,
            children = ingresses,
        )
    }

    private suspend fun findPodsOwnedBy(namespace: String, ownerKind: String, ownerName: String): List<DependencyNode> {
        return try {
            repository.getResourcesRaw(namespace, ResourceType.Pod)
                .filter { pod ->
                    pod.metadata.ownerReferences?.any {
                        it.kind == ownerKind && it.name == ownerName
                    } == true
                }
                .map { pod -> podToNode(pod) }
        } catch (_: Exception) { emptyList() }
    }

    private suspend fun findServicesForLabels(namespace: String, labels: Map<String, String>): List<DependencyNode> {
        if (labels.isEmpty()) return emptyList()
        return try {
            repository.getResourcesRaw(namespace, ResourceType.Service)
                .filterIsInstance<io.fabric8.kubernetes.api.model.Service>()
                .filter { svc ->
                    val selector = svc.spec?.selector ?: return@filter false
                    labels.all { (k, v) -> selector[k] == v }
                }
                .map { svc -> resolveService(namespace, svc.metadata.name) }
        } catch (_: Exception) { emptyList() }
    }

    private fun podToNode(pod: HasMetadata): DependencyNode {
        val p = pod as? io.fabric8.kubernetes.api.model.Pod
        val phase = p?.status?.phase ?: "Unknown"
        val status = when (phase) {
            "Running" -> ResourceStatus.Running
            "Pending" -> ResourceStatus.Pending
            "Succeeded" -> ResourceStatus.Succeeded
            "Failed" -> ResourceStatus.Failed
            else -> ResourceStatus.Unknown
        }
        return DependencyNode(
            name = pod.metadata.name,
            namespace = pod.metadata.namespace ?: "",
            kind = "Pod",
            status = status,
        )
    }

    private fun deploymentStatus(deployment: Deployment): ResourceStatus {
        val ready = deployment.status?.readyReplicas ?: 0
        val desired = deployment.spec?.replicas ?: 0
        return when {
            desired == 0 -> ResourceStatus.Running
            ready >= desired -> ResourceStatus.Running
            ready > 0 -> ResourceStatus.Pending
            else -> ResourceStatus.Failed
        }
    }
}
