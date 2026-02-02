package dev.nutting.kexplore.data.model

data class DependencyNode(
    val name: String,
    val namespace: String,
    val kind: String,
    val status: ResourceStatus,
    val readyCount: String? = null,
    val children: List<DependencyNode> = emptyList(),
)
