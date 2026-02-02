package dev.nutting.kexplore.ui.theme

import androidx.compose.ui.graphics.Color
import dev.nutting.kexplore.data.model.ResourceStatus

object StatusColors {
    val running = Color(0xFF4CAF50)
    val pending = Color(0xFFFFC107)
    val failed = Color(0xFFF44336)
    val succeeded = Color(0xFF2196F3)
    val terminating = Color(0xFFFF9800)
    val unknown = Color(0xFF9E9E9E)

    fun forStatus(status: ResourceStatus): Color = when (status) {
        ResourceStatus.Running -> running
        ResourceStatus.Pending -> pending
        ResourceStatus.Failed -> failed
        ResourceStatus.Succeeded -> succeeded
        ResourceStatus.Terminating -> terminating
        ResourceStatus.Unknown -> unknown
    }
}
