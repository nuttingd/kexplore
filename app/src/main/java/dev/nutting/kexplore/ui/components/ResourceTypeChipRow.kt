package dev.nutting.kexplore.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview
import dev.nutting.kexplore.data.model.ResourceType

@Preview
@Composable
private fun ResourceTypeChipRowPreview() {
    MaterialTheme {
        ResourceTypeChipRow(
            types = listOf(ResourceType.Pod, ResourceType.Deployment, ResourceType.Service, ResourceType.Ingress),
            selected = ResourceType.Pod,
            onSelect = {},
        )
    }
}

@Composable
fun ResourceTypeChipRow(
    types: List<ResourceType>,
    selected: ResourceType,
    onSelect: (ResourceType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        types.forEach { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelect(type) },
                label = { Text(type.pluralName) },
            )
        }
    }
}
