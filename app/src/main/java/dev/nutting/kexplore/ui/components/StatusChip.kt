package dev.nutting.kexplore.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.nutting.kexplore.data.model.ResourceStatus
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle

@Composable
fun StatusChip(status: ResourceStatus, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = {},
        label = { Text(status.label) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Circle,
                contentDescription = null,
                tint = status.color,
                modifier = Modifier.size(8.dp),
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = Color.Transparent,
        ),
        modifier = modifier,
    )
}
