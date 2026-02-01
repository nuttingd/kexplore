package dev.nutting.kexplore.data.model

import androidx.compose.ui.graphics.Color

enum class ResourceStatus(val color: Color, val label: String) {
    Running(Color(0xFF4CAF50), "Running"),
    Pending(Color(0xFFFFC107), "Pending"),
    Failed(Color(0xFFF44336), "Failed"),
    Succeeded(Color(0xFF2196F3), "Succeeded"),
    Terminating(Color(0xFFFF9800), "Terminating"),
    Unknown(Color(0xFF9E9E9E), "Unknown");
}
