package dev.nutting.kexplore.data.kubernetes

import dev.nutting.kexplore.data.model.ResourceDetail
import dev.nutting.kexplore.data.model.ResourceSummary
import dev.nutting.kexplore.data.model.ResourceType
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.ExecWatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import java.io.Closeable
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

class KubernetesRepository(private val client: KubernetesClient) {

    suspend fun getNamespaces(): List<String> = withContext(Dispatchers.IO) {
        client.namespaces().list().items.map { it.metadata.name }
    }

    suspend fun getResources(namespace: String, type: ResourceType): List<ResourceSummary> =
        withContext(Dispatchers.IO) {
            val allNamespaces = namespace.isEmpty()
            val items: List<HasMetadata> = when (type) {
                ResourceType.Pod -> if (allNamespaces) client.pods().inAnyNamespace().list().items else client.pods().inNamespace(namespace).list().items
                ResourceType.Deployment -> if (allNamespaces) client.apps().deployments().inAnyNamespace().list().items else client.apps().deployments().inNamespace(namespace).list().items
                ResourceType.ReplicaSet -> if (allNamespaces) client.apps().replicaSets().inAnyNamespace().list().items else client.apps().replicaSets().inNamespace(namespace).list().items
                ResourceType.StatefulSet -> if (allNamespaces) client.apps().statefulSets().inAnyNamespace().list().items else client.apps().statefulSets().inNamespace(namespace).list().items
                ResourceType.DaemonSet -> if (allNamespaces) client.apps().daemonSets().inAnyNamespace().list().items else client.apps().daemonSets().inNamespace(namespace).list().items
                ResourceType.Job -> if (allNamespaces) client.batch().v1().jobs().inAnyNamespace().list().items else client.batch().v1().jobs().inNamespace(namespace).list().items
                ResourceType.CronJob -> if (allNamespaces) client.batch().v1().cronjobs().inAnyNamespace().list().items else client.batch().v1().cronjobs().inNamespace(namespace).list().items
                ResourceType.Event -> if (allNamespaces) client.v1().events().inAnyNamespace().list().items else client.v1().events().inNamespace(namespace).list().items
                ResourceType.HorizontalPodAutoscaler -> if (allNamespaces) client.autoscaling().v2().horizontalPodAutoscalers().inAnyNamespace().list().items else client.autoscaling().v2().horizontalPodAutoscalers().inNamespace(namespace).list().items
                ResourceType.Service -> if (allNamespaces) client.services().inAnyNamespace().list().items else client.services().inNamespace(namespace).list().items
                ResourceType.Ingress -> if (allNamespaces) client.network().v1().ingresses().inAnyNamespace().list().items else client.network().v1().ingresses().inNamespace(namespace).list().items
                ResourceType.NetworkPolicy -> if (allNamespaces) client.network().v1().networkPolicies().inAnyNamespace().list().items else client.network().v1().networkPolicies().inNamespace(namespace).list().items
                ResourceType.Endpoints -> if (allNamespaces) client.endpoints().inAnyNamespace().list().items else client.endpoints().inNamespace(namespace).list().items
                ResourceType.ConfigMap -> if (allNamespaces) client.configMaps().inAnyNamespace().list().items else client.configMaps().inNamespace(namespace).list().items
                ResourceType.Secret -> if (allNamespaces) client.secrets().inAnyNamespace().list().items else client.secrets().inNamespace(namespace).list().items
                ResourceType.ServiceAccount -> if (allNamespaces) client.serviceAccounts().inAnyNamespace().list().items else client.serviceAccounts().inNamespace(namespace).list().items
                ResourceType.Role -> if (allNamespaces) client.rbac().roles().inAnyNamespace().list().items else client.rbac().roles().inNamespace(namespace).list().items
                ResourceType.RoleBinding -> if (allNamespaces) client.rbac().roleBindings().inAnyNamespace().list().items else client.rbac().roleBindings().inNamespace(namespace).list().items
                ResourceType.PersistentVolumeClaim -> if (allNamespaces) client.persistentVolumeClaims().inAnyNamespace().list().items else client.persistentVolumeClaims().inNamespace(namespace).list().items
                ResourceType.PersistentVolume -> client.persistentVolumes().list().items
                ResourceType.StorageClass -> client.storage().v1().storageClasses().list().items
                ResourceType.Node -> client.nodes().list().items
                ResourceType.Namespace -> client.namespaces().list().items
                ResourceType.ClusterRole -> client.rbac().clusterRoles().list().items
                ResourceType.ClusterRoleBinding -> client.rbac().clusterRoleBindings().list().items
                ResourceType.ResourceQuota -> if (allNamespaces) client.resourceQuotas().inAnyNamespace().list().items else client.resourceQuotas().inNamespace(namespace).list().items
                ResourceType.LimitRange -> if (allNamespaces) client.limitRanges().inAnyNamespace().list().items else client.limitRanges().inNamespace(namespace).list().items
            }
            items.map { ResourceMappers.toSummary(it, type) }
        }

