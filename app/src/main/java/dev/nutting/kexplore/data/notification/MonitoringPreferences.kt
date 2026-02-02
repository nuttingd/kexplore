package dev.nutting.kexplore.data.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.monitoringDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "monitoring_preferences",
)

class MonitoringPreferences(private val context: Context) {

    private val monitoringEnabledKey = booleanPreferencesKey("monitoring_enabled")
    private val realTimeEnabledKey = booleanPreferencesKey("real_time_enabled")
    private val enabledAlertTypesKey = stringSetPreferencesKey("enabled_alert_types")

    val monitoringEnabled: Flow<Boolean> = context.monitoringDataStore.data.map { prefs ->
        prefs[monitoringEnabledKey] ?: false
    }

    val realTimeEnabled: Flow<Boolean> = context.monitoringDataStore.data.map { prefs ->
        prefs[realTimeEnabledKey] ?: false
    }

    val enabledAlertTypes: Flow<Set<AlertType>> = context.monitoringDataStore.data.map { prefs ->
        val names = prefs[enabledAlertTypesKey] ?: AlertType.entries.map { it.name }.toSet()
        names.mapNotNull { name ->
            runCatching { AlertType.valueOf(name) }.getOrNull()
        }.toSet()
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.monitoringDataStore.edit { it[monitoringEnabledKey] = enabled }
    }

    suspend fun setRealTimeEnabled(enabled: Boolean) {
        context.monitoringDataStore.edit { it[realTimeEnabledKey] = enabled }
    }

    suspend fun setAlertTypeEnabled(type: AlertType, enabled: Boolean) {
        context.monitoringDataStore.edit { prefs ->
            val current = prefs[enabledAlertTypesKey]?.toMutableSet()
                ?: AlertType.entries.map { it.name }.toMutableSet()
            if (enabled) current.add(type.name) else current.remove(type.name)
            prefs[enabledAlertTypesKey] = current
        }
    }
}
