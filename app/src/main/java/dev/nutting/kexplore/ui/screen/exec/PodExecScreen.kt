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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nutting.kexplore.data.kubernetes.ExecSession
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.ui.components.TerminalView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ANSI_ESCAPE_REGEX = Regex("\u001b\\[[0-9;]*[a-zA-Z]")
private const val MAX_LINES = 5000

private fun stripAnsi(text: String): String = ANSI_ESCAPE_REGEX.replace(text, "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodExecScreen(
    repository: KubernetesRepository?,
    namespace: String,
    podName: String,
    container: String?,
    onBack: () -> Unit,
) {
    var lines by remember { mutableStateOf(listOf<String>()) }
    var command by remember { mutableStateOf("") }
    var commandHistory by remember { mutableStateOf(listOf<String>()) }
    var showHistory by remember { mutableStateOf(false) }
    var session by remember { mutableStateOf<ExecSession?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Start interactive session
    LaunchedEffect(namespace, podName, container) {
        if (repository == null) {
            error = "Not connected"
            return@LaunchedEffect
        }
        try {
            val execSession = repository.execInteractive(namespace, podName, container)
            session = execSession
            lines = lines + "Connected to $podName (interactive shell)"

            // Read stdout in background
            withContext(Dispatchers.IO) {
                val reader = execSession.stdout.bufferedReader()
                val buffer = StringBuilder()
                val charBuf = CharArray(4096)
                while (isActive) {
                    val count = try {
                        reader.read(charBuf)
                    } catch (_: Exception) {
                        -1
                    }
                    if (count == -1) break
                    buffer.append(charBuf, 0, count)
                    val text = stripAnsi(buffer.toString())
                    buffer.clear()
                    val newLines = text.split("\n")
                    withContext(Dispatchers.Main) {
                        lines = (lines + newLines).takeLast(MAX_LINES)
                    }
                }
            }
        } catch (e: Exception) {
            error = "Failed to start shell: ${e.message}"
        }
    }

    // Also read stderr
    LaunchedEffect(session) {
        val s = session ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val reader = s.stderr.bufferedReader()
            val charBuf = CharArray(4096)
            while (isActive) {
                val count = try {
                    reader.read(charBuf)
                } catch (_: Exception) {
                    -1
                }
                if (count == -1) break
                val text = stripAnsi(String(charBuf, 0, count))
                val newLines = text.split("\n")
                withContext(Dispatchers.Main) {
                    lines = (lines + newLines).takeLast(MAX_LINES)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            session?.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exec: $podName") },
                navigationIcon = {
                    IconButton(onClick = {
                        session?.close()
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
                lines = lines,
                modifier = Modifier.weight(1f),
            )

            error?.let {
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
                if (commandHistory.isNotEmpty()) {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(Icons.Default.History, contentDescription = "Command history")
                    }
                    DropdownMenu(
                        expanded = showHistory,
                        onDismissRequest = { showHistory = false },
                    ) {
                        commandHistory.reversed().take(20).forEach { histCmd ->
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
                        val s = session ?: return@IconButton
                        if (command.isBlank()) return@IconButton
                        val cmd = command
                        command = ""
                        commandHistory = commandHistory + cmd

                        scope.launch(Dispatchers.IO) {
                            try {
                                s.stdin.write("$cmd\n".toByteArray())
                                s.stdin.flush()
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    error = "Write error: ${e.message}"
                                }
                            }
                        }
                    },
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Execute")
                }
            }
        }
    }
}
