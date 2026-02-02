package dev.nutting.kexplore.ui.screen.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.ui.theme.StatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventStreamScreen(
    repository: KubernetesRepository?,
    namespace: String,
    onBack: () -> Unit,
    eventViewModel: EventStreamViewModel = viewModel(),
) {
    val state by eventViewModel.state.collectAsState()
    var showSearch by remember { mutableStateOf(false) }

    LaunchedEffect(repository, namespace) {
        if (repository != null) {
            eventViewModel.start(repository, namespace.ifEmpty { null })
        }
    }

    DisposableEffect(Unit) {
        onDispose { eventViewModel.stop() }
    }

    val filteredEvents by remember(state.events, state.filter) {
        derivedStateOf {
            state.events.filter { event ->
                val matchesType = state.filter.type == null || event.type == state.filter.type
                val matchesSearch = state.filter.searchQuery.isBlank() ||
                    event.message.contains(state.filter.searchQuery, ignoreCase = true) ||
                    event.reason.contains(state.filter.searchQuery, ignoreCase = true) ||
                    event.involvedObject.contains(state.filter.searchQuery, ignoreCase = true)
                matchesType && matchesSearch
            }
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(filteredEvents.size, state.autoScroll) {
        if (state.autoScroll && filteredEvents.isNotEmpty()) {
            listState.animateScrollToItem(filteredEvents.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        buildString {
                            append("Events")
                            if (state.isStreaming) append(" (live)")
                            append(" — ${filteredEvents.size}")
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { eventViewModel.toggleAutoScroll() }) {
                        Icon(
                            Icons.Default.VerticalAlignBottom,
                            contentDescription = "Auto-scroll",
                            tint = if (state.autoScroll) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
            // Filter row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.filter.type == null,
                    onClick = { eventViewModel.setTypeFilter(null) },
                    label = { Text("All") },
                )
                FilterChip(
                    selected = state.filter.type == "Warning",
                    onClick = { eventViewModel.setTypeFilter("Warning") },
                    label = { Text("Warning") },
                )
                FilterChip(
                    selected = state.filter.type == "Normal",
                    onClick = { eventViewModel.setTypeFilter("Normal") },
                    label = { Text("Normal") },
                )
            }

            // Search bar
            if (showSearch) {
                OutlinedTextField(
                    value = state.filter.searchQuery,
                    onValueChange = { eventViewModel.setSearchQuery(it) },
                    label = { Text("Search events") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            // Error
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            // Event list
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(filteredEvents, key = { "${it.uid}-${it.count}" }) { event ->
                    EventListItem(event)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun EventListItem(event: EventItem) {
    ListItem(
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EventTypeBadge(event.type)
                Text(event.reason, style = MaterialTheme.typography.bodyMedium)
            }
        },
        supportingContent = {
            Column {
                Text(event.message, maxLines = 2)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        event.involvedObject,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (event.namespace.isNotEmpty()) {
                        Text(
                            event.namespace,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        event.age,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (event.count > 1) {
                        Text(
                            "×${event.count}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun EventTypeBadge(type: String) {
    val color = when (type) {
        "Warning" -> StatusColors.failed
        "Normal" -> Color(0xFF2196F3)
        else -> Color(0xFF9E9E9E)
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = type,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
