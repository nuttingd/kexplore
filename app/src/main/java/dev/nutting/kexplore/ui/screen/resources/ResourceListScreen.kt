package dev.nutting.kexplore.ui.screen.resources

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.data.model.ResourceCategory
import dev.nutting.kexplore.data.model.ResourceStatus
import dev.nutting.kexplore.data.model.ResourceSummary
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.ui.components.ContentStateHost
import dev.nutting.kexplore.ui.components.EmptyContent
import dev.nutting.kexplore.ui.components.ResourceListItem
import dev.nutting.kexplore.ui.components.ResourceTypeChipRow
import dev.nutting.kexplore.ui.components.SearchFilterBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceListScreen(
    repository: KubernetesRepository?,
    namespace: String,
    category: ResourceCategory,
    isConnected: Boolean,
    connectionError: String?,
    modifier: Modifier = Modifier,
    onResourceClick: (ResourceSummary) -> Unit,
    listViewModel: ResourceListViewModel,
) {
    val types = ResourceType.forCategory(category)
    var selectedType by remember(category) { mutableStateOf(types.first()) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilters by remember { mutableStateOf(emptySet<ResourceStatus>()) }
    var labelFilter by remember { mutableStateOf("") }
    val listState by listViewModel.state.collectAsState()

    val effectiveNamespace = if (selectedType.isClusterScoped) "" else namespace

    // Load resources on type/namespace/connection change
    LaunchedEffect(selectedType, effectiveNamespace, isConnected, repository) {
        listViewModel.loadResources(repository, effectiveNamespace, selectedType, isConnected, connectionError)
    }

    // Auto-refresh while screen is visible
    LaunchedEffect(selectedType, effectiveNamespace, isConnected, repository) {
        listViewModel.startAutoRefresh(repository, effectiveNamespace, selectedType, isConnected, connectionError)
    }

    DisposableEffect(Unit) {
        onDispose { listViewModel.stopAutoRefresh() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ResourceTypeChipRow(
            types = types,
            selected = selectedType,
            onSelect = { selectedType = it },
        )

        SearchFilterBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            statusFilters = statusFilters,
            onStatusFilterToggle = { status ->
                statusFilters = if (status in statusFilters) {
                    statusFilters - status
                } else {
                    statusFilters + status
                }
            },
            labelFilter = labelFilter,
            onLabelFilterChange = { labelFilter = it },
        )

        PullToRefreshBox(
            isRefreshing = listState.isRefreshing,
            onRefresh = {
                listViewModel.loadResources(repository, effectiveNamespace, selectedType, isConnected, connectionError, isRefresh = true)
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            ContentStateHost(
                state = listState.resources,
                onRetry = {
                    listViewModel.loadResources(repository, effectiveNamespace, selectedType, isConnected, connectionError, isRefresh = true)
                },
            ) { items ->
                val filtered = items.filter { summary ->
                    val matchesName = searchQuery.isBlank() ||
                        summary.name.contains(searchQuery, ignoreCase = true)
                    val matchesStatus = statusFilters.isEmpty() ||
                        summary.status in statusFilters
                    val matchesLabel = labelFilter.isBlank() || run {
                        val parts = labelFilter.split("=", limit = 2)
                        if (parts.size == 2) {
                            val (key, value) = parts
                            summary.labels.any { (k, v) ->
                                k.contains(key, ignoreCase = true) &&
                                    v.contains(value, ignoreCase = true)
                            }
                        } else {
                            summary.labels.any { (k, v) ->
                                k.contains(labelFilter, ignoreCase = true) ||
                                    v.contains(labelFilter, ignoreCase = true)
                            }
                        }
                    }
                    matchesName && matchesStatus && matchesLabel
                }
                if (filtered.isEmpty()) {
                    EmptyContent(message = "No ${selectedType.pluralName} found")
                } else {
                    LazyColumn {
                        items(filtered, key = { "${it.namespace}/${it.name}" }) { summary ->
                            ResourceListItem(
                                summary = summary,
                                onClick = { onResourceClick(summary) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
