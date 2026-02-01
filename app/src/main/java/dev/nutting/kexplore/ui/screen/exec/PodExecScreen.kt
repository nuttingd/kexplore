package dev.nutting.kexplore.ui.screen.exec

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.ui.components.TerminalView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodExecScreen(
    repository: KubernetesRepository?,
    namespace: String,
    podName: String,
    container: String?,
    onBack: () -> Unit,
    execViewModel: PodExecViewModel,
) {
    var command by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    val state by execViewModel.state.collectAsState()

    LaunchedEffect(namespace, podName, container) {
        execViewModel.connect(repository, namespace, podName, container)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exec: $podName") },
                navigationIcon = {
                    IconButton(onClick = {
                        execViewModel.closeSession()
                        onBack()
                    }) {
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
            // Terminal output area
            TerminalView(
                lines = state.lines,
                modifier = Modifier.weight(1f),
            )

            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            // Command input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // History button
                if (state.commandHistory.isNotEmpty()) {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(Icons.Default.History, contentDescription = "Command history")
                    }
                    DropdownMenu(
                        expanded = showHistory,
                        onDismissRequest = { showHistory = false },
                    ) {
                        state.commandHistory.reversed().take(20).forEach { histCmd ->
                            DropdownMenuItem(
                                text = { Text(histCmd) },
                                onClick = {
                                    command = histCmd
                                    showHistory = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    placeholder = { Text("Enter command...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        if (command.isNotBlank()) {
                            execViewModel.sendCommand(command)
                            command = ""
                        }
                    },
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Execute")
                }
            }
        }
    }
}
