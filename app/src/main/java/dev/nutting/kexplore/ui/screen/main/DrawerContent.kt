package dev.nutting.kexplore.ui.screen.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nutting.kexplore.UiState
import dev.nutting.kexplore.data.model.ContentState

@Composable
fun DrawerContent(
    state: UiState,
    onSelectConnection: (String) -> Unit,
    onSelectNamespace: (String) -> Unit,
    onManageConnections: () -> Unit,
    onNavigateToMonitoring: () -> Unit = {},
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Clusters",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(state.connections) { connection ->
                ListItem(
                    headlineContent = { Text(connection.name) },
                    supportingContent = { Text(connection.server) },
                    leadingContent = {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = null,
                            tint = if (connection.id == state.activeConnectionId)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        if (connection.id == state.activeConnectionId) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Active",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    modifier = Modifier.clickable(role = Role.Button) { onSelectConnection(connection.id) },
                )
            }
        }

        HorizontalDivider()

        if (state.isConnected && state.namespaces is ContentState.Success) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Namespace",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
            ) {
                item {
                    ListItem(
                        headlineContent = {
                            Text(
                                "All Namespaces",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        trailingContent = {
                            if (state.activeNamespace.isEmpty()) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        modifier = Modifier.clickable(role = Role.Button) { onSelectNamespace("") },
                    )
                    HorizontalDivider()
                }
                items(state.namespaces.data) { ns ->
                    ListItem(
                        headlineContent = { Text(ns) },
                        trailingContent = {
                            if (ns == state.activeNamespace) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        modifier = Modifier.clickable(role = Role.Button) { onSelectNamespace(ns) },
                    )
                }
            }
        }

        HorizontalDivider()
        TextButton(
            onClick = onNavigateToMonitoring,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null)
            Text("Monitoring")
        }
        TextButton(
            onClick = onManageConnections,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Text("Manage Connections")
        }
    }
}
