package dev.nutting.kexplore.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import dev.nutting.kexplore.data.model.ResourceSummary

@Composable
fun ResourceListItem(
    summary: ResourceSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(summary.name) },
        supportingContent = {
            val parts = mutableListOf<String>()
            summary.readyCount?.let { parts.add("Ready: $it") }
            summary.restarts?.let { if (it > 0) parts.add("Restarts: $it") }
            parts.add(summary.age)
            Text(parts.joinToString(" | "))
        },
        leadingContent = {
            Icon(
                imageVector = summary.kind.icon,
                contentDescription = summary.kind.displayName,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            StatusChip(status = summary.status)
        },
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
    )
}
