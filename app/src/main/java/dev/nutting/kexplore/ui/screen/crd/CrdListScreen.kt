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
import dev.nutting.kexplore.data.model.CrdDefinition
import dev.nutting.kexplore.ui.components.ContentStateHost
import dev.nutting.kexplore.ui.components.EmptyContent
import dev.nutting.kexplore.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrdListScreen(
    crdRepository: CrdRepository?,
    onBack: () -> Unit,
    onCrdClick: (crdName: String) -> Unit,
    viewModel: CrdListViewModel,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Resources") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        CrdListContent(
            crdRepository = crdRepository,
            onCrdClick = onCrdClick,
            viewModel = viewModel,
            modifier = Modifier.padding(padding),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrdListContent(
    crdRepository: CrdRepository?,
    onCrdClick: (crdName: String) -> Unit,
    viewModel: CrdListViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(crdRepository) {
        viewModel.loadCrds(crdRepository)
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search CRDs...") },
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
            onRefresh = { viewModel.loadCrds(crdRepository, isRefresh = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            ContentStateHost(
                state = state.crds,
                onRetry = { viewModel.loadCrds(crdRepository) },
            ) { crds ->
                val filtered by remember(crds, searchQuery) {
                    derivedStateOf {
                        if (searchQuery.isBlank()) crds
                        else crds.filter { crd ->
                            crd.kind.contains(searchQuery, ignoreCase = true) ||
                                crd.group.contains(searchQuery, ignoreCase = true) ||
                                crd.name.contains(searchQuery, ignoreCase = true)
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    EmptyContent(message = "No CRDs found")
                } else {
                    val grouped = remember(filtered) {
                        filtered.groupBy { it.group }.toSortedMap()
                    }
                    LazyColumn {
                        grouped.forEach { (group, definitions) ->
                            item(key = "header_$group") {
                                SectionHeader(group)
                            }
                            items(definitions, key = { it.name }) { crd ->
                                CrdListItem(
                                    crd = crd,
                                    onClick = { onCrdClick(crd.name) },
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
private fun CrdListItem(crd: CrdDefinition, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(crd.kind) },
        supportingContent = {
            Text(
                "${crd.plural} | ${crd.scope} | ${crd.versions.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}
