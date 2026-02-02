package dev.nutting.kexplore.ui.screen.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.data.kubernetes.MetricsRepository
import dev.nutting.kexplore.data.model.ContainerInfo
import dev.nutting.kexplore.data.model.ContentState
import dev.nutting.kexplore.data.model.ResourceDetail
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.ui.components.ContentStateHost
import dev.nutting.kexplore.ui.components.KeyValueRow
import dev.nutting.kexplore.ui.components.LabelChipGroup
import dev.nutting.kexplore.ui.components.MetadataCard
import dev.nutting.kexplore.ui.components.ScaleDialog
import dev.nutting.kexplore.ui.components.SectionHeader
import dev.nutting.kexplore.ui.components.YamlView
import dev.nutting.kexplore.ui.screen.logs.PodLogsScreen

private enum class ConfirmAction {
    Delete, Restart, Trigger, Cordon, Uncordon
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceDetailScreen(
    repository: KubernetesRepository?,
    metricsRepository: MetricsRepository? = null,
    namespace: String,
    resourceType: ResourceType,
    resourceName: String,
    onBack: () -> Unit,
    onExec: (container: String) -> Unit = {},
    onViewFullScreenLogs: () -> Unit = {},
    onDeleted: (message: String) -> Unit = {},
    onNavigateToRelated: (namespace: String, kind: ResourceType, name: String) -> Unit = { _, _, _ -> },
    detailViewModel: ResourceDetailViewModel,
) {
    val detailState by detailViewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<ConfirmAction?>(null) }
    var showScaleDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val hasMetrics = resourceType == ResourceType.Pod || resourceType == ResourceType.Node
    val hasDependencies = resourceType in setOf(
        ResourceType.Deployment, ResourceType.StatefulSet, ResourceType.DaemonSet,
        ResourceType.ReplicaSet, ResourceType.Service,
    )
    val tabs = buildList {
        add("Overview")
        add("YAML")
        if (hasMetrics) {
            add("Metrics")
        }
        if (hasDependencies) {
            add("Dependencies")
        }
        if (resourceType == ResourceType.Pod) {
            add("Logs")
            add("Exec")
        }
    }

    // Determine if node is currently cordoned
    val isNodeCordoned = remember(detailState.detail) {
        if (resourceType.isNode) {
            val detail = (detailState.detail as? ContentState.Success)?.data
            detail?.spec?.get("Unschedulable") == "true"
        } else false
    }

    LaunchedEffect(namespace, resourceType, resourceName) {
        detailViewModel.loadResource(repository, namespace, resourceType, resourceName)
    }

    // Show snackbar for action results (errors stay on detail, success delete navigates away)
    LaunchedEffect(detailState.actionResult) {
        when (val result = detailState.actionResult) {
            is ActionResult.Error -> {
                snackbarHostState.showSnackbar(result.message)
                detailViewModel.dismissActionResult()
            }
            is ActionResult.Success -> {
                // For non-delete successes, show snackbar on this screen
                snackbarHostState.showSnackbar(result.message)
                detailViewModel.dismissActionResult()
            }
            null -> {}
        }
    }

