package dev.nutting.kexplore.ui.screen.resources

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.data.model.ContentState
import dev.nutting.kexplore.data.model.ResourceCategory
import dev.nutting.kexplore.data.model.ResourceSummary
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.ui.components.ContentStateHost
import dev.nutting.kexplore.ui.components.EmptyContent
import dev.nutting.kexplore.ui.components.ResourceListItem
import dev.nutting.kexplore.ui.components.ResourceTypeChipRow
import dev.nutting.kexplore.ui.components.SearchFilterBar
import dev.nutting.kexplore.util.ErrorMapper
import kotlinx.coroutines.delay

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
) {
    val types = ResourceType.forCategory(category)
    var selectedType by remember(category) { mutableStateOf(types.first()) }
    var resources by remember { mutableStateOf<ContentState<List<ResourceSummary>>>(ContentState.Loading) }
    var searchQuery by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Load resources on type/namespace/connection change, or manual refresh
    LaunchedEffect(selectedType, namespace, isConnected, refreshTrigger) {
        if (!isConnected || repository == null) {
            resources = if (connectionError != null) {
                ContentState.Error(connectionError)
            } else {
                ContentState.Loading
            }
            return@LaunchedEffect
        }
        if (!isRefreshing) {
            resources = ContentState.Loading
        }
        resources = try {
            ContentState.Success(repository.getResources(namespace, selectedType))
        } catch (e: Exception) {
            ContentState.Error(ErrorMapper.map(e))
        }
        isRefreshing = false
    }

    // Auto-refresh every 30s while screen is visible
    LaunchedEffect(selectedType, namespace, isConnected) {
        if (!isConnected || repository == null) return@LaunchedEffect
        while (true) {
            delay(30_000)
            refreshTrigger++
        }
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
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                refreshTrigger++
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            ContentStateHost(
                state = resources,
                onRetry = {
                    isRefreshing = true
                    refreshTrigger++
                },
            ) { items ->
                val filtered = if (searchQuery.isBlank()) items else {
                    items.filter { it.name.contains(searchQuery, ignoreCase = true) }
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
