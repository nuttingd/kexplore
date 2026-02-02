package dev.nutting.kexplore.data.model

data class CustomResourceSummary(
    val name: String,
    val namespace: String?,
    val crdKind: String,
    val status: ResourceStatus,
    val age: String,
    val labels: Map<String, String> = emptyMap(),
    val conditions: List<Condition> = emptyList(),
)
