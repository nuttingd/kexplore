package dev.nutting.kexplore.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import dev.nutting.kexplore.data.model.ResourceCategory

enum class BottomTab(
    val label: String,
    val icon: ImageVector,
    val category: ResourceCategory,
) {
    Workloads("Workloads", Icons.Default.Widgets, ResourceCategory.Workloads),
    Network("Network", Icons.Default.Lan, ResourceCategory.Network),
    Config("Config", Icons.Default.Settings, ResourceCategory.Config),
    Storage("Storage", Icons.Default.Storage, ResourceCategory.Storage),
    Cluster("Cluster", Icons.Default.Dns, ResourceCategory.Cluster),
}
