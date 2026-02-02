package dev.nutting.kexplore.data.model

data class CrdDefinition(
    val name: String,
    val group: String,
    val kind: String,
    val plural: String,
    val scope: String,
    val versions: List<String>,
    val categories: List<String> = emptyList(),
)
