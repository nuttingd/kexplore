package dev.nutting.kexplore.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import android.net.Uri
import dev.nutting.kexplore.MainViewModel
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.ui.screen.connection.ConnectionManagementScreen
import dev.nutting.kexplore.ui.screen.connection.ConnectionSetupScreen
import dev.nutting.kexplore.ui.screen.connection.ConnectionViewModel
import dev.nutting.kexplore.ui.screen.connection.ImportKubeconfigScreen
import dev.nutting.kexplore.ui.screen.connection.ManualConnectionScreen
import dev.nutting.kexplore.ui.screen.detail.ResourceDetailScreen
import dev.nutting.kexplore.ui.screen.detail.ResourceDetailViewModel
import dev.nutting.kexplore.ui.screen.exec.PodExecScreen
import dev.nutting.kexplore.ui.screen.exec.PodExecViewModel
import dev.nutting.kexplore.ui.screen.events.EventStreamScreen
import dev.nutting.kexplore.ui.screen.health.HealthDashboardScreen
import dev.nutting.kexplore.ui.screen.logs.PodLogsScreen
import dev.nutting.kexplore.ui.screen.main.MainScreen

object Routes {
    const val SETUP = "setup"
    const val SETUP_IMPORT = "setup/import"
    const val SETUP_MANUAL = "setup/manual"
    const val CONNECTIONS = "connections"
    const val MAIN = "main"
    const val RESOURCE_DETAIL = "resource/{namespace}/{kind}/{name}"
    const val POD_LOGS = "logs/{namespace}/{pod}"
    const val POD_EXEC = "exec/{namespace}/{pod}/{container}"

    const val HEALTH = "health"
    const val EVENTS = "events"

    const val CLUSTER_SCOPE_SENTINEL = "_cluster"

    fun resourceDetail(namespace: String, kind: ResourceType, name: String): String {
        val ns = if (kind.isClusterScoped) CLUSTER_SCOPE_SENTINEL else Uri.encode(namespace)
        return "resource/$ns/${kind.name}/${Uri.encode(name)}"
    }

    fun podLogs(namespace: String, pod: String): String =
        "logs/${Uri.encode(namespace)}/${Uri.encode(pod)}"

