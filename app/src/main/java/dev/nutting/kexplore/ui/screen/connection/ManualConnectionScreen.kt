package dev.nutting.kexplore.ui.screen.connection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualConnectionScreen(
    viewModel: ConnectionViewModel,
    onBack: () -> Unit,
    onConnected: () -> Unit,
) {
    val state by viewModel.manualState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual Connection") },
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
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { v -> viewModel.updateManualState { copy(name = v) } },
                label = { Text("Connection Name") },
                placeholder = { Text("My Cluster") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = { v -> viewModel.updateManualState { copy(serverUrl = v) } },
                label = { Text("Server URL") },
                placeholder = { Text("https://kubernetes.example.com:6443") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Skip TLS Verification")
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = state.skipTlsVerify,
                    onCheckedChange = { v -> viewModel.updateManualState { copy(skipTlsVerify = v) } },
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Authentication", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row {
                FilterChip(
                    selected = state.authType == AuthType.BearerToken,
                    onClick = { viewModel.updateManualState { copy(authType = AuthType.BearerToken) } },
                    label = { Text("Bearer Token") },
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = state.authType == AuthType.ClientCertificate,
                    onClick = { viewModel.updateManualState { copy(authType = AuthType.ClientCertificate) } },
                    label = { Text("Client Certificate") },
                )
            }
            Spacer(Modifier.height(12.dp))
            when (state.authType) {
                AuthType.BearerToken -> {
                    OutlinedTextField(
                        value = state.token,
                        onValueChange = { v -> viewModel.updateManualState { copy(token = v) } },
                        label = { Text("Bearer Token") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                    )
                }
                AuthType.ClientCertificate -> {
                    OutlinedTextField(
                        value = state.clientCertData,
                        onValueChange = { v -> viewModel.updateManualState { copy(clientCertData = v) } },
                        label = { Text("Client Certificate (base64)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.clientKeyData,
                        onValueChange = { v -> viewModel.updateManualState { copy(clientKeyData = v) } },
                        label = { Text("Client Key (base64)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.caData,
                onValueChange = { v -> viewModel.updateManualState { copy(caData = v) } },
                label = { Text("CA Certificate (base64, optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )
            Spacer(Modifier.height(16.dp))

            state.testResult?.let { result ->
                Text(
                    text = result,
                    color = if (state.testSuccess) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
            }

            Row {
                OutlinedButton(
                    onClick = { viewModel.testManualConnection() },
                    enabled = state.serverUrl.isNotBlank() && !state.isTesting,
                ) {
                    if (state.isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text("Test Connection")
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        viewModel.saveManualConnection()
                        onConnected()
                    },
                    enabled = state.serverUrl.isNotBlank(),
                ) {
                    Text("Save & Connect")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
