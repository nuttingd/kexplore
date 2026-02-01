package dev.nutting.kexplore.data.kubernetes

import dev.nutting.kexplore.data.model.ResourceDetail
import dev.nutting.kexplore.data.model.ResourceSummary
import dev.nutting.kexplore.data.model.ResourceType
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.client.KubernetesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class KubernetesRepository(private val client: KubernetesClient) {

    suspend fun getNamespaces(): List<String> = withContext(Dispatchers.IO) {
        client.namespaces().list().items.map { it.metadata.name }
    }

    suspend fun getResources(namespace: String, type: ResourceType): List<ResourceSummary> =
        withContext(Dispatchers.IO) {
            val items: List<HasMetadata> = when (type) {
                ResourceType.Pod -> client.pods().inNamespace(namespace).list().items
                ResourceType.Deployment -> client.apps().deployments().inNamespace(namespace).list().items
                ResourceType.ReplicaSet -> client.apps().replicaSets().inNamespace(namespace).list().items
                ResourceType.StatefulSet -> client.apps().statefulSets().inNamespace(namespace).list().items
                ResourceType.DaemonSet -> client.apps().daemonSets().inNamespace(namespace).list().items
                ResourceType.Job -> client.batch().v1().jobs().inNamespace(namespace).list().items
                ResourceType.CronJob -> client.batch().v1().cronjobs().inNamespace(namespace).list().items
                ResourceType.Service -> client.services().inNamespace(namespace).list().items
                ResourceType.Ingress -> client.network().v1().ingresses().inNamespace(namespace).list().items
                ResourceType.ConfigMap -> client.configMaps().inNamespace(namespace).list().items
                ResourceType.Secret -> client.secrets().inNamespace(namespace).list().items
                ResourceType.ServiceAccount -> client.serviceAccounts().inNamespace(namespace).list().items
                ResourceType.PersistentVolumeClaim -> client.persistentVolumeClaims().inNamespace(namespace).list().items
            }
            items.map { ResourceMappers.toSummary(it, type) }
        }

    suspend fun getResource(namespace: String, type: ResourceType, name: String): HasMetadata =
        withContext(Dispatchers.IO) {
            when (type) {
                ResourceType.Pod -> client.pods().inNamespace(namespace).withName(name).get()
                ResourceType.Deployment -> client.apps().deployments().inNamespace(namespace).withName(name).get()
                ResourceType.ReplicaSet -> client.apps().replicaSets().inNamespace(namespace).withName(name).get()
                ResourceType.StatefulSet -> client.apps().statefulSets().inNamespace(namespace).withName(name).get()
                ResourceType.DaemonSet -> client.apps().daemonSets().inNamespace(namespace).withName(name).get()
                ResourceType.Job -> client.batch().v1().jobs().inNamespace(namespace).withName(name).get()
                ResourceType.CronJob -> client.batch().v1().cronjobs().inNamespace(namespace).withName(name).get()
                ResourceType.Service -> client.services().inNamespace(namespace).withName(name).get()
                ResourceType.Ingress -> client.network().v1().ingresses().inNamespace(namespace).withName(name).get()
                ResourceType.ConfigMap -> client.configMaps().inNamespace(namespace).withName(name).get()
                ResourceType.Secret -> client.secrets().inNamespace(namespace).withName(name).get()
                ResourceType.ServiceAccount -> client.serviceAccounts().inNamespace(namespace).withName(name).get()
                ResourceType.PersistentVolumeClaim -> client.persistentVolumeClaims().inNamespace(namespace).withName(name).get()
            }
        }

    suspend fun getResourceDetail(namespace: String, type: ResourceType, name: String): ResourceDetail =
        withContext(Dispatchers.IO) {
            val resource = getResource(namespace, type, name)
            ResourceMappers.toDetail(resource, type)
        }

    suspend fun getResourceYaml(namespace: String, type: ResourceType, name: String): String =
        withContext(Dispatchers.IO) {
            val resource = getResource(namespace, type, name)
            dev.nutting.kexplore.util.YamlSerializer.toYaml(resource)
        }

    fun streamPodLogs(
        namespace: String,
        name: String,
        container: String? = null,
        tailLines: Int = 100,
    ): Flow<String> = flow {
        val logWatch = client.pods()
            .inNamespace(namespace)
            .withName(name)
            .let { op -> if (container != null) op.inContainer(container) else op }
            .tailingLines(tailLines)
            .watchLog()

        logWatch.output.bufferedReader().use { reader ->
            var line = reader.readLine()
            while (line != null) {
                emit(line)
                line = reader.readLine()
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getContainerNames(namespace: String, podName: String): List<String> =
        withContext(Dispatchers.IO) {
            val pod = client.pods().inNamespace(namespace).withName(podName).get()
            pod.spec.containers.map { it.name }
        }

    suspend fun execCommand(
        namespace: String,
        podName: String,
        container: String?,
        command: List<String>,
    ): String = withContext(Dispatchers.IO) {
        val outputStream = java.io.ByteArrayOutputStream()
        val errorStream = java.io.ByteArrayOutputStream()

        val execWatch = client.pods()
            .inNamespace(namespace)
            .withName(podName)
            .let { op -> if (container != null) op.inContainer(container) else op }
            .writingOutput(outputStream)
            .writingError(errorStream)
            .exec(*command.toTypedArray())

        // Wait for the exec to complete
        try {
            execWatch.exitCode().join()
        } catch (_: Exception) {
            // Timeout or interruption
        } finally {
            execWatch.close()
        }

        val stdout = outputStream.toString()
        val stderr = errorStream.toString()
        buildString {
            if (stdout.isNotEmpty()) append(stdout)
            if (stderr.isNotEmpty()) {
                if (isNotEmpty()) append("\n")
                append(stderr)
            }
        }
    }
}
