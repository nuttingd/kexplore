package dev.nutting.kexplore.data.portforward

enum class TargetType { Pod, Service }

enum class ForwardStatus { Starting, Active, Failed, Stopped }

data class PortForwardSession(
    val id: String,
    val connectionId: String,
    val namespace: String,
    val podName: String,
    val remotePort: Int,
    val localPort: Int,
    val targetType: TargetType,
    val serviceName: String? = null,
    val status: ForwardStatus = ForwardStatus.Starting,
    val error: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
)
