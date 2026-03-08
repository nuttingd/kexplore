package dev.nutting.kexplore.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview
import dev.nutting.kexplore.data.model.ResourceStatus

@Preview
@Composable
internal fun SearchFilterBarPreview() {
    MaterialTheme {
        SearchFilterBar(
            query = "",
            onQueryChange = {},
        )
    }
}

@Preview
@Composable
internal fun SearchFilterBarWithFiltersPreview() {
    MaterialTheme {
        SearchFilterBar(
            query = "my-resource",
            onQueryChange = {},
            statusFilters = setOf(ResourceStatus.Running, ResourceStatus.Failed),
            onStatusFilterToggle = {},
            labelFilter = "app=test",
            onLabelFilterChange = {},
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search resources...",
    statusFilters: Set<ResourceStatus> = emptySet(),
    onStatusFilterToggle: (ResourceStatus) -> Unit = {},
    labelFilter: String = "",
    onLabelFilterChange: (String) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )

        // Status filter chips
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val filterableStatuses = listOf(
                ResourceStatus.Running,
                ResourceStatus.Pending,
                ResourceStatus.Failed,
                ResourceStatus.Succeeded,
            )
            filterableStatuses.forEach { status ->
                FilterChip(
                    selected = status in statusFilters,
                    onClick = { onStatusFilterToggle(status) },
                    label = { Text(status.label) },
                )
            }
        }

        // Label filter
        if (labelFilter.isNotEmpty() || statusFilters.isNotEmpty()) {
            OutlinedTextField(
                value = labelFilter,
                onValueChange = onLabelFilterChange,
                placeholder = { Text("Filter by label (key=value)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}
