package dev.nutting.kexplore.data.model

data class ResourceSummary(
    val name: String,
    val namespace: String,
    val kind: ResourceType,
    val status: ResourceStatus,
    val age: String,
    val labels: Map<String, String> = emptyMap(),
    val readyCount: String? = null,
    val restarts: Int? = null,
)
