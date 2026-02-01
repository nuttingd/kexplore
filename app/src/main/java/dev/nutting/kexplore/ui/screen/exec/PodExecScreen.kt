package dev.nutting.kexplore.ui.screen.exec

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import io.fabric8.kubernetes.client.dsl.ExecWatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodExecScreen(
    repository: KubernetesRepository?,
    namespace: String,
    podName: String,
    container: String?,
    onBack: () -> Unit,
) {
    var output by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var stdin by remember { mutableStateOf<OutputStream?>(null) }
    var execWatch by remember { mutableStateOf<ExecWatch?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(namespace, podName, container) {
        if (repository == null) {
            error = "Not connected"
            return@LaunchedEffect
        }
        // Exec sessions will be set up when user submits a command
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exec: $podName") },
                navigationIcon = {
                    IconButton(onClick = {
                        execWatch?.close()
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .verticalScroll(scrollState)
                    .padding(8.dp),
            ) {
                Text(
                    text = output.ifEmpty { "Type a command below and press send to execute." },
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFD4D4D4),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

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
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    placeholder = { Text("Enter command...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        if (command.isBlank() || repository == null) return@IconButton
                        val cmd = command
                        command = ""
                        output += "\n\$ $cmd\n"

                        scope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    val cmdParts = cmd.split(" ")
                                    val op = repository.execCommand(
                                        namespace = namespace,
                                        podName = podName,
                                        container = container,
                                        command = cmdParts,
                                    )
                                    op
                                }
                                output += result
                            } catch (e: Exception) {
                                output += "Error: ${e.message}\n"
                            }
                        }
                    },
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Execute")
                }
            }
        }
    }

    LaunchedEffect(output) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }
}
