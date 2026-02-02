package dev.nutting.kexplore.ui.screen.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.ui.components.HighlightedTerminalView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodLogsScreen(
    repository: KubernetesRepository?,
    namespace: String,
    podName: String,
    onBack: (() -> Unit)? = null,
    embedded: Boolean = false,
    viewModel: PodLogsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(namespace, podName) {
        if (repository != null) {
            viewModel.initialize(repository, namespace, podName)
        }
    }

    val scrollToLine = remember(state.currentMatchIndex, state.searchMatchIndices) {
        if (state.currentMatchIndex >= 0 && state.currentMatchIndex < state.searchMatchIndices.size) {
            state.searchMatchIndices[state.currentMatchIndex]
        } else -1
    }

    val content = @Composable {
        Column(modifier = Modifier.fillMaxSize()) {
            // Controls row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Container selector
                if (state.containers.size > 1) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.weight(1f),
                    ) {
                        OutlinedTextField(
                            value = state.selectedContainer ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Container") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            singleLine = true,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            state.containers.forEach { container ->
                                DropdownMenuItem(
                                    text = { Text(container) },
                                    onClick = {
                                        expanded = false
                                        if (repository != null) {
                                            viewModel.selectContainer(container, repository, namespace, podName)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                // Tail lines chips
                val tailOptions = listOf(100, 500, 1000)
                tailOptions.forEach { count ->
                    FilterChip(
                        selected = state.tailLines == count,
                        onClick = {
                            if (repository != null) {
                                viewModel.setTailLines(count, repository, namespace, podName)
                            }
                        },
                        label = { Text("$count") },
                    )
                }
            }

            // Action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Follow toggle
                FilterChip(
                    selected = state.isFollowing,
                    onClick = { viewModel.toggleFollowing() },
                    label = { Text("Follow") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.VerticalAlignBottom,
                            contentDescription = null,
                            modifier = Modifier.padding(0.dp),
                        )
                    },
                )

                // Pause/resume
                FilterChip(
                    selected = !state.isStreaming,
                    onClick = {
                        if (repository != null) {
                            viewModel.toggleStreaming(repository, namespace, podName)
                        }
                    },
                    label = { Text(if (state.isStreaming) "Pause" else "Resume") },
                    leadingIcon = {
                        Icon(
                            if (state.isStreaming) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                        )
                    },
                )

                // Search toggle
                FilterChip(
                    selected = state.searchVisible,
                    onClick = { viewModel.toggleSearch() },
                    label = { Text("Search") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                )
            }

            // Search bar
            AnimatedVisibility(visible = state.searchVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearch(it) },
                        label = { Text("Search") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = state.isRegex,
                        onClick = { viewModel.toggleRegex() },
                        label = { Text(".*") },
                    )
                    if (state.searchMatchIndices.isNotEmpty()) {
                        Text(
                            "${state.currentMatchIndex + 1}/${state.searchMatchIndices.size}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    IconButton(onClick = { viewModel.previousMatch() }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous")
                    }
                    IconButton(onClick = { viewModel.nextMatch() }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next")
                    }
                }
            }

            // Error display
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            // Log output with highlighting
            HighlightedTerminalView(
                lines = state.lines,
                autoScroll = state.isFollowing,
                showLineNumbers = true,
                searchQuery = state.searchQuery,
                isRegex = state.isRegex,
                searchMatchIndices = state.searchMatchIndices,
                currentMatchIndex = state.currentMatchIndex,
                scrollToLine = scrollToLine,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (embedded) {
        content()
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Logs: $podName") },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("logs", state.lines.joinToString("\n")))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy logs")
                        }
                    },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                content()
            }
        }
    }
}
