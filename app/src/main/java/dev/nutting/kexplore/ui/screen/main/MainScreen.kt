package dev.nutting.kexplore.ui.screen.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nutting.kexplore.BuildConfig
import dev.nutting.kexplore.MainViewModel
import dev.nutting.kexplore.R
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.ui.navigation.BottomTab
import dev.nutting.kexplore.ui.screen.resources.ResourceListScreen
import dev.nutting.kexplore.ui.screen.resources.ResourceListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onManageConnections: () -> Unit,
    onNavigateToDetail: (namespace: String, kind: ResourceType, name: String) -> Unit,
    actionMessage: String? = null,
    onActionMessageShown: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val repository by viewModel.repository.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val resourceListViewModel: ResourceListViewModel = viewModel()
    var showAboutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        if (actionMessage != null) {
            snackbarHostState.showSnackbar(actionMessage)
            onActionMessageShown()
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                state = state,
                onSelectConnection = { id ->
                    viewModel.connectToCluster(id)
                    scope.launch { drawerState.close() }
                },
                onSelectNamespace = { ns ->
                    viewModel.selectNamespace(ns)
                    scope.launch { drawerState.close() }
                },
                onManageConnections = {
                    scope.launch { drawerState.close() }
                    onManageConnections()
                },
            )
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            viewModel.getActiveConnectionName()
                                ?.let { clusterName ->
                                    if (state.selectedTab == BottomTab.Cluster) {
                                        "$clusterName / Cluster Resources"
                                    } else {
                                        val ns = state.activeNamespace.ifEmpty { "All Namespaces" }
                                        "$clusterName / $ns"
                                    }
                                }
                                ?: "Kexplore"
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAboutDialog = true }) {
                            Icon(Icons.Outlined.Info, contentDescription = "About")
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    BottomTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = state.selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            },
        ) { padding ->
            ResourceListScreen(
                repository = repository,
                namespace = state.activeNamespace,
                category = state.selectedTab.category,
                isConnected = state.isConnected,
                connectionError = state.connectionError,
                modifier = Modifier.padding(padding),
                onResourceClick = { summary ->
                    onNavigateToDetail(summary.namespace, summary.kind, summary.name)
                },
                listViewModel = resourceListViewModel,
            )
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
        },
        title = { Text("Kexplore") },
        text = {
            Column {
                Text(
                    "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "A Kubernetes API explorer for Android.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nuttingd/kexplore"))
                        )
                    },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("View on GitHub")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
