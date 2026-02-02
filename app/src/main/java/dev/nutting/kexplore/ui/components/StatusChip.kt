package dev.nutting.kexplore.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nutting.kexplore.data.model.ResourceStatus
import dev.nutting.kexplore.ui.theme.StatusColors

@Composable
fun StatusChip(status: ResourceStatus, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Circle,
                contentDescription = null,
                tint = StatusColors.forStatus(status),
                modifier = Modifier.size(8.dp).padding(end = 4.dp),
            )
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
