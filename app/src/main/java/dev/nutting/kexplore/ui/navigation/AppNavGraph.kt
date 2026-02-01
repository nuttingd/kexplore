package dev.nutting.kexplore.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.nutting.kexplore.MainViewModel
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.ui.screen.connection.ConnectionManagementScreen
import dev.nutting.kexplore.ui.screen.connection.ConnectionSetupScreen
import dev.nutting.kexplore.ui.screen.connection.ConnectionViewModel
import dev.nutting.kexplore.ui.screen.connection.ImportKubeconfigScreen
import dev.nutting.kexplore.ui.screen.connection.ManualConnectionScreen
import dev.nutting.kexplore.ui.screen.detail.ResourceDetailScreen
import dev.nutting.kexplore.ui.screen.exec.PodExecScreen
import dev.nutting.kexplore.ui.screen.main.MainScreen

object Routes {
    const val SETUP = "setup"
    const val SETUP_IMPORT = "setup/import"
    const val SETUP_MANUAL = "setup/manual"
    const val CONNECTIONS = "connections"
    const val MAIN = "main"
    const val RESOURCE_DETAIL = "resource/{namespace}/{kind}/{name}"
    const val POD_EXEC = "exec/{namespace}/{pod}/{container}"

    fun resourceDetail(namespace: String, kind: ResourceType, name: String): String =
        "resource/$namespace/${kind.name}/$name"

    fun podExec(namespace: String, pod: String, container: String): String =
        "exec/$namespace/$pod/$container"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    hasConnections: Boolean,
    mainViewModel: MainViewModel,
) {
    val connectionViewModel: ConnectionViewModel = viewModel()
    val startDestination = if (hasConnections) Routes.MAIN else Routes.SETUP

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.SETUP) {
            ConnectionSetupScreen(
                onImportKubeconfig = { navController.navigate(Routes.SETUP_IMPORT) },
                onManualEntry = { navController.navigate(Routes.SETUP_MANUAL) },
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

        composable(Routes.MAIN) {
            MainScreen(
                viewModel = mainViewModel,
                onManageConnections = { navController.navigate(Routes.CONNECTIONS) },
                onNavigateToDetail = { namespace, kind, name ->
                    navController.navigate(Routes.resourceDetail(namespace, kind, name))
                },
            )
        }

        composable(Routes.CONNECTIONS) {
            ConnectionManagementScreen(
                viewModel = connectionViewModel,
                connections = mainViewModel.uiState.value.connections,
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate(Routes.SETUP) },
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
            val namespace = backStackEntry.arguments?.getString("namespace") ?: ""
            val kind = ResourceType.valueOf(backStackEntry.arguments?.getString("kind") ?: "Pod")
            val name = backStackEntry.arguments?.getString("name") ?: ""

            ResourceDetailScreen(
                repository = mainViewModel.repository,
                namespace = namespace,
                resourceType = kind,
                resourceName = name,
                onBack = { navController.popBackStack() },
                onViewLogs = { /* logs are inline in detail screen */ },
                onExec = { container ->
                    navController.navigate(Routes.podExec(namespace, name, container))
                },
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

            PodExecScreen(
                repository = mainViewModel.repository,
                namespace = namespace,
                podName = pod,
                container = container.ifEmpty { null },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
