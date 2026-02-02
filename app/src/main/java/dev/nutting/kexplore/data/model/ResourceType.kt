package dev.nutting.kexplore.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.ui.graphics.vector.ImageVector

enum class ResourceCategory {
    Workloads, Network, Config, Storage, Cluster
}

enum class ResourceType(
    val displayName: String,
    val pluralName: String,
    val apiName: String,
    val icon: ImageVector,
    val category: ResourceCategory,
    val isClusterScoped: Boolean = false,
) {
    Pod("Pod", "Pods", "pods", Icons.Default.ViewInAr, ResourceCategory.Workloads),
    Deployment("Deployment", "Deployments", "deployments", Icons.Default.Apps, ResourceCategory.Workloads),
    ReplicaSet("ReplicaSet", "ReplicaSets", "replicasets", Icons.Default.ContentCopy, ResourceCategory.Workloads),
    StatefulSet("StatefulSet", "StatefulSets", "statefulsets", Icons.Default.Dns, ResourceCategory.Workloads),
    DaemonSet("DaemonSet", "DaemonSets", "daemonsets", Icons.Default.Widgets, ResourceCategory.Workloads),
    Job("Job", "Jobs", "jobs", Icons.Default.WorkOutline, ResourceCategory.Workloads),
    CronJob("CronJob", "CronJobs", "cronjobs", Icons.Default.Schedule, ResourceCategory.Workloads),
    Event("Event", "Events", "events", Icons.Default.Event, ResourceCategory.Workloads),
    HorizontalPodAutoscaler("HPA", "HPAs", "horizontalpodautoscalers", Icons.Default.Scale, ResourceCategory.Workloads),

    Service("Service", "Services", "services", Icons.Default.Lan, ResourceCategory.Network),
    Ingress("Ingress", "Ingresses", "ingresses", Icons.Default.Hub, ResourceCategory.Network),
    NetworkPolicy("NetworkPolicy", "NetworkPolicies", "networkpolicies", Icons.Default.Policy, ResourceCategory.Network),
    Endpoints("Endpoints", "Endpoints", "endpoints", Icons.Default.AccountTree, ResourceCategory.Network),

    ConfigMap("ConfigMap", "ConfigMaps", "configmaps", Icons.Default.Settings, ResourceCategory.Config),
    Secret("Secret", "Secrets", "secrets", Icons.Default.Key, ResourceCategory.Config),
    ServiceAccount("ServiceAccount", "ServiceAccounts", "serviceaccounts", Icons.Default.Person, ResourceCategory.Config),
    Role("Role", "Roles", "roles", Icons.Default.Security, ResourceCategory.Config),
    RoleBinding("RoleBinding", "RoleBindings", "rolebindings", Icons.Default.Lock, ResourceCategory.Config),

    PersistentVolumeClaim("PVC", "PVCs", "persistentvolumeclaims", Icons.Default.Storage, ResourceCategory.Storage),
    PersistentVolume("PV", "PVs", "persistentvolumes", Icons.Default.Storage, ResourceCategory.Storage, isClusterScoped = true),
    StorageClass("StorageClass", "StorageClasses", "storageclasses", Icons.Default.Speed, ResourceCategory.Storage, isClusterScoped = true),

    Node("Node", "Nodes", "nodes", Icons.Default.Memory, ResourceCategory.Cluster, isClusterScoped = true),
    Namespace("Namespace", "Namespaces", "namespaces", Icons.Default.DataUsage, ResourceCategory.Cluster, isClusterScoped = true),
    ClusterRole("ClusterRole", "ClusterRoles", "clusterroles", Icons.Default.Security, ResourceCategory.Cluster, isClusterScoped = true),
    ClusterRoleBinding("ClusterRoleBinding", "ClusterRoleBindings", "clusterrolebindings", Icons.Default.Lock, ResourceCategory.Cluster, isClusterScoped = true),
    ResourceQuota("ResourceQuota", "ResourceQuotas", "resourcequotas", Icons.Default.Tune, ResourceCategory.Cluster),
    LimitRange("LimitRange", "LimitRanges", "limitranges", Icons.AutoMirrored.Filled.Rule, ResourceCategory.Cluster);

    companion object {
        fun forCategory(category: ResourceCategory): List<ResourceType> =
            entries.filter { it.category == category }
    }
}
