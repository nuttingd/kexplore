package dev.nutting.kexplore.ui.screen.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.data.model.ResourceSummary
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.ui.components.ContentStateHost
import dev.nutting.kexplore.ui.components.ResourceListItem
import dev.nutting.kexplore.ui.components.SectionHeader
import dev.nutting.kexplore.ui.theme.StatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDashboardScreen(
    repository: KubernetesRepository?,
    namespace: String,
    onBack: () -> Unit,
    onNavigateToDetail: (namespace: String, kind: ResourceType, name: String) -> Unit,
    healthViewModel: HealthDashboardViewModel = viewModel(),
) {
    val state by healthViewModel.state.collectAsState()

    LaunchedEffect(repository, namespace) {
        healthViewModel.loadHealth(repository, namespace)
        healthViewModel.startAutoRefresh(repository, namespace)
    }

    DisposableEffect(Unit) {
        onDispose { healthViewModel.stopAutoRefresh() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cluster Health") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { healthViewModel.loadHealth(repository, namespace, isRefresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ContentStateHost(
                state = state.health,
                onRetry = { healthViewModel.loadHealth(repository, namespace) },
            ) { health ->
                if (health.isHealthy) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusColors.running,
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Everything looks healthy",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (health.failedPods.isNotEmpty()) {
                            item { SectionHeader("Failed Pods (${health.failedPods.size})") }
                            items(health.failedPods, key = { "pod-${it.namespace}/${it.name}" }) { summary ->
                                HealthResourceItem(summary, onNavigateToDetail)
                                HorizontalDivider()
                            }
                        }
                        if (health.unhealthyDeployments.isNotEmpty()) {
                            item { SectionHeader("Unhealthy Deployments (${health.unhealthyDeployments.size})") }
                            items(health.unhealthyDeployments, key = { "deploy-${it.namespace}/${it.name}" }) { summary ->
                                HealthResourceItem(summary, onNavigateToDetail)
                                HorizontalDivider()
                            }
                        }
                        if (health.unhealthyNodes.isNotEmpty()) {
                            item { SectionHeader("Unhealthy Nodes (${health.unhealthyNodes.size})") }
                            items(health.unhealthyNodes, key = { "node-${it.name}" }) { summary ->
                                HealthResourceItem(summary, onNavigateToDetail)
                                HorizontalDivider()
                            }
                        }
                        if (health.warningEvents.isNotEmpty()) {
                            item { SectionHeader("Warning Events (${health.warningEvents.size})") }
                            items(health.warningEvents, key = { "event-${it.namespace}/${it.name}" }) { summary ->
                                HealthResourceItem(summary, onNavigateToDetail)
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthResourceItem(
    summary: ResourceSummary,
    onNavigateToDetail: (namespace: String, kind: ResourceType, name: String) -> Unit,
) {
    ResourceListItem(
        summary = summary,
        onClick = { onNavigateToDetail(summary.namespace, summary.kind, summary.name) },
    )
}
