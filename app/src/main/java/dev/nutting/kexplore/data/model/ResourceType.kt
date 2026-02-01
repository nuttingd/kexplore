package dev.nutting.kexplore.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.ui.graphics.vector.ImageVector

enum class ResourceCategory {
    Workloads, Network, Config
}

enum class ResourceType(
    val displayName: String,
    val pluralName: String,
    val apiName: String,
    val icon: ImageVector,
    val category: ResourceCategory
) {
    Pod("Pod", "Pods", "pods", Icons.Default.ViewInAr, ResourceCategory.Workloads),
    Deployment("Deployment", "Deployments", "deployments", Icons.Default.Apps, ResourceCategory.Workloads),
    ReplicaSet("ReplicaSet", "ReplicaSets", "replicasets", Icons.Default.ContentCopy, ResourceCategory.Workloads),
    StatefulSet("StatefulSet", "StatefulSets", "statefulsets", Icons.Default.Dns, ResourceCategory.Workloads),
    DaemonSet("DaemonSet", "DaemonSets", "daemonsets", Icons.Default.Widgets, ResourceCategory.Workloads),
    Job("Job", "Jobs", "jobs", Icons.Default.WorkOutline, ResourceCategory.Workloads),
    CronJob("CronJob", "CronJobs", "cronjobs", Icons.Default.Schedule, ResourceCategory.Workloads),

    Service("Service", "Services", "services", Icons.Default.Lan, ResourceCategory.Network),
    Ingress("Ingress", "Ingresses", "ingresses", Icons.Default.Hub, ResourceCategory.Network),

    ConfigMap("ConfigMap", "ConfigMaps", "configmaps", Icons.Default.Settings, ResourceCategory.Config),
    Secret("Secret", "Secrets", "secrets", Icons.Default.Key, ResourceCategory.Config),
    ServiceAccount("ServiceAccount", "ServiceAccounts", "serviceaccounts", Icons.Default.Person, ResourceCategory.Config),
    PersistentVolumeClaim("PVC", "PVCs", "persistentvolumeclaims", Icons.Default.Storage, ResourceCategory.Config);

    companion object {
        fun forCategory(category: ResourceCategory): List<ResourceType> =
            entries.filter { it.category == category }
    }
}
