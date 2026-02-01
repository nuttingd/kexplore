package dev.nutting.kexplore.ui.screen.detail

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.nutting.kexplore.ui.components.SectionHeader
import dev.nutting.kexplore.ui.screen.logs.PodLogsScreen

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
    detailViewModel: ResourceDetailViewModel,
) {
    val detailState by detailViewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val hasMetrics = resourceType == ResourceType.Pod || resourceType == ResourceType.Node
    val tabs = buildList {
        add("Overview")
        add("YAML")
        if (hasMetrics) {
            add("Metrics")
        }
        if (resourceType == ResourceType.Pod) {
            add("Logs")
            add("Exec")
        }
    }

    LaunchedEffect(namespace, resourceType, resourceName) {
        detailViewModel.loadResource(repository, namespace, resourceType, resourceName)
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
            )
        },
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
                "YAML" -> YamlTab(yaml = detailState.yaml)
                "Metrics" -> MetricsTab(
                    metricsRepository = metricsRepository,
                    namespace = namespace,
                    resourceName = resourceName,
                    resourceType = resourceType,
                )
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
private fun YamlTab(yaml: ContentState<String>) {
    ContentStateHost(state = yaml) { content ->
        val lines = remember(content) { content.lines() }
        androidx.compose.foundation.text.selection.SelectionContainer {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            ) {
                items(lines.size) { index ->
                    Text(
                        text = lines[index],
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
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

