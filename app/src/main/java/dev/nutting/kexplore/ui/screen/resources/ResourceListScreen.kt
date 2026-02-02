package dev.nutting.kexplore.ui.screen.resources

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import dev.nutting.kexplore.ui.components.ScaleDialog
import dev.nutting.kexplore.ui.components.SearchFilterBar
import dev.nutting.kexplore.ui.components.SwipeToDeleteContainer
import dev.nutting.kexplore.util.ErrorMapper
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Confirmation dialog state
    var confirmDeleteTarget by remember { mutableStateOf<ResourceSummary?>(null) }
    var confirmRestartTarget by remember { mutableStateOf<ResourceSummary?>(null) }
    var confirmTriggerTarget by remember { mutableStateOf<ResourceSummary?>(null) }
    var scaleTarget by remember { mutableStateOf<ResourceSummary?>(null) }

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

    // Delete confirmation dialog
    confirmDeleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDeleteTarget = null },
            title = { Text("Delete ${target.kind.displayName}") },
            text = { Text("Delete ${target.kind.displayName} '${target.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    val t = target
                    confirmDeleteTarget = null
                    scope.launch {
                        try {
                            repository?.deleteResource(t.namespace, t.kind, t.name)
                            snackbarHostState.showSnackbar("${t.kind.displayName} '${t.name}' deleted")
                            listViewModel.loadResources(repository, effectiveNamespace, selectedType, isConnected, connectionError, isRefresh = true)
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(ErrorMapper.map(e))
                        }
                    }
                }) {
                    Text("Delete", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Restart confirmation dialog
    confirmRestartTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmRestartTarget = null },
            title = { Text("Restart ${target.kind.displayName}") },
            text = { Text("Restart ${target.kind.displayName} '${target.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    val t = target
                    confirmRestartTarget = null
                    scope.launch {
                        try {
                            repository?.restartResource(t.namespace, t.kind, t.name)
                            snackbarHostState.showSnackbar("${t.kind.displayName} '${t.name}' restarting")
                            listViewModel.loadResources(repository, effectiveNamespace, selectedType, isConnected, connectionError, isRefresh = true)
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(ErrorMapper.map(e))
                        }
                    }
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestartTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Trigger confirmation dialog
    confirmTriggerTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmTriggerTarget = null },
            title = { Text("Trigger Job") },
            text = { Text("Create a Job from CronJob '${target.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    val t = target
                    confirmTriggerTarget = null
                    scope.launch {
                        try {
                            val jobName = repository?.triggerCronJob(t.namespace, t.name)
                            snackbarHostState.showSnackbar("Job '$jobName' created")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(ErrorMapper.map(e))
                        }
                    }
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmTriggerTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Scale dialog
    scaleTarget?.let { target ->
        ScaleDialog(
            currentReplicas = run {
                val ready = target.readyCount ?: "1/1"
                val parts = ready.split("/")
                parts.lastOrNull()?.trim()?.toIntOrNull() ?: 1
            },
            onDismiss = { scaleTarget = null },
            onConfirm = { replicas ->
                val t = target
                scaleTarget = null
                scope.launch {
                    try {
                        repository?.scaleResource(t.namespace, t.kind, t.name, replicas)
                        snackbarHostState.showSnackbar("Scaled '${t.name}' to $replicas replicas")
                        listViewModel.loadResources(repository, effectiveNamespace, selectedType, isConnected, connectionError, isRefresh = true)
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(ErrorMapper.map(e))
                    }
                }
            },
        )
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

        SnackbarHost(snackbarHostState)

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
                val filtered by remember(items, searchQuery, statusFilters, labelFilter) {
                    derivedStateOf {
                        items.filter { summary ->
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
                    }
                }
                if (filtered.isEmpty()) {
                    EmptyContent(message = "No ${selectedType.pluralName} found")
                } else {
                    LazyColumn {
                        items(filtered, key = { "${it.namespace}/${it.name}" }) { summary ->
                            val canDelete = summary.kind == ResourceType.Pod
                            val canScale = summary.kind.canScale
                            val canRestart = summary.kind.canRestart
                            val canTrigger = summary.kind.canTrigger

                            val itemContent = @Composable {
                                ResourceListItem(
                                    summary = summary,
                                    onClick = { onResourceClick(summary) },
                                    onDelete = if (canDelete || canScale || canRestart || canTrigger) {
                                        { confirmDeleteTarget = summary }
                                    } else null,
                                    onScale = if (canScale) {
                                        { scaleTarget = summary }
                                    } else null,
                                    onRestart = if (canRestart) {
                                        { confirmRestartTarget = summary }
                                    } else null,
                                    onTrigger = if (canTrigger) {
                                        { confirmTriggerTarget = summary }
                                    } else null,
                                )
                            }

                            if (canDelete) {
                                SwipeToDeleteContainer(
                                    onDelete = { confirmDeleteTarget = summary },
                                ) {
                                    itemContent()
                                }
                            } else {
                                itemContent()
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
