package dev.nutting.kexplore.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.displayDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "display_preferences",
)

/**
 * Density mode options for UI display.
 * 
 * - Default: Material3 standard density
 * - Compact: 25% reduction in vertical spacing for power users
 */
enum class DensityMode {
    Default,
    Compact,
}

/**
 * Display preferences for UI customization.
 * 
 * This handles:
 * - Density mode (default vs compact)
 * 
 * Preferences are persisted using DataStore and survive process death.
 */
class DisplayPreferences(private val context: Context) {

    private val densityModeKey = stringPreferencesKey("density_mode")

    /**
     * Flow of the current density mode.
     * Defaults to [DensityMode.Default].
     */
    val densityMode: Flow<DensityMode> = context.displayDataStore.data.map { prefs ->
        val value = prefs[densityModeKey] ?: DensityMode.Default.name
        runCatching { DensityMode.valueOf(value) }.getOrDefault(DensityMode.Default)
    }

    /**
     * Sets the density mode.
     */
    suspend fun setDensityMode(mode: DensityMode) {
        context.displayDataStore.edit { prefs ->
            prefs[densityModeKey] = mode.name
        }
    }

    /**
     * Toggles between Default and Compact density.
     */
    suspend fun toggleDensityMode() {
        context.displayDataStore.edit { prefs ->
            val current = prefs[densityModeKey] ?: DensityMode.Default.name
            val newMode = if (current == DensityMode.Default.name) {
                DensityMode.Compact
            } else {
                DensityMode.Default
            }
            prefs[densityModeKey] = newMode.name
        }
    }
}