    suspend fun getResource(namespace: String, type: ResourceType, name: String): HasMetadata =
        withContext(Dispatchers.IO) {
            val resource: HasMetadata? = when (type) {
                ResourceType.Pod -> client.pods().inNamespace(namespace).withName(name).get()
                ResourceType.Deployment -> client.apps().deployments().inNamespace(namespace).withName(name).get()
                ResourceType.ReplicaSet -> client.apps().replicaSets().inNamespace(namespace).withName(name).get()
                ResourceType.StatefulSet -> client.apps().statefulSets().inNamespace(namespace).withName(name).get()
                ResourceType.DaemonSet -> client.apps().daemonSets().inNamespace(namespace).withName(name).get()
                ResourceType.Job -> client.batch().v1().jobs().inNamespace(namespace).withName(name).get()
                ResourceType.CronJob -> client.batch().v1().cronjobs().inNamespace(namespace).withName(name).get()
                ResourceType.Event -> client.v1().events().inNamespace(namespace).withName(name).get()
                ResourceType.HorizontalPodAutoscaler -> client.autoscaling().v2().horizontalPodAutoscalers().inNamespace(namespace).withName(name).get()
                ResourceType.Service -> client.services().inNamespace(namespace).withName(name).get()
                ResourceType.Ingress -> client.network().v1().ingresses().inNamespace(namespace).withName(name).get()
                ResourceType.NetworkPolicy -> client.network().v1().networkPolicies().inNamespace(namespace).withName(name).get()
                ResourceType.Endpoints -> client.endpoints().inNamespace(namespace).withName(name).get()
                ResourceType.ConfigMap -> client.configMaps().inNamespace(namespace).withName(name).get()
                ResourceType.Secret -> client.secrets().inNamespace(namespace).withName(name).get()
                ResourceType.ServiceAccount -> client.serviceAccounts().inNamespace(namespace).withName(name).get()
                ResourceType.Role -> client.rbac().roles().inNamespace(namespace).withName(name).get()
                ResourceType.RoleBinding -> client.rbac().roleBindings().inNamespace(namespace).withName(name).get()
                ResourceType.PersistentVolumeClaim -> client.persistentVolumeClaims().inNamespace(namespace).withName(name).get()
                ResourceType.PersistentVolume -> client.persistentVolumes().withName(name).get()
                ResourceType.StorageClass -> client.storage().v1().storageClasses().withName(name).get()
                ResourceType.Node -> client.nodes().withName(name).get()
                ResourceType.Namespace -> client.namespaces().withName(name).get()
                ResourceType.ClusterRole -> client.rbac().clusterRoles().withName(name).get()
                ResourceType.ClusterRoleBinding -> client.rbac().clusterRoleBindings().withName(name).get()
                ResourceType.ResourceQuota -> client.resourceQuotas().inNamespace(namespace).withName(name).get()
                ResourceType.LimitRange -> client.limitRanges().inNamespace(namespace).withName(name).get()
            }
            resource ?: throw NoSuchElementException("${type.name} '$name' not found")
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
    ): Flow<String> = callbackFlow {
        val logWatch = client.pods()
            .inNamespace(namespace)
            .withName(name)
            .let { op -> if (container != null) op.inContainer(container) else op }
            .tailingLines(tailLines)
            .watchLog()

        try {
            logWatch.output.bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    send(line)
                    line = reader.readLine()
                }
            }
            channel.close()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            channel.close(e)
        }

        awaitClose { logWatch.close() }
    }.flowOn(Dispatchers.IO)

    suspend fun getContainerNames(namespace: String, podName: String): List<String> =
        withContext(Dispatchers.IO) {
            val pod = client.pods().inNamespace(namespace).withName(podName).get()
                ?: throw NoSuchElementException("Pod '$podName' not found in namespace '$namespace'")
            pod.spec?.containers?.map { it.name } ?: emptyList()
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
        } catch (e: CancellationException) {
            throw e
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

    suspend fun execInteractive(
        namespace: String,
        podName: String,
        container: String?,
    ): ExecSession = withContext(Dispatchers.IO) {
        val stdoutPipe = PipedInputStream()
        val stdoutOutput = PipedOutputStream().also { stdoutPipe.connect(it) }
        val stderrPipe = PipedInputStream()
        val stderrOutput = PipedOutputStream().also { stderrPipe.connect(it) }

        val watch = client.pods()
            .inNamespace(namespace)
            .withName(podName)
            .let { op -> if (container != null) op.inContainer(container) else op }
            .redirectingInput()
            .writingOutput(stdoutOutput)
            .writingError(stderrOutput)
            .withTTY()
            .exec("/bin/sh")

        ExecSession(
            stdin = watch.input,
            stdout = stdoutPipe,
            stderr = stderrPipe,
            watch = watch,
        )
    }
}

class ExecSession(
    val stdin: OutputStream,
    val stdout: PipedInputStream,
    val stderr: PipedInputStream,
    val watch: ExecWatch,
) : Closeable {
    override fun close() {
        try { stdin.close() } catch (_: Exception) {}
        try { stdout.close() } catch (_: Exception) {}
        try { stderr.close() } catch (_: Exception) {}
        try { watch.close() } catch (_: Exception) {}
    }
}
