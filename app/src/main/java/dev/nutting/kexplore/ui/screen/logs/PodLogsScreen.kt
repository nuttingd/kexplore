package dev.nutting.kexplore.ui.screen.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository

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
            }

            // Error display
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            // Log output
            val listState = rememberLazyListState()

            LaunchedEffect(state.lines.size, state.isFollowing) {
                if (state.isFollowing && state.lines.isNotEmpty()) {
                    listState.animateScrollToItem(state.lines.size - 1)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(4.dp),
            ) {
                itemsIndexed(state.lines) { index, line ->
                    Row {
                        Text(
                            text = "${index + 1}".padStart(5),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF858585),
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFD4D4D4),
                        )
                    }
                }
            }
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
