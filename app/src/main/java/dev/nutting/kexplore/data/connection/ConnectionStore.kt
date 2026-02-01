package dev.nutting.kexplore.data.connection

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ConnectionStore(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getConnections(): List<ClusterConnection> {
        val raw = prefs.getString(KEY_CONNECTIONS, null) ?: return emptyList()
        return json.decodeFromString(raw)
    }

    fun saveConnection(connection: ClusterConnection) {
        val connections = getConnections().toMutableList()
        val index = connections.indexOfFirst { it.id == connection.id }
        if (index >= 0) {
            connections[index] = connection
        } else {
            connections.add(connection)
        }
        prefs.edit().putString(KEY_CONNECTIONS, json.encodeToString(connections)).apply()
    }

    fun deleteConnection(id: String) {
        val connections = getConnections().filter { it.id != id }
        prefs.edit().putString(KEY_CONNECTIONS, json.encodeToString(connections)).apply()
    }

    fun getConnection(id: String): ClusterConnection? =
        getConnections().find { it.id == id }

    fun getActiveConnectionId(): String? =
        prefs.getString(KEY_ACTIVE_CONNECTION, null)

    fun setActiveConnectionId(id: String?) {
        prefs.edit().putString(KEY_ACTIVE_CONNECTION, id).apply()
    }

    companion object {
        private const val PREFS_NAME = "kexplore_connections"
        private const val KEY_CONNECTIONS = "connections"
        private const val KEY_ACTIVE_CONNECTION = "active_connection_id"
    }
}
