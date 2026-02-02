package dev.nutting.kexplore.ui.screen.crd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.nutting.kexplore.data.kubernetes.CrdRepository
import dev.nutting.kexplore.data.model.ContentState
import dev.nutting.kexplore.data.model.CustomResourceSummary
import dev.nutting.kexplore.ui.components.ContentStateHost
import dev.nutting.kexplore.ui.components.EmptyContent
import dev.nutting.kexplore.ui.components.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrdInstanceListScreen(
    crdRepository: CrdRepository?,
    crdName: String,
    namespace: String?,
    onBack: () -> Unit,
    onInstanceClick: (namespace: String?, name: String) -> Unit,
    viewModel: CrdInstanceListViewModel,
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val title = when (val crd = state.crd) {
        is ContentState.Success -> crd.data.kind
        else -> crdName
    }

    LaunchedEffect(crdRepository, crdName, namespace) {
        viewModel.loadInstances(crdRepository, crdName, namespace)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search instances...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = {
                    viewModel.loadInstances(crdRepository, crdName, namespace, isRefresh = true)
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                ContentStateHost(
                    state = state.instances,
                    onRetry = { viewModel.loadInstances(crdRepository, crdName, namespace) },
                ) { instances ->
                    val filtered by remember(instances, searchQuery) {
                        derivedStateOf {
                            if (searchQuery.isBlank()) instances
                            else instances.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        }
                    }
                    if (filtered.isEmpty()) {
                        EmptyContent(message = "No instances found")
                    } else {
                        LazyColumn {
                            items(filtered, key = { "${it.namespace ?: ""}/${it.name}" }) { instance ->
                                InstanceListItem(
                                    instance = instance,
                                    onClick = { onInstanceClick(instance.namespace, instance.name) },
                                )
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
private fun InstanceListItem(instance: CustomResourceSummary, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(instance.name) },
        supportingContent = {
            val parts = buildList {
                instance.namespace?.let { add(it) }
                add(instance.age)
            }
            Text(
                parts.joinToString(" | "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = { StatusChip(status = instance.status) },
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}
