package dev.nutting.kexplore.widget

import kotlinx.serialization.Serializable

@Serializable
data class WidgetState(
    val clusterName: String = "",
    val totalPods: Int = 0,
    val runningPods: Int = 0,
    val failedPods: Int = 0,
    val totalNodes: Int = 0,
    val readyNodes: Int = 0,
    val lastUpdated: Long = 0L,
    val error: String? = null,
)
