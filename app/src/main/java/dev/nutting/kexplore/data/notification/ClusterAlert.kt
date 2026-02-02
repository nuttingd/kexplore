package dev.nutting.kexplore.data.notification

enum class AlertType {
    PodCrashLoop, PodFailed, NodeNotReady, DeploymentDegraded
}

data class ClusterAlert(
    val id: String,
    val connectionId: String,
    val type: AlertType,
    val resourceName: String,
    val namespace: String?,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
)
