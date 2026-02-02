package dev.nutting.kexplore.ui.screen.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nutting.kexplore.data.model.ContentState
import dev.nutting.kexplore.data.model.DependencyNode
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.ui.components.ContentStateHost
import dev.nutting.kexplore.ui.components.StatusChip

@Composable
fun DependencyTab(
    dependencies: ContentState<DependencyNode>,
    onNavigateToRelated: (namespace: String, kind: ResourceType, name: String) -> Unit,
    onRetry: () -> Unit = {},
) {
    ContentStateHost(state = dependencies, onRetry = onRetry) { root ->
        val flatNodes = remember(root) {
            mutableListOf<Pair<Int, DependencyNode>>().also { flattenTree(root, 0, it) }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(flatNodes, key = { (_, node) -> "${node.kind}/${node.namespace}/${node.name}" }) { (depth, node) ->
                DependencyNodeRow(
                    node = node,
                    depth = depth,
                    onClick = {
                        val type = kindToResourceType(node.kind)
                        if (type != null) {
                            onNavigateToRelated(node.namespace, type, node.name)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DependencyNodeRow(
    node: DependencyNode,
    depth: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = (depth * 24).dp, top = 8.dp, bottom = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Connector line indicator
        if (depth > 0) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(1.dp)
                    .padding(end = 8.dp),
            )
            Text(
                text = "└─ ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Kind badge
        Text(
            text = node.kind,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp),
        )

        // Name
        Text(
            text = node.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )

        // Ready count
        node.readyCount?.let { count ->
            Text(
                text = count,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        // Status
        StatusChip(status = node.status)
    }
}

private fun flattenTree(
    node: DependencyNode,
    depth: Int,
    result: MutableList<Pair<Int, DependencyNode>>,
) {
    result.add(depth to node)
    node.children.forEach { child ->
        flattenTree(child, depth + 1, result)
    }
}

private fun kindToResourceType(kind: String): ResourceType? = when (kind) {
    "Deployment" -> ResourceType.Deployment
    "ReplicaSet" -> ResourceType.ReplicaSet
    "StatefulSet" -> ResourceType.StatefulSet
    "DaemonSet" -> ResourceType.DaemonSet
    "Pod" -> ResourceType.Pod
    "Service" -> ResourceType.Service
    "Ingress" -> ResourceType.Ingress
    else -> null
}
