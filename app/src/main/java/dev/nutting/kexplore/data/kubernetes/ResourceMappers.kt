package dev.nutting.kexplore.data.kubernetes

import dev.nutting.kexplore.data.model.Condition
import dev.nutting.kexplore.data.model.ContainerInfo
import dev.nutting.kexplore.data.model.ResourceDetail
import dev.nutting.kexplore.data.model.ResourceStatus
import dev.nutting.kexplore.data.model.ResourceSummary
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.util.DateFormatter
import io.fabric8.kubernetes.api.model.ConfigMap
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.Secret
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DaemonSet
import io.fabric8.kubernetes.api.model.apps.ReplicaSet
import io.fabric8.kubernetes.api.model.apps.StatefulSet
import io.fabric8.kubernetes.api.model.batch.v1.CronJob
import io.fabric8.kubernetes.api.model.batch.v1.Job

object ResourceMappers {

    fun toSummary(resource: HasMetadata, type: ResourceType): ResourceSummary {
        val metadata = resource.metadata
        return ResourceSummary(
            name = metadata.name,
            namespace = metadata.namespace ?: "",
            kind = type,
            status = inferStatus(resource),
            age = DateFormatter.age(metadata.creationTimestamp),
            labels = metadata.labels ?: emptyMap(),
            readyCount = readyCount(resource),
            restarts = restartCount(resource),
        )
    }

    fun toDetail(resource: HasMetadata, type: ResourceType): ResourceDetail {
        val metadata = resource.metadata
        return ResourceDetail(
            name = metadata.name,
            namespace = metadata.namespace ?: "",
            kind = type,
            status = inferStatus(resource),
            uid = metadata.uid ?: "",
            creationTimestamp = metadata.creationTimestamp ?: "",
            labels = metadata.labels ?: emptyMap(),
            annotations = metadata.annotations ?: emptyMap(),
            conditions = extractConditions(resource),
            spec = extractSpec(resource),
            containers = extractContainers(resource),
        )
    }

    private fun inferStatus(resource: HasMetadata): ResourceStatus = when (resource) {
        is Pod -> when (resource.status?.phase) {
            "Running" -> ResourceStatus.Running
            "Pending" -> ResourceStatus.Pending
            "Succeeded" -> ResourceStatus.Succeeded
            "Failed" -> ResourceStatus.Failed
            else -> {
                if (resource.metadata?.deletionTimestamp != null) ResourceStatus.Terminating
                else ResourceStatus.Unknown
            }
        }
        is Deployment -> {
            val available = resource.status?.conditions
                ?.any { it.type == "Available" && it.status == "True" } ?: false
            if (available) ResourceStatus.Running else ResourceStatus.Pending
        }
        is StatefulSet -> {
            val ready = resource.status?.readyReplicas ?: 0
            val desired = resource.spec?.replicas ?: 0
            if (ready >= desired) ResourceStatus.Running else ResourceStatus.Pending
        }
        is DaemonSet -> {
            val ready = resource.status?.numberReady ?: 0
            val desired = resource.status?.desiredNumberScheduled ?: 0
            if (ready >= desired) ResourceStatus.Running else ResourceStatus.Pending
        }
        is Job -> {
            val succeeded = resource.status?.succeeded ?: 0
            val failed = resource.status?.failed ?: 0
            when {
                succeeded > 0 -> ResourceStatus.Succeeded
                failed > 0 -> ResourceStatus.Failed
                else -> ResourceStatus.Pending
            }
        }
        else -> ResourceStatus.Running
    }

    private fun readyCount(resource: HasMetadata): String? = when (resource) {
        is Pod -> {
            val total = resource.spec?.containers?.size ?: 0
            val ready = resource.status?.containerStatuses?.count { it.ready } ?: 0
            "$ready/$total"
        }
        is Deployment -> {
            val ready = resource.status?.readyReplicas ?: 0
            val desired = resource.spec?.replicas ?: 0
            "$ready/$desired"
        }
        is StatefulSet -> {
            val ready = resource.status?.readyReplicas ?: 0
            val desired = resource.spec?.replicas ?: 0
            "$ready/$desired"
        }
        is ReplicaSet -> {
            val ready = resource.status?.readyReplicas ?: 0
            val desired = resource.spec?.replicas ?: 0
            "$ready/$desired"
        }
        is DaemonSet -> {
            val ready = resource.status?.numberReady ?: 0
            val desired = resource.status?.desiredNumberScheduled ?: 0
            "$ready/$desired"
        }
        else -> null
    }

    private fun restartCount(resource: HasMetadata): Int? = when (resource) {
        is Pod -> resource.status?.containerStatuses?.sumOf { it.restartCount } ?: 0
        else -> null
    }

