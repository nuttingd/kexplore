package dev.nutting.kexplore.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import dev.nutting.kexplore.data.model.ResourceSummary
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.data.model.ResourceStatus
import dev.nutting.kexplore.ui.theme.KexploreTextStyles
import dev.nutting.kexplore.ui.theme.getColor

@Preview
@Composable
internal fun ResourceListItemPreview() {
    MaterialTheme {
        ResourceListItem(
            summary = ResourceSummary(
                name = "my-deployment-abc123",
                namespace = "default",
                kind = ResourceType.Deployment,
                status = ResourceStatus.Running,
                age = "2d",
                labels = emptyMap(),
                readyCount = "3/3",
            ),
            onClick = {},
        )
    }
}

@Preview
@Composable
internal fun ResourceListItemFailedPreview() {
    MaterialTheme {
        ResourceListItem(
            summary = ResourceSummary(
                name = "failing-pod-xyz",
                namespace = "default",
                kind = ResourceType.Pod,
                status = ResourceStatus.Failed,
                age = "1h",
                labels = emptyMap(),
                restarts = 5,
            ),
            onClick = {},
            onDelete = {},
            onScale = {},
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ResourceListItem(
    summary: ResourceSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onToggleSelection: (() -> Unit)? = null,
    onEnterSelectionMode: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onScale: (() -> Unit)? = null,
    onRestart: (() -> Unit)? = null,
    onTrigger: (() -> Unit)? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val hasActions = onDelete != null || onScale != null || onRestart != null || onTrigger != null
    val isDarkTheme = isSystemInDarkTheme()

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    ListItem(
        colors = ListItemDefaults.colors(containerColor = containerColor),
        headlineContent = { Text(summary.name) },
        supportingContent = {
            val parts = mutableListOf<String>()
            summary.readyCount?.let { parts.add("Ready: $it") }
            summary.restarts?.let { if (it > 0) parts.add("Restarts: $it") }
            parts.add(summary.age)
            Text(
                text = parts.joinToString(" | "),
                style = KexploreTextStyles.timestamp,
            )
        },
        leadingContent = {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                )
            } else {
                Icon(
                    imageVector = summary.kind.icon,
                    contentDescription = summary.kind.displayName,
                    tint = summary.kind.category.getColor(isDarkTheme),
                )
            }
        },
        trailingContent = {
            StatusChip(status = summary.status)
            if (hasActions && !selectionMode) {
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    if (onScale != null) {
                        DropdownMenuItem(
                            text = { Text("Scale") },
                            onClick = { menuExpanded = false; onScale() },
                            leadingIcon = { Icon(Icons.Default.SwapVert, contentDescription = null) },
                        )
                    }
                    if (onRestart != null) {
                        DropdownMenuItem(
                            text = { Text("Restart") },
                            onClick = { menuExpanded = false; onRestart() },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        )
                    }
                    if (onTrigger != null) {
                        DropdownMenuItem(
                            text = { Text("Trigger Job") },
                            onClick = { menuExpanded = false; onTrigger() },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                        )
                    }
                    if (onDelete != null) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onDelete() },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                        )
                    }
                }
            }
        },
        modifier = modifier.combinedClickable(
            role = Role.Button,
            onClick = if (selectionMode) {
                { onToggleSelection?.invoke(); Unit }
            } else {
                onClick
            },
            onLongClick = if (selectionMode) {
                { onToggleSelection?.invoke() }
            } else if (onEnterSelectionMode != null) {
                { onEnterSelectionMode() }
            } else if (hasActions) {
                { menuExpanded = true }
            } else null,
        ),
    )
}
