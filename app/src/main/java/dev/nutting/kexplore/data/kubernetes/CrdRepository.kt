package dev.nutting.kexplore.data.kubernetes

import dev.nutting.kexplore.data.model.Condition
import dev.nutting.kexplore.data.model.CrdDefinition
import dev.nutting.kexplore.data.model.CustomResourceSummary
import dev.nutting.kexplore.data.model.ResourceStatus
import dev.nutting.kexplore.util.DateFormatter
import io.fabric8.kubernetes.api.model.GenericKubernetesResource
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext
import io.fabric8.kubernetes.client.utils.Serialization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CrdRepository(private val client: KubernetesClient) {

    suspend fun listCrds(): List<CrdDefinition> = withContext(Dispatchers.IO) {
        client.apiextensions().v1().customResourceDefinitions().list().items.map { crd ->
            val spec = crd.spec
            CrdDefinition(
                name = crd.metadata.name,
                group = spec.group,
                kind = spec.names.kind,
                plural = spec.names.plural,
                scope = spec.scope,
                versions = spec.versions.map { it.name },
                categories = spec.names.categories ?: emptyList(),
            )
        }
    }

    suspend fun listInstances(
        crd: CrdDefinition,
        namespace: String?,
    ): List<CustomResourceSummary> = withContext(Dispatchers.IO) {
        val context = buildContext(crd)
        val resources = if (crd.scope == "Namespaced" && !namespace.isNullOrEmpty()) {
            client.genericKubernetesResources(context)
                .inNamespace(namespace).list().items
        } else {
            client.genericKubernetesResources(context)
                .inAnyNamespace().list().items
        }
        resources.map { toSummary(it, crd.kind) }
    }

    suspend fun getInstance(
        crd: CrdDefinition,
        namespace: String?,
        name: String,
    ): GenericKubernetesResource = withContext(Dispatchers.IO) {
        val context = buildContext(crd)
        val resource = if (crd.scope == "Namespaced" && !namespace.isNullOrEmpty()) {
            client.genericKubernetesResources(context)
                .inNamespace(namespace).withName(name).get()
        } else {
            client.genericKubernetesResources(context)
                .withName(name).get()
        }
        resource ?: throw NoSuchElementException("${crd.kind} '$name' not found")
    }

    suspend fun getInstanceYaml(
        crd: CrdDefinition,
        namespace: String?,
        name: String,
    ): String = withContext(Dispatchers.IO) {
        val resource = getInstance(crd, namespace, name)
        Serialization.asYaml(resource)
    }

    private fun buildContext(crd: CrdDefinition): CustomResourceDefinitionContext =
        CustomResourceDefinitionContext.Builder()
            .withGroup(crd.group)
            .withVersion(crd.versions.firstOrNull() ?: "v1")
            .withPlural(crd.plural)
            .withScope(crd.scope)
            .build()

    private fun toSummary(
        resource: GenericKubernetesResource,
        kind: String,
    ): CustomResourceSummary {
        val status = deriveStatus(resource)
        val conditions = deriveConditions(resource)
        return CustomResourceSummary(
            name = resource.metadata.name,
            namespace = resource.metadata.namespace,
            crdKind = kind,
            status = status,
            age = DateFormatter.age(resource.metadata.creationTimestamp),
            labels = resource.metadata.labels ?: emptyMap(),
            conditions = conditions,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun deriveStatus(resource: GenericKubernetesResource): ResourceStatus {
        val statusMap = resource.additionalProperties["status"] as? Map<String, Any?> ?: return ResourceStatus.Unknown
        // Check for phase string
        val phase = statusMap["phase"] as? String
        if (phase != null) {
            return when (phase.lowercase()) {
                "running" -> ResourceStatus.Running
                "active", "ready", "bound", "available", "healthy" -> ResourceStatus.Running
                "pending", "provisioning" -> ResourceStatus.Pending
                "failed", "error" -> ResourceStatus.Failed
                "succeeded", "complete" -> ResourceStatus.Succeeded
                "terminating", "deleting" -> ResourceStatus.Terminating
                else -> ResourceStatus.Unknown
            }
        }
        // Check for conditions list
        val conditions = statusMap["conditions"] as? List<Map<String, Any?>>
        if (conditions != null) {
            val ready = conditions.find { it["type"] == "Ready" }
            if (ready != null) {
                return if (ready["status"] == "True") ResourceStatus.Running else ResourceStatus.Pending
            }
        }
        return ResourceStatus.Unknown
    }

    @Suppress("UNCHECKED_CAST")
    private fun deriveConditions(resource: GenericKubernetesResource): List<Condition> {
        val statusMap = resource.additionalProperties["status"] as? Map<String, Any?> ?: return emptyList()
        val conditions = statusMap["conditions"] as? List<Map<String, Any?>> ?: return emptyList()
        return conditions.mapNotNull { c ->
            val type = c["type"] as? String ?: return@mapNotNull null
            val status = c["status"] as? String ?: "Unknown"
            Condition(
                type = type,
                status = status,
                reason = c["reason"] as? String,
                message = c["message"] as? String,
                lastTransitionTime = c["lastTransitionTime"] as? String,
            )
        }
    }
}
