package dev.nutting.kexplore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import dev.nutting.kexplore.data.preferences.DensityMode
import dev.nutting.kexplore.ui.navigation.AppNavGraph
import dev.nutting.kexplore.ui.theme.KexploreTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = application as KexploreApp
            val displayPrefs = remember { app.displayPreferences }
            val densityMode by displayPrefs.densityMode.collectAsState(initial = DensityMode.Default)

            KexploreTheme(densityMode = densityMode) {
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
