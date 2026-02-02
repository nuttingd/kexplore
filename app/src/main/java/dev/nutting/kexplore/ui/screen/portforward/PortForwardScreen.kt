package dev.nutting.kexplore.ui.screen.portforward

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.data.portforward.ForwardStatus
import dev.nutting.kexplore.data.portforward.PortForwardSession
import dev.nutting.kexplore.data.portforward.TargetType
import dev.nutting.kexplore.ui.components.SectionHeader
import io.fabric8.kubernetes.client.KubernetesClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortForwardScreen(
    repository: KubernetesRepository?,
    client: KubernetesClient?,
    connectionId: String?,
    namespace: String,
    onBack: () -> Unit,
    viewModel: PortForwardViewModel,
    preSelectedPod: String? = null,
    preSelectedService: String? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form state
    var targetTypeIndex by remember { mutableIntStateOf(if (preSelectedService != null) 1 else 0) }
    var selectedPod by remember { mutableStateOf(preSelectedPod ?: "") }
    var selectedService by remember { mutableStateOf(preSelectedService ?: "") }
    var remotePortText by remember { mutableStateOf("") }
    var localPortText by remember { mutableStateOf("") }
    var podExpanded by remember { mutableStateOf(false) }
    var serviceExpanded by remember { mutableStateOf(false) }
    var portExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(repository, namespace) {
        viewModel.loadResources(repository, namespace)
    }

    // Auto-select when there's only one pod or service
    LaunchedEffect(uiState.pods) {
        if (selectedPod.isEmpty() && uiState.pods.size == 1) {
            val pod = uiState.pods.first()
            selectedPod = pod.name
            pod.namedPorts.firstOrNull()?.let {
                remotePortText = it.port.toString()
                if (localPortText.isEmpty()) localPortText = it.port.toString()
            }
        }
    }
    LaunchedEffect(uiState.services) {
        if (selectedService.isEmpty() && uiState.services.size == 1 && targetTypeIndex == 1) {
            val svc = uiState.services.first()
            selectedService = svc.name
            svc.namedPorts.firstOrNull()?.let {
                remotePortText = it.port.toString()
                if (localPortText.isEmpty()) localPortText = it.port.toString()
            }
        }
    }

    // Auto-fill ports from pre-selected pod
    LaunchedEffect(uiState.pods, preSelectedPod) {
        if (preSelectedPod != null && selectedPod == preSelectedPod && remotePortText.isEmpty()) {
            val pod = uiState.pods.find { it.name == preSelectedPod }
            pod?.namedPorts?.firstOrNull()?.let {
                remotePortText = it.port.toString()
                if (localPortText.isEmpty()) localPortText = it.port.toString()
            }
        }
    }

    // Auto-fill ports from pre-selected service
    LaunchedEffect(uiState.services, preSelectedService) {
        if (preSelectedService != null && selectedService == preSelectedService && remotePortText.isEmpty()) {
            val svc = uiState.services.find { it.name == preSelectedService }
            svc?.namedPorts?.firstOrNull()?.let {
                remotePortText = it.port.toString()
                if (localPortText.isEmpty()) localPortText = it.port.toString()
            }
        }
    }

    val currentNamedPorts = remember(uiState.pods, uiState.services, selectedPod, selectedService, targetTypeIndex) {
        if (targetTypeIndex == 0) {
            uiState.pods.find { it.name == selectedPod }?.namedPorts ?: emptyList()
        } else {
            uiState.services.find { it.name == selectedService }?.namedPorts ?: emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Port Forward") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            // New Forward form
            item {
                SectionHeader("New Forward")
                Spacer(Modifier.height(8.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("Pod", "Service").forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = targetTypeIndex == index,
                            onClick = { targetTypeIndex = index },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                        ) {
                            Text(label)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (targetTypeIndex == 0) {
                    // Pod selector
                    ExposedDropdownMenuBox(
                        expanded = podExpanded,
                        onExpandedChange = { podExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedPod,
                            onValueChange = { selectedPod = it },
                            label = { Text("Pod") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = podExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                        )
                        ExposedDropdownMenu(
                            expanded = podExpanded,
                            onDismissRequest = { podExpanded = false },
                        ) {
                            uiState.pods.forEach { pod ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(pod.name)
                                            if (pod.namedPorts.isNotEmpty()) {
                                                Text(
                                                    text = pod.namedPorts.joinToString(", ") { it.displayLabel },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedPod = pod.name
                                        podExpanded = false
                                        pod.namedPorts.firstOrNull()?.let {
                                            remotePortText = it.port.toString()
                                            if (localPortText.isEmpty()) localPortText = it.port.toString()
                                        }
                                    },
                                )
                            }
                        }
                    }
                } else {
                    // Service selector
                    ExposedDropdownMenuBox(
                        expanded = serviceExpanded,
                        onExpandedChange = { serviceExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedService,
                            onValueChange = { selectedService = it },
                            label = { Text("Service") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                        )
                        ExposedDropdownMenu(
                            expanded = serviceExpanded,
                            onDismissRequest = { serviceExpanded = false },
                        ) {
                            uiState.services.forEach { svc ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(svc.name)
                                            if (svc.namedPorts.isNotEmpty()) {
                                                Text(
                                                    text = svc.namedPorts.joinToString(", ") { it.displayLabel },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedService = svc.name
                                        serviceExpanded = false
                                        svc.namedPorts.firstOrNull()?.let {
                                            remotePortText = it.port.toString()
                                            if (localPortText.isEmpty()) localPortText = it.port.toString()
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Port selector
                ExposedDropdownMenuBox(
                    expanded = portExpanded,
                    onExpandedChange = { portExpanded = it },
                ) {
                    OutlinedTextField(
                        value = remotePortText,
                        onValueChange = { remotePortText = it },
                        label = { Text("Remote Port") },
                        trailingIcon = {
                            if (currentNamedPorts.isNotEmpty()) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = portExpanded)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                if (currentNamedPorts.isNotEmpty()) MenuAnchorType.PrimaryNotEditable
                                else MenuAnchorType.PrimaryEditable,
                            ),
                        readOnly = currentNamedPorts.isNotEmpty(),
                    )
                    if (currentNamedPorts.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = portExpanded,
                            onDismissRequest = { portExpanded = false },
                        ) {
                            currentNamedPorts.forEach { namedPort ->
                                DropdownMenuItem(
                                    text = { Text(namedPort.displayLabel) },
                                    onClick = {
                                        remotePortText = namedPort.port.toString()
                                        localPortText = namedPort.port.toString()
                                        portExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = localPortText,
                    onValueChange = { localPortText = it },
                    label = { Text("Local Port (auto-assigned if empty)") },
                    placeholder = {
                        if (remotePortText.isNotEmpty()) Text(remotePortText)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        val remotePort = remotePortText.toIntOrNull() ?: return@Button
                        val localPort = localPortText.toIntOrNull()
                        val connId = connectionId ?: return@Button
                        val k8sClient = client ?: return@Button

                        if (targetTypeIndex == 0) {
                            val pod = uiState.pods.find { it.name == selectedPod }
                            val podNs = pod?.namespace ?: namespace
                            viewModel.startForward(
                                client = k8sClient,
                                connectionId = connId,
                                namespace = podNs,
                                podName = selectedPod,
                                remotePort = remotePort,
                                localPort = localPort,
                            )
                        } else {
                            val svc = uiState.services.find { it.name == selectedService }
                            if (svc != null) {
                                // Resolve service port → container targetPort for pod forwarding
                                val containerPort = svc.namedPorts
                                    .find { it.port == remotePort }?.forwardPort
                                    ?: remotePort
                                viewModel.resolveServiceToPod(repository, svc) { podName ->
                                    if (podName != null) {
                                        viewModel.startForward(
                                            client = k8sClient,
                                            connectionId = connId,
                                            namespace = svc.namespace,
                                            podName = podName,
                                            remotePort = containerPort,
                                            localPort = localPort,
                                            targetType = TargetType.Service,
                                            serviceName = svc.name,
                                        )
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("No ready pod found for service ${svc.name}")
                                        }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = remotePortText.toIntOrNull() != null &&
                        (if (targetTypeIndex == 0) selectedPod.isNotEmpty() else selectedService.isNotEmpty()),
                ) {
                    Text("Start Forward")
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
            }

            // Active sessions
            item {
                SectionHeader("Active Forwards")
            }

            val activeSessions = sessions.filter { it.status != ForwardStatus.Stopped }
            if (activeSessions.isEmpty()) {
                item {
                    Text(
                        text = "No active port forwards",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            } else {
                items(activeSessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        onStop = { viewModel.stopForward(session.id) },
                        onRemove = { viewModel.removeSession(session.id) },
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Port forward", "localhost:${session.localPort}")
                            clipboard.setPrimaryClip(clip)
                            scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: PortForwardSession,
    onStop: () -> Unit,
    onRemove: () -> Unit,
    onCopy: () -> Unit,
) {
    val statusColor = when (session.status) {
        ForwardStatus.Active -> MaterialTheme.colorScheme.primary
        ForwardStatus.Starting -> MaterialTheme.colorScheme.tertiary
        ForwardStatus.Failed -> MaterialTheme.colorScheme.error
        ForwardStatus.Stopped -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val target = if (session.serviceName != null) {
                        "svc/${session.serviceName} -> ${session.podName}"
                    } else {
                        session.podName
                    }
                    Text(
                        text = target,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "${session.namespace} | :${session.remotePort} -> localhost:${session.localPort}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = session.status.name + (session.error?.let { " - $it" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                    )
                }
                Row {
                    if (session.status == ForwardStatus.Active) {
                        IconButton(onClick = onCopy) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy address")
                        }
                        IconButton(onClick = onStop) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop")
                        }
                    }
                    if (session.status == ForwardStatus.Failed || session.status == ForwardStatus.Stopped) {
                        IconButton(onClick = onRemove) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                }
            }
        }
    }
}
