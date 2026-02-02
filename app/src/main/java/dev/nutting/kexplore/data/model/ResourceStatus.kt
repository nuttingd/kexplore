package dev.nutting.kexplore.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ResourceStatus(val label: String) {
    Running("Running"),
    Pending("Pending"),
    Failed("Failed"),
    Succeeded("Succeeded"),
    Terminating("Terminating"),
    Unknown("Unknown"),
}