    private fun extractConditions(resource: HasMetadata): List<Condition> = when (resource) {
        is Pod -> resource.status?.conditions?.map {
            Condition(
                type = it.type,
                status = it.status,
                reason = it.reason,
                message = it.message,
                lastTransitionTime = it.lastTransitionTime,
            )
        } ?: emptyList()
        is Deployment -> resource.status?.conditions?.map {
            Condition(
                type = it.type,
                status = it.status,
                reason = it.reason,
                message = it.message,
                lastTransitionTime = it.lastTransitionTime,
            )
        } ?: emptyList()
        else -> emptyList()
    }

    private fun extractSpec(resource: HasMetadata): Map<String, String> = when (resource) {
        is Pod -> buildMap {
            put("Node", resource.spec?.nodeName ?: "")
            put("Pod IP", resource.status?.podIP ?: "")
            put("QoS Class", resource.status?.qosClass ?: "")
            put("Service Account", resource.spec?.serviceAccountName ?: "")
            put("Restart Policy", resource.spec?.restartPolicy ?: "")
            resource.spec?.volumes?.let { volumes ->
                put("Volumes", volumes.joinToString(", ") { it.name })
            }
        }
        is Deployment -> buildMap {
            put("Strategy", resource.spec?.strategy?.type ?: "")
            val desired = resource.spec?.replicas ?: 0
            val ready = resource.status?.readyReplicas ?: 0
            val updated = resource.status?.updatedReplicas ?: 0
            val available = resource.status?.availableReplicas ?: 0
            put("Replicas", "$ready ready / $desired desired")
            put("Updated Replicas", "$updated")
            put("Available Replicas", "$available")
            resource.spec?.selector?.matchLabels?.let { selectors ->
                put("Selector", selectors.entries.joinToString(", ") { "${it.key}=${it.value}" })
            }
        }
        is Service -> buildMap {
            put("Type", resource.spec?.type ?: "")
            put("Cluster IP", resource.spec?.clusterIP ?: "")
            resource.spec?.ports?.let { ports ->
                put("Ports", ports.joinToString("\n") {
                    buildString {
                        append("${it.port}")
                        if (it.targetPort != null) append(" → ${it.targetPort.value}")
                        append("/${it.protocol}")
                        if (it.nodePort != null && it.nodePort > 0) append(" (nodePort: ${it.nodePort})")
                    }
                })
            }
            resource.spec?.selector?.let { selectors ->
                put("Selector", selectors.entries.joinToString(", ") { "${it.key}=${it.value}" })
            }
        }
        is StatefulSet -> mapOf(
            "Replicas" to "${resource.spec?.replicas ?: 0}",
            "Service Name" to (resource.spec?.serviceName ?: ""),
        )
        is CronJob -> mapOf(
            "Schedule" to (resource.spec?.schedule ?: ""),
            "Suspend" to "${resource.spec?.suspend ?: false}",
        )
        is ConfigMap -> buildMap {
            resource.data?.forEach { (key, value) ->
                val preview = if (value.length > 80) value.take(80) + "..." else value
                put(key, preview)
            }
        }
        is Secret -> buildMap {
            put("Type", resource.type ?: "Opaque")
            resource.data?.keys?.forEach { key ->
                put(key, "\u2022\u2022\u2022\u2022\u2022")
            }
        }
        else -> emptyMap()
    }

    private fun extractContainers(resource: HasMetadata): List<ContainerInfo> = when (resource) {
        is Pod -> resource.spec?.containers?.map { container ->
            val status = resource.status?.containerStatuses?.find { it.name == container.name }
            ContainerInfo(
                name = container.name,
                image = container.image,
                ready = status?.ready ?: false,
                restartCount = status?.restartCount ?: 0,
                state = when {
                    status?.state?.running != null -> "Running"
                    status?.state?.waiting != null -> status.state.waiting.reason ?: "Waiting"
                    status?.state?.terminated != null -> status.state.terminated.reason ?: "Terminated"
                    else -> "Unknown"
                },
                ports = container.ports?.map { "${it.containerPort}/${it.protocol ?: "TCP"}" } ?: emptyList(),
                env = container.env?.map { env ->
                    if (env.valueFrom != null) "${env.name}=<ref>" else "${env.name}=${env.value ?: ""}"
                } ?: emptyList(),
                resources = buildString {
                    container.resources?.requests?.let { req ->
                        if (req.isNotEmpty()) append("Requests: ${req.entries.joinToString(", ") { "${it.key}=${it.value}" }}")
                    }
                    container.resources?.limits?.let { lim ->
                        if (lim.isNotEmpty()) {
                            if (isNotEmpty()) append(" | ")
                            append("Limits: ${lim.entries.joinToString(", ") { "${it.key}=${it.value}" }}")
                        }
                    }
                }.ifEmpty { null },
                mounts = container.volumeMounts?.map { "${it.name} → ${it.mountPath}" } ?: emptyList(),
            )
        } ?: emptyList()
        else -> emptyList()
    }
}
