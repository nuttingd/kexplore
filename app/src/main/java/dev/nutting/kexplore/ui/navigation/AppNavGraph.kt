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
import dev.nutting.kexplore.ui.screen.connection.ConnectionSetupScreen
import dev.nutting.kexplore.ui.screen.connection.ConnectionViewModel
import dev.nutting.kexplore.ui.screen.connection.ImportKubeconfigScreen
import dev.nutting.kexplore.ui.screen.connection.ManualConnectionScreen
import dev.nutting.kexplore.ui.screen.detail.ResourceDetailScreen
import dev.nutting.kexplore.ui.screen.main.MainScreen

object Routes {
    const val SETUP = "setup"
    const val SETUP_IMPORT = "setup/import"
    const val SETUP_MANUAL = "setup/manual"
    const val MAIN = "main"
    const val RESOURCE_DETAIL = "resource/{namespace}/{kind}/{name}"

    fun resourceDetail(namespace: String, kind: ResourceType, name: String): String =
        "resource/$namespace/${kind.name}/$name"
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
                onAddConnection = { navController.navigate(Routes.SETUP) },
                onNavigateToDetail = { namespace, kind, name ->
                    navController.navigate(Routes.resourceDetail(namespace, kind, name))
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
                onViewLogs = { container ->
                    // Will be wired in Phase 5
                },
            )
        }
    }
}
