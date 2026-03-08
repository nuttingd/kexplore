package dev.nutting.kexplore.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.ui.theme.getColor

@Preview
@Composable
internal fun ResourceTypeChipRowPreview() {
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
    val isDarkTheme = isSystemInDarkTheme()
    
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        types.forEach { type ->
            val isSelected = type == selected
            val categoryColor = type.category.getColor(isDarkTheme)
            
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(type) },
                label = { Text(type.pluralName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = categoryColor.copy(alpha = 0.2f),
                    selectedLabelColor = categoryColor,
                ),
            )
        }
    }
}
