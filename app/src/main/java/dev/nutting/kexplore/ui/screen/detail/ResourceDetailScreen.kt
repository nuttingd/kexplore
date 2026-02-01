package dev.nutting.kexplore.ui.screen.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.data.model.ContentState
import dev.nutting.kexplore.data.model.ResourceDetail
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.ui.components.ContentStateHost
import dev.nutting.kexplore.util.ErrorMapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceDetailScreen(
    repository: KubernetesRepository?,
    namespace: String,
    resourceType: ResourceType,
    resourceName: String,
    onBack: () -> Unit,
    onViewLogs: (container: String) -> Unit,
    onExec: (container: String) -> Unit = {},
) {
    var detail by remember { mutableStateOf<ContentState<ResourceDetail>>(ContentState.Loading) }
    var yaml by remember { mutableStateOf<ContentState<String>>(ContentState.Loading) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = buildList {
        add("Overview")
        add("YAML")
        if (resourceType == ResourceType.Pod) {
            add("Logs")
            add("Exec")
        }
    }

    LaunchedEffect(namespace, resourceType, resourceName) {
        if (repository == null) {
            detail = ContentState.Error("Not connected")
            return@LaunchedEffect
        }
        try {
            detail = ContentState.Success(repository.getResourceDetail(namespace, resourceType, resourceName))
            yaml = ContentState.Success(repository.getResourceYaml(namespace, resourceType, resourceName))
        } catch (e: Exception) {
            detail = ContentState.Error(ErrorMapper.map(e))
            yaml = ContentState.Error(ErrorMapper.map(e))
        }
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

            when (selectedTab) {
                0 -> OverviewTab(detail = detail)
                1 -> YamlTab(yaml = yaml)
                2 -> LogsTab(
                    repository = repository,
                    namespace = namespace,
                    podName = resourceName,
                )
                3 -> ExecTab(onExec = { container ->
                    onExec(container)
                }, detail = detail)
            }
        }
    }
}

@Composable
private fun OverviewTab(detail: ContentState<ResourceDetail>) {
    ContentStateHost(state = detail) { resource ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            DetailSection("Metadata") {
                DetailRow("Name", resource.name)
                DetailRow("Namespace", resource.namespace)
                DetailRow("UID", resource.uid)
                DetailRow("Created", resource.creationTimestamp)
                DetailRow("Status", resource.status.label)
            }

            if (resource.labels.isNotEmpty()) {
                DetailSection("Labels") {
                    resource.labels.forEach { (k, v) ->
                        DetailRow(k, v)
                    }
                }
            }

            if (resource.annotations.isNotEmpty()) {
                DetailSection("Annotations") {
                    resource.annotations.forEach { (k, v) ->
                        DetailRow(k, v)
                    }
                }
            }

            if (resource.spec.isNotEmpty()) {
                DetailSection("Spec") {
                    resource.spec.forEach { (k, v) ->
                        DetailRow(k, v)
                    }
                }
            }

            if (resource.conditions.isNotEmpty()) {
                DetailSection("Conditions") {
                    resource.conditions.forEach { condition ->
                        DetailRow(condition.type, "${condition.status} ${condition.reason ?: ""}")
                    }
                }
            }

            if (resource.containers.isNotEmpty()) {
                DetailSection("Containers") {
                    resource.containers.forEach { container ->
                        DetailRow(container.name, "${container.image} (${container.state})")
                    }
                }
            }
        }
    }
}

@Composable
private fun YamlTab(yaml: ContentState<String>) {
    ContentStateHost(state = yaml) { content ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        ) {
            Text(
                text = content,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LogsTab(
    repository: KubernetesRepository?,
    namespace: String,
    podName: String,
) {
    // Stub - will be fully implemented in Phase 5
    var logs by remember { mutableStateOf<ContentState<List<String>>>(ContentState.Loading) }

    LaunchedEffect(namespace, podName) {
        if (repository == null) {
            logs = ContentState.Error("Not connected")
            return@LaunchedEffect
        }
        try {
            val lines = mutableListOf<String>()
            repository.streamPodLogs(namespace, podName).collect { line ->
                lines.add(line)
                logs = ContentState.Success(lines.toList())
            }
        } catch (e: Exception) {
            logs = ContentState.Error(ErrorMapper.map(e))
        }
    }

    ContentStateHost(state = logs) { lines ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        ) {
            Text(
                text = lines.joinToString("\n"),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth(),
            )
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
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                resource.containers.forEach { container ->
                    androidx.compose.material3.FilledTonalButton(
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

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
    content()
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )
    }
}
