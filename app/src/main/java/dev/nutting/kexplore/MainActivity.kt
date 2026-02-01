package dev.nutting.kexplore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import dev.nutting.kexplore.ui.navigation.AppNavGraph
import dev.nutting.kexplore.ui.theme.KexploreTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KexploreTheme {
                val mainViewModel: MainViewModel = viewModel()
                val state by mainViewModel.uiState.collectAsState()
                val navController = rememberNavController()

                AppNavGraph(
                    navController = navController,
                    hasConnections = state.connections.isNotEmpty(),
                    mainViewModel = mainViewModel,
                )
            }
        }
    }
}
