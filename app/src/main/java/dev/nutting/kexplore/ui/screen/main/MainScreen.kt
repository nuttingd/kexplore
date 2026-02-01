package dev.nutting.kexplore.ui.screen.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import dev.nutting.kexplore.MainViewModel
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.ui.navigation.BottomTab
import dev.nutting.kexplore.ui.screen.resources.ResourceListScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onManageConnections: () -> Unit,
    onNavigateToDetail: (namespace: String, kind: ResourceType, name: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            viewModel.getActiveConnectionName()
                                ?.let {
                                    val ns = state.activeNamespace.ifEmpty { "All Namespaces" }
                                    "$it / $ns"
                                }
                                ?: "Kexplore"
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
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
                repository = viewModel.repository,
                namespace = state.activeNamespace,
                category = state.selectedTab.category,
                isConnected = state.isConnected,
                connectionError = state.connectionError,
                modifier = Modifier.padding(padding),
                onResourceClick = { summary ->
                    onNavigateToDetail(summary.namespace, summary.kind, summary.name)
                },
            )
        }
    }
}
