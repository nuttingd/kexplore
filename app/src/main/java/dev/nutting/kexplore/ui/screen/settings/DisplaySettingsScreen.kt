package dev.nutting.kexplore.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.nutting.kexplore.KexploreApp
import dev.nutting.kexplore.data.preferences.DensityMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as KexploreApp
    val prefs = remember { app.displayPreferences }
    val scope = rememberCoroutineScope()

    val densityMode by prefs.densityMode.collectAsState(initial = DensityMode.Default)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Display") },
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
                .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                headlineContent = { Text("Compact Mode") },
                supportingContent = { Text("Reduce spacing by 25% for more content on screen") },
                trailingContent = {
                    Switch(
                        checked = densityMode == DensityMode.Compact,
                        onCheckedChange = { compact ->
                            scope.launch {
                                val newMode = if (compact) DensityMode.Compact else DensityMode.Default
                                prefs.setDensityMode(newMode)
                            }
                        },
                    )
                },
            )
        }
    }
}
