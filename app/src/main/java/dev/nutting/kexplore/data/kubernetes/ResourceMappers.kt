package dev.nutting.kexplore.data.kubernetes

import dev.nutting.kexplore.data.model.Condition
import dev.nutting.kexplore.data.model.ContainerInfo
import dev.nutting.kexplore.data.model.ResourceDetail
import dev.nutting.kexplore.data.model.ResourceStatus
import dev.nutting.kexplore.data.model.ResourceSummary
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.util.DateFormatter
import io.fabric8.kubernetes.api.model.ConfigMap
import io.fabric8.kubernetes.api.model.Endpoints
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.LimitRange
import io.fabric8.kubernetes.api.model.Namespace
import io.fabric8.kubernetes.api.model.Node
import io.fabric8.kubernetes.api.model.PersistentVolume
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.ResourceQuota
import io.fabric8.kubernetes.api.model.Secret
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.apps.DaemonSet
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.ServiceAccount
import io.fabric8.kubernetes.api.model.apps.ReplicaSet
import io.fabric8.kubernetes.api.model.apps.StatefulSet
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscaler
import io.fabric8.kubernetes.api.model.batch.v1.CronJob
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.api.model.networking.v1.Ingress
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy
import io.fabric8.kubernetes.api.model.rbac.ClusterRole
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding
import io.fabric8.kubernetes.api.model.rbac.Role
import io.fabric8.kubernetes.api.model.rbac.RoleBinding
import io.fabric8.kubernetes.api.model.storage.StorageClass

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
        is Node -> {
            val ready = resource.status?.conditions
                ?.any { it.type == "Ready" && it.status == "True" } ?: false
            if (ready) ResourceStatus.Running else ResourceStatus.Failed
        }
        is Namespace -> when (resource.status?.phase) {
            "Active" -> ResourceStatus.Running
            "Terminating" -> ResourceStatus.Terminating
            else -> ResourceStatus.Unknown
        }
        is PersistentVolume -> when (resource.status?.phase) {
            "Available" -> ResourceStatus.Running
            "Bound" -> ResourceStatus.Running
            "Released" -> ResourceStatus.Pending
            "Failed" -> ResourceStatus.Failed
            else -> ResourceStatus.Unknown
        }
        is PersistentVolumeClaim -> when (resource.status?.phase) {
            "Bound" -> ResourceStatus.Running
            "Pending" -> ResourceStatus.Pending
            "Lost" -> ResourceStatus.Failed
            else -> ResourceStatus.Unknown
        }
        is io.fabric8.kubernetes.api.model.Event -> when (resource.type) {
            "Normal" -> ResourceStatus.Running
            "Warning" -> ResourceStatus.Failed
            else -> ResourceStatus.Unknown
        }
        is HorizontalPodAutoscaler -> {
            val conditions = resource.status?.conditions
            val active = conditions?.any { it.type == "ScalingActive" && it.status == "True" } ?: false
            if (active) ResourceStatus.Running else ResourceStatus.Pending
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
        is HorizontalPodAutoscaler -> {
            val current = resource.status?.currentReplicas ?: 0
            val desired = resource.status?.desiredReplicas ?: 0
            "$current/$desired"
        }
        else -> null
    }

    private fun restartCount(resource: HasMetadata): Int? = when (resource) {
        is Pod -> resource.status?.containerStatuses?.sumOf { it.restartCount } ?: 0
        else -> null
    }

    private fun <T> mapConditions(
        conditions: List<T>?,
        type: (T) -> String?,
        status: (T) -> String?,
        reason: (T) -> String?,
        message: (T) -> String?,
        lastTransitionTime: (T) -> String?,
    ): List<Condition> = conditions?.map { c ->
        Condition(
            type = type(c) ?: "",
            status = status(c) ?: "",
            reason = reason(c),
            message = message(c),
            lastTransitionTime = lastTransitionTime(c),
        )
    } ?: emptyList()

    private fun extractConditions(resource: HasMetadata): List<Condition> = when (resource) {
        is Pod -> mapConditions(resource.status?.conditions,
            { it.type }, { it.status }, { it.reason }, { it.message }, { it.lastTransitionTime })
        is Deployment -> mapConditions(resource.status?.conditions,
            { it.type }, { it.status }, { it.reason }, { it.message }, { it.lastTransitionTime })
        is Node -> mapConditions(resource.status?.conditions,
            { it.type }, { it.status }, { it.reason }, { it.message }, { it.lastTransitionTime })
        is HorizontalPodAutoscaler -> mapConditions(resource.status?.conditions,
            { it.type }, { it.status }, { it.reason }, { it.message }, { it.lastTransitionTime })
        is Namespace -> mapConditions(resource.status?.conditions,
            { it.type }, { it.status }, { it.reason }, { it.message }, { it.lastTransitionTime })
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
        is Node -> buildMap {
            put("Unschedulable", (resource.spec?.unschedulable == true).toString())
            val info = resource.status?.nodeInfo
            put("OS", "${info?.operatingSystem ?: ""} ${info?.osImage ?: ""}")
            put("Kernel", info?.kernelVersion ?: "")
            put("Kubelet", info?.kubeletVersion ?: "")
            put("Container Runtime", info?.containerRuntimeVersion ?: "")
            put("Architecture", info?.architecture ?: "")
            resource.status?.allocatable?.let { alloc ->
                alloc["cpu"]?.let { put("Allocatable CPU", it.toString()) }
                alloc["memory"]?.let { put("Allocatable Memory", it.toString()) }
                alloc["pods"]?.let { put("Allocatable Pods", it.toString()) }
            }
            resource.status?.addresses?.let { addrs ->
                addrs.forEach { addr ->
                    put("${addr.type} Address", addr.address)
                }
            }
        }
        is Namespace -> buildMap {
            put("Phase", resource.status?.phase ?: "")
        }
        is PersistentVolume -> buildMap {
            put("Phase", resource.status?.phase ?: "")
            resource.spec?.capacity?.get("storage")?.let { put("Capacity", it.toString()) }
            resource.spec?.accessModes?.let { put("Access Modes", it.joinToString(", ")) }
            put("Reclaim Policy", resource.spec?.persistentVolumeReclaimPolicy ?: "")
            put("Storage Class", resource.spec?.storageClassName ?: "")
            resource.spec?.claimRef?.let { ref ->
                put("Claim", "${ref.namespace}/${ref.name}")
            }
        }
        is PersistentVolumeClaim -> buildMap {
            put("Phase", resource.status?.phase ?: "")
            resource.status?.capacity?.get("storage")?.let { put("Capacity", it.toString()) }
            resource.spec?.accessModes?.let { put("Access Modes", it.joinToString(", ")) }
            put("Storage Class", resource.spec?.storageClassName ?: "")
            put("Volume Name", resource.spec?.volumeName ?: "")
        }
        is StorageClass -> buildMap {
            put("Provisioner", resource.provisioner ?: "")
            put("Reclaim Policy", resource.reclaimPolicy ?: "")
            put("Volume Binding Mode", resource.volumeBindingMode ?: "")
            if (resource.allowVolumeExpansion == true) put("Allow Expansion", "true")
            resource.parameters?.forEach { (k, v) -> put("Param: $k", v) }
        }
        is io.fabric8.kubernetes.api.model.Event -> buildMap {
            put("Type", resource.type ?: "")
            put("Reason", resource.reason ?: "")
            put("Message", resource.message ?: "")
            put("Count", "${resource.count ?: 0}")
            resource.firstTimestamp?.let { put("First Seen", it) }
            resource.lastTimestamp?.let { put("Last Seen", it) }
            resource.involvedObject?.let { obj ->
                put("Object", "${obj.kind}/${obj.name}")
            }
            put("Source", buildString {
                resource.source?.component?.let { append(it) }
                resource.source?.host?.let { if (isNotEmpty()) append("/"); append(it) }
            })
        }
        is HorizontalPodAutoscaler -> buildMap {
            put("Min Replicas", "${resource.spec?.minReplicas ?: 0}")
            put("Max Replicas", "${resource.spec?.maxReplicas ?: 0}")
            put("Current Replicas", "${resource.status?.currentReplicas ?: 0}")
            put("Desired Replicas", "${resource.status?.desiredReplicas ?: 0}")
            resource.spec?.metrics?.let { metrics ->
                put("Metrics", metrics.joinToString("\n") { metric ->
                    when (metric.type) {
                        "Resource" -> {
                            val name = metric.resource?.name ?: ""
                            val target = metric.resource?.target?.averageUtilization
                            "$name target: ${target ?: "?"}%"
                        }
                        else -> "${metric.type}: ${metric.`object`?.metric?.name ?: metric.pods?.metric?.name ?: ""}"
                    }
                })
            }
            resource.spec?.scaleTargetRef?.let { ref ->
                put("Target", "${ref.kind}/${ref.name}")
            }
        }
        is NetworkPolicy -> buildMap {
            resource.spec?.podSelector?.matchLabels?.let { labels ->
                put("Pod Selector", labels.entries.joinToString(", ") { "${it.key}=${it.value}" })
            } ?: put("Pod Selector", "<all pods>")
            resource.spec?.policyTypes?.let { put("Policy Types", it.joinToString(", ")) }
            resource.spec?.ingress?.let { rules ->
                put("Ingress Rules", "${rules.size}")
            }
            resource.spec?.egress?.let { rules ->
                put("Egress Rules", "${rules.size}")
            }
        }
        is Endpoints -> buildMap {
            resource.subsets?.let { subsets ->
                subsets.forEachIndexed { i, subset ->
                    val addresses = subset.addresses?.joinToString(", ") { it.ip } ?: "none"
                    val ports = subset.ports?.joinToString(", ") { "${it.port}/${it.protocol}" } ?: "none"
                    put("Subset ${i + 1} Addresses", addresses)
                    put("Subset ${i + 1} Ports", ports)
                }
            }
        }
        is Role -> buildMap {
            put("Rules", "${resource.rules?.size ?: 0}")
            resource.rules?.forEachIndexed { i, rule ->
                val resources = rule.resources?.joinToString(", ") ?: "*"
                val verbs = rule.verbs?.joinToString(", ") ?: "*"
                put("Rule ${i + 1}", "$verbs on $resources")
            }
        }
        is ClusterRole -> buildMap {
            put("Rules", "${resource.rules?.size ?: 0}")
            resource.rules?.take(5)?.forEachIndexed { i, rule ->
                val resources = rule.resources?.joinToString(", ") ?: "*"
                val verbs = rule.verbs?.joinToString(", ") ?: "*"
                put("Rule ${i + 1}", "$verbs on $resources")
            }
            val ruleCount = resource.rules?.size ?: 0
            if (ruleCount > 5) put("...", "+${ruleCount - 5} more rules")
        }
        is RoleBinding -> buildMap {
            put("Role", "${resource.roleRef.kind}/${resource.roleRef.name}")
            put("Subjects", "${resource.subjects?.size ?: 0}")
            resource.subjects?.forEach { subj ->
                put("${subj.kind}", "${subj.namespace?.let { "$it/" } ?: ""}${subj.name}")
            }
        }
        is ClusterRoleBinding -> buildMap {
            put("Role", "${resource.roleRef.kind}/${resource.roleRef.name}")
            put("Subjects", "${resource.subjects?.size ?: 0}")
            resource.subjects?.forEach { subj ->
                put("${subj.kind}", "${subj.namespace?.let { "$it/" } ?: ""}${subj.name}")
            }
        }
        is ResourceQuota -> buildMap {
            resource.status?.hard?.forEach { (k, v) ->
                val used = resource.status?.used?.get(k)
                put(k, "${used ?: "0"} / $v")
            }
        }
        is LimitRange -> buildMap {
            resource.spec?.limits?.forEachIndexed { i, limit ->
                put("Type ${i + 1}", limit.type ?: "")
                limit.default?.forEach { (k, v) -> put("Default $k", v.toString()) }
                limit.defaultRequest?.forEach { (k, v) -> put("Default Request $k", v.toString()) }
                limit.max?.forEach { (k, v) -> put("Max $k", v.toString()) }
                limit.min?.forEach { (k, v) -> put("Min $k", v.toString()) }
            }
        }
        is ReplicaSet -> buildMap {
            val desired = resource.spec?.replicas ?: 0
            val ready = resource.status?.readyReplicas ?: 0
            val available = resource.status?.availableReplicas ?: 0
            put("Replicas", "$ready ready / $desired desired")
            put("Available Replicas", "$available")
            resource.spec?.selector?.matchLabels?.let { selectors ->
                put("Selector", selectors.entries.joinToString(", ") { "${it.key}=${it.value}" })
            }
        }
        is Ingress -> buildMap {
            resource.spec?.ingressClassName?.let { put("Ingress Class", it) }
            resource.spec?.defaultBackend?.service?.let { svc ->
                put("Default Backend", "${svc.name}:${svc.port?.number ?: svc.port?.name ?: ""}")
            }
            resource.spec?.tls?.let { tlsList ->
                put("TLS", tlsList.joinToString(", ") { tls ->
                    "${tls.hosts?.joinToString(", ") ?: ""} (${tls.secretName ?: "no secret"})"
                })
            }
            resource.spec?.rules?.forEachIndexed { i, rule ->
                val host = rule.host ?: "*"
                val paths = rule.http?.paths?.joinToString(", ") { path ->
                    val backend = path.backend?.service
                    "${path.path ?: "/"} -> ${backend?.name ?: ""}:${backend?.port?.number ?: backend?.port?.name ?: ""}"
                } ?: ""
                put("Rule ${i + 1}", "$host: $paths")
            }
        }
        is ServiceAccount -> buildMap {
            resource.secrets?.let { secrets ->
                put("Secrets", secrets.joinToString(", ") { it.name ?: "" })
            }
            put("Automount Token", "${resource.automountServiceAccountToken ?: true}")
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
