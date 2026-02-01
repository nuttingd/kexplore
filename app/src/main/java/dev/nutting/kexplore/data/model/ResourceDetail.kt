package dev.nutting.kexplore.data.model

data class ResourceDetail(
    val name: String,
    val namespace: String,
    val kind: ResourceType,
    val status: ResourceStatus,
    val uid: String,
    val creationTimestamp: String,
    val labels: Map<String, String> = emptyMap(),
    val annotations: Map<String, String> = emptyMap(),
    val conditions: List<Condition> = emptyList(),
    val spec: Map<String, String> = emptyMap(),
    val containers: List<ContainerInfo> = emptyList(),
)

data class Condition(
    val type: String,
    val status: String,
    val reason: String? = null,
    val message: String? = null,
    val lastTransitionTime: String? = null,
)

data class ContainerInfo(
    val name: String,
    val image: String,
    val ready: Boolean = false,
    val restartCount: Int = 0,
    val state: String = "Unknown",
)
