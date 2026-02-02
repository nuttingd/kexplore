package dev.nutting.kexplore.data.kubernetes

import dev.nutting.kexplore.data.model.ResourceDetail
import dev.nutting.kexplore.data.model.ResourceSummary
import dev.nutting.kexplore.data.model.ResourceType
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.KubernetesResourceList
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.ExecWatch
import io.fabric8.kubernetes.client.dsl.MixedOperation
import io.fabric8.kubernetes.client.dsl.Resource
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

    /**
     * Lists items from a namespaced operation, respecting the all-namespaces vs single-namespace scope.
     */
    private fun <T : HasMetadata, L : KubernetesResourceList<T>, R : Resource<T>>
            MixedOperation<T, L, R>.listScoped(namespace: String): List<T> =
        if (namespace.isEmpty()) inAnyNamespace().list().items
        else inNamespace(namespace).list().items

    suspend fun getNamespaces(): List<String> = withContext(Dispatchers.IO) {
        client.namespaces().list().items.map { it.metadata.name }
    }

    suspend fun getResources(namespace: String, type: ResourceType): List<ResourceSummary> =
        withContext(Dispatchers.IO) {
            val items: List<HasMetadata> = when (type) {
                ResourceType.Pod -> client.pods().listScoped(namespace)
                ResourceType.Deployment -> client.apps().deployments().listScoped(namespace)
                ResourceType.ReplicaSet -> client.apps().replicaSets().listScoped(namespace)
                ResourceType.StatefulSet -> client.apps().statefulSets().listScoped(namespace)
                ResourceType.DaemonSet -> client.apps().daemonSets().listScoped(namespace)
                ResourceType.Job -> client.batch().v1().jobs().listScoped(namespace)
                ResourceType.CronJob -> client.batch().v1().cronjobs().listScoped(namespace)
                ResourceType.Event -> client.v1().events().listScoped(namespace)
                ResourceType.HorizontalPodAutoscaler -> client.autoscaling().v2().horizontalPodAutoscalers().listScoped(namespace)
                ResourceType.Service -> client.services().listScoped(namespace)
                ResourceType.Ingress -> client.network().v1().ingresses().listScoped(namespace)
                ResourceType.NetworkPolicy -> client.network().v1().networkPolicies().listScoped(namespace)
                ResourceType.Endpoints -> client.endpoints().listScoped(namespace)
                ResourceType.ConfigMap -> client.configMaps().listScoped(namespace)
                ResourceType.Secret -> client.secrets().listScoped(namespace)
                ResourceType.ServiceAccount -> client.serviceAccounts().listScoped(namespace)
                ResourceType.Role -> client.rbac().roles().listScoped(namespace)
                ResourceType.RoleBinding -> client.rbac().roleBindings().listScoped(namespace)
                ResourceType.PersistentVolumeClaim -> client.persistentVolumeClaims().listScoped(namespace)
                ResourceType.PersistentVolume -> client.persistentVolumes().list().items
                ResourceType.StorageClass -> client.storage().v1().storageClasses().list().items
                ResourceType.Node -> client.nodes().list().items
                ResourceType.Namespace -> client.namespaces().list().items
                ResourceType.ClusterRole -> client.rbac().clusterRoles().list().items
                ResourceType.ClusterRoleBinding -> client.rbac().clusterRoleBindings().list().items
                ResourceType.ResourceQuota -> client.resourceQuotas().listScoped(namespace)
                ResourceType.LimitRange -> client.limitRanges().listScoped(namespace)
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

    suspend fun deleteResource(namespace: String, type: ResourceType, name: String): Unit =
        withContext(Dispatchers.IO) {
            when (type) {
                ResourceType.Pod -> client.pods().inNamespace(namespace).withName(name).delete()
                ResourceType.Deployment -> client.apps().deployments().inNamespace(namespace).withName(name).delete()
                ResourceType.ReplicaSet -> client.apps().replicaSets().inNamespace(namespace).withName(name).delete()
                ResourceType.StatefulSet -> client.apps().statefulSets().inNamespace(namespace).withName(name).delete()
                ResourceType.DaemonSet -> client.apps().daemonSets().inNamespace(namespace).withName(name).delete()
                ResourceType.Job -> client.batch().v1().jobs().inNamespace(namespace).withName(name).delete()
                ResourceType.CronJob -> client.batch().v1().cronjobs().inNamespace(namespace).withName(name).delete()
                ResourceType.Event -> client.v1().events().inNamespace(namespace).withName(name).delete()
                ResourceType.HorizontalPodAutoscaler -> client.autoscaling().v2().horizontalPodAutoscalers().inNamespace(namespace).withName(name).delete()
                ResourceType.Service -> client.services().inNamespace(namespace).withName(name).delete()
                ResourceType.Ingress -> client.network().v1().ingresses().inNamespace(namespace).withName(name).delete()
                ResourceType.NetworkPolicy -> client.network().v1().networkPolicies().inNamespace(namespace).withName(name).delete()
                ResourceType.Endpoints -> client.endpoints().inNamespace(namespace).withName(name).delete()
                ResourceType.ConfigMap -> client.configMaps().inNamespace(namespace).withName(name).delete()
                ResourceType.Secret -> client.secrets().inNamespace(namespace).withName(name).delete()
                ResourceType.ServiceAccount -> client.serviceAccounts().inNamespace(namespace).withName(name).delete()
                ResourceType.Role -> client.rbac().roles().inNamespace(namespace).withName(name).delete()
                ResourceType.RoleBinding -> client.rbac().roleBindings().inNamespace(namespace).withName(name).delete()
                ResourceType.PersistentVolumeClaim -> client.persistentVolumeClaims().inNamespace(namespace).withName(name).delete()
                ResourceType.PersistentVolume -> client.persistentVolumes().withName(name).delete()
                ResourceType.StorageClass -> client.storage().v1().storageClasses().withName(name).delete()
                ResourceType.Node -> client.nodes().withName(name).delete()
                ResourceType.Namespace -> client.namespaces().withName(name).delete()
                ResourceType.ClusterRole -> client.rbac().clusterRoles().withName(name).delete()
                ResourceType.ClusterRoleBinding -> client.rbac().clusterRoleBindings().withName(name).delete()
                ResourceType.ResourceQuota -> client.resourceQuotas().inNamespace(namespace).withName(name).delete()
                ResourceType.LimitRange -> client.limitRanges().inNamespace(namespace).withName(name).delete()
            }
        }

    suspend fun scaleResource(namespace: String, type: ResourceType, name: String, replicas: Int): Unit =
        withContext(Dispatchers.IO) {
            when (type) {
                ResourceType.Deployment -> client.apps().deployments().inNamespace(namespace).withName(name).scale(replicas)
                ResourceType.StatefulSet -> client.apps().statefulSets().inNamespace(namespace).withName(name).scale(replicas)
                ResourceType.ReplicaSet -> client.apps().replicaSets().inNamespace(namespace).withName(name).scale(replicas)
                else -> throw IllegalArgumentException("${type.displayName} does not support scaling")
            }
        }

    suspend fun restartResource(namespace: String, type: ResourceType, name: String): Unit =
        withContext(Dispatchers.IO) {
            when (type) {
                ResourceType.Deployment -> client.apps().deployments().inNamespace(namespace).withName(name).rolling().restart()
                ResourceType.StatefulSet -> client.apps().statefulSets().inNamespace(namespace).withName(name).rolling().restart()
                ResourceType.DaemonSet -> {
                    val ds = client.apps().daemonSets().inNamespace(namespace).withName(name).get()
                        ?: throw NoSuchElementException("DaemonSet '$name' not found")
                    val annotations = ds.spec?.template?.metadata?.annotations?.toMutableMap() ?: mutableMapOf()
                    annotations["kubectl.kubernetes.io/restartedAt"] = java.time.Instant.now().toString()
                    ds.spec.template.metadata.annotations = annotations
                    client.apps().daemonSets().inNamespace(namespace).resource(ds).update()
                }
                else -> throw IllegalArgumentException("${type.displayName} does not support restart")
            }
        }

    suspend fun triggerCronJob(namespace: String, name: String): String =
        withContext(Dispatchers.IO) {
            val cronJob = client.batch().v1().cronjobs().inNamespace(namespace).withName(name).get()
                ?: throw NoSuchElementException("CronJob '$name' not found")
            val jobName = "$name-manual-${System.currentTimeMillis() / 1000}"
            val job = io.fabric8.kubernetes.api.model.batch.v1.JobBuilder()
                .withNewMetadata()
                    .withName(jobName)
                    .withNamespace(namespace)
                .endMetadata()
                .withSpec(cronJob.spec.jobTemplate.spec)
                .build()
            client.batch().v1().jobs().inNamespace(namespace).resource(job).create()
            jobName
        }

    suspend fun cordonNode(name: String): Unit =
        withContext(Dispatchers.IO) {
            val node = client.nodes().withName(name).get()
                ?: throw NoSuchElementException("Node '$name' not found")
            node.spec.unschedulable = true
            client.nodes().resource(node).update()
        }

    suspend fun uncordonNode(name: String): Unit =
        withContext(Dispatchers.IO) {
            val node = client.nodes().withName(name).get()
                ?: throw NoSuchElementException("Node '$name' not found")
            node.spec.unschedulable = false
            client.nodes().resource(node).update()
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