    // Confirmation dialog
    confirmAction?.let { action ->
        val (title, message) = when (action) {
            ConfirmAction.Delete -> {
                val extra = if (resourceType == ResourceType.Namespace) {
                    "\n\nAll resources in this namespace will be deleted."
                } else ""
                "Delete ${resourceType.displayName}" to
                    "Delete ${resourceType.displayName} '$resourceName'?$extra"
            }
            ConfirmAction.Restart ->
                "Restart ${resourceType.displayName}" to
                    "Restart ${resourceType.displayName} '$resourceName'? This will trigger a rolling restart of all pods."
            ConfirmAction.Trigger ->
                "Trigger Job" to
                    "Create a Job from CronJob '$resourceName'?"
            ConfirmAction.Cordon ->
                "Cordon Node" to
                    "Mark node '$resourceName' as unschedulable? No new pods will be scheduled."
            ConfirmAction.Uncordon ->
                "Uncordon Node" to
                    "Mark node '$resourceName' as schedulable? New pods can be scheduled."
        }
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentAction = action
                        confirmAction = null
                        when (currentAction) {
                            ConfirmAction.Delete -> detailViewModel.deleteResource(repository) {
                                onDeleted("${resourceType.displayName} '$resourceName' deleted")
                            }
                            ConfirmAction.Restart -> detailViewModel.restartResource(repository)
                            ConfirmAction.Trigger -> detailViewModel.triggerCronJob(repository)
                            ConfirmAction.Cordon -> detailViewModel.cordonNode(repository)
                            ConfirmAction.Uncordon -> detailViewModel.uncordonNode(repository)
                        }
                    },
                    enabled = !detailState.actionInProgress,
                ) {
                    Text(
                        if (action == ConfirmAction.Delete) "Delete" else "Confirm",
                        color = if (action == ConfirmAction.Delete) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Scale dialog
    if (showScaleDialog) {
        val currentReplicas = remember(detailState.detail) {
            val detail = (detailState.detail as? ContentState.Success)?.data
            ResourceDetailViewModel.parseDesiredReplicas(detail?.spec?.get("Replicas")) ?: 1
        }
        ScaleDialog(
            currentReplicas = currentReplicas,
            onDismiss = { showScaleDialog = false },
            onConfirm = { replicas ->
                showScaleDialog = false
                detailViewModel.scaleResource(repository, replicas)
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resourceName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        // Scale
                        if (resourceType.canScale) {
                            DropdownMenuItem(
                                text = { Text("Scale") },
                                onClick = {
                                    menuExpanded = false
                                    showScaleDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.SwapVert, contentDescription = null) },
                                enabled = !detailState.actionInProgress,
                            )
                        }
                        // Restart
                        if (resourceType.canRestart) {
                            DropdownMenuItem(
                                text = { Text("Restart") },
                                onClick = {
                                    menuExpanded = false
                                    confirmAction = ConfirmAction.Restart
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                enabled = !detailState.actionInProgress,
                            )
                        }
                        // Trigger Job (CronJob)
                        if (resourceType.canTrigger) {
                            DropdownMenuItem(
                                text = { Text("Trigger Job") },
                                onClick = {
                                    menuExpanded = false
                                    confirmAction = ConfirmAction.Trigger
                                },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                                enabled = !detailState.actionInProgress,
                            )
                        }
                        // Cordon / Uncordon (Node)
                        if (resourceType.isNode) {
                            if (isNodeCordoned) {
                                DropdownMenuItem(
                                    text = { Text("Uncordon") },
                                    onClick = {
                                        menuExpanded = false
                                        confirmAction = ConfirmAction.Uncordon
                                    },
                                    enabled = !detailState.actionInProgress,
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Cordon") },
                                    onClick = {
                                        menuExpanded = false
                                        confirmAction = ConfirmAction.Cordon
                                    },
                                    enabled = !detailState.actionInProgress,
                                )
                            }
                        }
                        // Delete (always available)
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                confirmAction = ConfirmAction.Delete
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            enabled = !detailState.actionInProgress,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            when (tabs.getOrNull(selectedTab)) {
                "Overview" -> OverviewTab(
                    detail = detailState.detail,
                    resourceType = resourceType,
                    onRetry = { detailViewModel.retry(repository) },
                )
                "YAML" -> YamlView(yaml = detailState.yaml)
                "Metrics" -> MetricsTab(
                    metricsRepository = metricsRepository,
                    namespace = namespace,
                    resourceName = resourceName,
                    resourceType = resourceType,
                )
                "Dependencies" -> {
                    LaunchedEffect(Unit) {
                        if (detailState.dependencies == null) {
                            detailViewModel.loadDependencies(repository)
                        }
                    }
                    DependencyTab(
                        dependencies = detailState.dependencies ?: ContentState.Loading,
                        onNavigateToRelated = onNavigateToRelated,
                        onRetry = { detailViewModel.loadDependencies(repository) },
                    )
                }
                "Logs" -> PodLogsScreen(
                    repository = repository,
                    namespace = namespace,
                    podName = resourceName,
                    embedded = true,
                )
                "Exec" -> ExecTab(onExec = { container ->
                    onExec(container)
                }, detail = detailState.detail)
            }
        }
    }
}

@Composable
private fun OverviewTab(
    detail: ContentState<ResourceDetail>,
    resourceType: ResourceType,
    onRetry: () -> Unit = {},
) {
    ContentStateHost(state = detail, onRetry = onRetry) { resource ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            MetadataCard(
                name = resource.name,
                namespace = resource.namespace,
                uid = resource.uid,
                creationTimestamp = resource.creationTimestamp,
                status = resource.status,
            )

            if (resource.labels.isNotEmpty()) {
                SectionHeader("Labels")
                LabelChipGroup(labels = resource.labels)
            }

            if (resource.annotations.isNotEmpty()) {
                SectionHeader("Annotations")
                resource.annotations.forEach { (k, v) ->
                    KeyValueRow(k, v)
                }
            }

            if (resource.spec.isNotEmpty()) {
                SectionHeader(specSectionTitle(resourceType))
                resource.spec.forEach { (k, v) ->
                    KeyValueRow(k, v)
                }
            }

            // Deployment replicas progress bar
            if (resourceType == ResourceType.Deployment) {
                val replicasStr = resource.spec["Replicas"] ?: ""
                val match = Regex("(\\d+) ready / (\\d+) desired").find(replicasStr)
                if (match != null) {
                    val ready = match.groupValues[1].toFloatOrNull() ?: 0f
                    val desired = match.groupValues[2].toFloatOrNull() ?: 1f
                    if (desired > 0f) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (ready / desired).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            if (resource.conditions.isNotEmpty()) {
                SectionHeader("Conditions")
                resource.conditions.forEach { condition ->
                    KeyValueRow(
                        condition.type,
                        buildString {
                            append(condition.status)
                            condition.reason?.let { append(" — $it") }
                        },
                    )
                }
            }

            if (resource.containers.isNotEmpty()) {
                SectionHeader("Containers")
                resource.containers.forEach { container ->
                    ContainerCard(container)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

private fun specSectionTitle(type: ResourceType): String = when (type) {
    ResourceType.ConfigMap -> "Data"
    ResourceType.Secret -> "Data Keys"
    else -> "Spec"
}

@Composable
private fun ContainerCard(container: ContainerInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = container.name,
                style = MaterialTheme.typography.titleSmall,
            )
            KeyValueRow("Image", container.image)
            KeyValueRow("State", "${container.state} (ready: ${container.ready}, restarts: ${container.restartCount})")

            if (container.ports.isNotEmpty()) {
                KeyValueRow("Ports", container.ports.joinToString(", "))
            }
            if (container.env.isNotEmpty()) {
                KeyValueRow("Env", container.env.take(5).joinToString("\n") +
                    if (container.env.size > 5) "\n... +${container.env.size - 5} more" else "")
            }
            container.resources?.let {
                KeyValueRow("Resources", it)
            }
            if (container.mounts.isNotEmpty()) {
                KeyValueRow("Mounts", container.mounts.joinToString("\n"))
            }
        }
    }
}

@Composable
private fun ExecTab(
    onExec: (container: String) -> Unit,
    detail: ContentState<ResourceDetail>,
) {
    ContentStateHost(state = detail) { resource ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (resource.containers.isEmpty()) {
                Text("No containers found")
            } else {
                Text(
                    text = "Select a container to exec into:",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                resource.containers.forEach { container ->
                    FilledTonalButton(
                        onClick = { onExec(container.name) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Text(container.name)
                    }
                }
            }
        }
    }
}