    fun podExec(namespace: String, pod: String, container: String): String =
        "exec/${Uri.encode(namespace)}/${Uri.encode(pod)}/${Uri.encode(container)}"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    hasConnections: Boolean,
    mainViewModel: MainViewModel,
) {
    val connectionViewModel: ConnectionViewModel = viewModel()
    val startDestination = remember { if (hasConnections) Routes.MAIN else Routes.SETUP }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.SETUP) {
            ConnectionSetupScreen(
                onImportKubeconfig = { navController.navigate(Routes.SETUP_IMPORT) },
                onManualEntry = {
                    connectionViewModel.resetManualState()
                    navController.navigate(Routes.SETUP_MANUAL)
                },
            )
        }

        composable(Routes.SETUP_IMPORT) {
            ImportKubeconfigScreen(
                viewModel = connectionViewModel,
                onBack = { navController.popBackStack() },
                onImported = {
                    mainViewModel.loadConnections()
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.SETUP_MANUAL) {
            ManualConnectionScreen(
                viewModel = connectionViewModel,
                onBack = { navController.popBackStack() },
                onConnected = {
                    mainViewModel.loadConnections()
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.MAIN) { backStackEntry ->
            val actionMessage = backStackEntry.savedStateHandle
                .get<String>("action_message")
            MainScreen(
                viewModel = mainViewModel,
                onManageConnections = { navController.navigate(Routes.CONNECTIONS) },
                onNavigateToDetail = { namespace, kind, name ->
                    navController.navigate(Routes.resourceDetail(namespace, kind, name))
                },
                onNavigateToHealth = { navController.navigate(Routes.HEALTH) },
                onNavigateToEvents = { navController.navigate(Routes.EVENTS) },
                actionMessage = actionMessage,
                onActionMessageShown = {
                    backStackEntry.savedStateHandle.remove<String>("action_message")
                },
            )
        }

        composable(Routes.EVENTS) {
            val repository by mainViewModel.repository.collectAsState()
            val uiState by mainViewModel.uiState.collectAsState()
            EventStreamScreen(
                repository = repository,
                namespace = uiState.activeNamespace,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.HEALTH) {
            val repository by mainViewModel.repository.collectAsState()
            val uiState by mainViewModel.uiState.collectAsState()
            HealthDashboardScreen(
                repository = repository,
                namespace = uiState.activeNamespace,
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { namespace, kind, name ->
                    navController.navigate(Routes.resourceDetail(namespace, kind, name))
                },
            )
        }

        composable(Routes.CONNECTIONS) {
            val uiState by mainViewModel.uiState.collectAsState()
            ConnectionManagementScreen(
                viewModel = connectionViewModel,
                connections = uiState.connections,
                onBack = { navController.popBackStack() },
                onAdd = {
                    connectionViewModel.resetManualState()
                    navController.navigate(Routes.SETUP)
                },
                onEdit = { connection ->
                    connectionViewModel.loadConnectionForEdit(connection)
                    navController.navigate(Routes.SETUP_MANUAL)
                },
                onDeleted = {
                    mainViewModel.loadConnections()
                },
            )
        }

        composable(
            route = Routes.RESOURCE_DETAIL,
            arguments = listOf(
                navArgument("namespace") { type = NavType.StringType },
                navArgument("kind") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val namespace = backStackEntry.arguments?.getString("namespace")?.let {
                if (it == Routes.CLUSTER_SCOPE_SENTINEL) "" else it
            } ?: ""
            val kind = runCatching {
                ResourceType.valueOf(backStackEntry.arguments?.getString("kind") ?: "Pod")
            }.getOrDefault(ResourceType.Pod)
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val repository by mainViewModel.repository.collectAsState()
            val metricsRepository by mainViewModel.metricsRepository.collectAsState()
            val detailViewModel: ResourceDetailViewModel = viewModel()

            ResourceDetailScreen(
                repository = repository,
                metricsRepository = metricsRepository,
                namespace = namespace,
                resourceType = kind,
                resourceName = name,
                onBack = { navController.popBackStack() },
                onViewFullScreenLogs = {
                    navController.navigate(Routes.podLogs(namespace, name))
                },
                onExec = { container ->
                    navController.navigate(Routes.podExec(namespace, name, container))
                },
                onDeleted = { message ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("action_message", message)
                    navController.popBackStack()
                },
                onNavigateToRelated = { relNs, relKind, relName ->
                    navController.navigate(Routes.resourceDetail(relNs, relKind, relName))
                },
                detailViewModel = detailViewModel,
            )
        }

        composable(
            route = Routes.POD_LOGS,
            arguments = listOf(
                navArgument("namespace") { type = NavType.StringType },
                navArgument("pod") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val namespace = backStackEntry.arguments?.getString("namespace") ?: ""
            val pod = backStackEntry.arguments?.getString("pod") ?: ""
            val repository by mainViewModel.repository.collectAsState()

            PodLogsScreen(
                repository = repository,
                namespace = namespace,
                podName = pod,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.POD_EXEC,
            arguments = listOf(
                navArgument("namespace") { type = NavType.StringType },
                navArgument("pod") { type = NavType.StringType },
                navArgument("container") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val namespace = backStackEntry.arguments?.getString("namespace") ?: ""
            val pod = backStackEntry.arguments?.getString("pod") ?: ""
            val container = backStackEntry.arguments?.getString("container") ?: ""
            val repository by mainViewModel.repository.collectAsState()
            val execViewModel: PodExecViewModel = viewModel()

            PodExecScreen(
                repository = repository,
                namespace = namespace,
                podName = pod,
                container = container.ifEmpty { null },
                onBack = { navController.popBackStack() },
                execViewModel = execViewModel,
            )
        }
    }
}
