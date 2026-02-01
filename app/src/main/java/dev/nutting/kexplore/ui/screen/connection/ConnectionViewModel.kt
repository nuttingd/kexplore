package dev.nutting.kexplore.ui.screen.connection

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nutting.kexplore.data.connection.AuthMethod
import dev.nutting.kexplore.data.connection.ClusterConnection
import dev.nutting.kexplore.data.connection.ConnectionStore
import dev.nutting.kexplore.data.kubernetes.KubernetesClientFactory
import dev.nutting.kexplore.util.ErrorMapper
import io.fabric8.kubernetes.client.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class ManualConnectionState(
    val name: String = "",
    val serverUrl: String = "",
    val skipTlsVerify: Boolean = false,
    val authType: AuthType = AuthType.BearerToken,
    val token: String = "",
    val clientCertData: String = "",
    val clientKeyData: String = "",
    val caData: String = "",
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val testSuccess: Boolean = false,
)

enum class AuthType { BearerToken, ClientCertificate }

data class KubeconfigImportState(
    val fileUri: Uri? = null,
    val rawContent: String? = null,
    val contexts: List<KubeconfigContext> = emptyList(),
    val selectedContexts: Set<String> = emptySet(),
    val error: String? = null,
    val importing: Boolean = false,
)

data class KubeconfigContext(
    val name: String,
    val cluster: String,
    val user: String,
)

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {

    val connectionStore = ConnectionStore(application)

    private val _manualState = MutableStateFlow(ManualConnectionState())
    val manualState: StateFlow<ManualConnectionState> = _manualState.asStateFlow()

    private val _kubeconfigState = MutableStateFlow(KubeconfigImportState())
    val kubeconfigState: StateFlow<KubeconfigImportState> = _kubeconfigState.asStateFlow()

    fun updateManualState(update: ManualConnectionState.() -> ManualConnectionState) {
        _manualState.update { it.update() }
    }

    fun testManualConnection() {
        val state = _manualState.value
        _manualState.update { it.copy(isTesting = true, testResult = null) }

        viewModelScope.launch {
            try {
                val connection = buildManualConnection(state)
                withContext(Dispatchers.IO) {
                    val client = KubernetesClientFactory.createClient(connection)
                    client.use { it.namespaces().list() }
                }
                _manualState.update {
                    it.copy(isTesting = false, testResult = "Connection successful", testSuccess = true)
                }
            } catch (e: Exception) {
                _manualState.update {
                    it.copy(isTesting = false, testResult = ErrorMapper.map(e), testSuccess = false)
                }
            }
        }
    }

    fun saveManualConnection(): ClusterConnection {
        val state = _manualState.value
        val connection = buildManualConnection(state)
        connectionStore.saveConnection(connection)
        connectionStore.setActiveConnectionId(connection.id)
        return connection
    }

    fun loadKubeconfigFile(uri: Uri) {
        _kubeconfigState.update { it.copy(fileUri = uri, importing = true, error = null) }

        viewModelScope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openInputStream(uri)
                        ?.bufferedReader()
                        ?.readText()
                        ?: throw IllegalStateException("Could not read file")
                }

                val contexts = withContext(Dispatchers.IO) {
                    parseKubeconfigContexts(content)
                }

                _kubeconfigState.update {
                    it.copy(
                        rawContent = content,
                        contexts = contexts,
                        importing = false,
                    )
                }
            } catch (e: Exception) {
                _kubeconfigState.update {
                    it.copy(importing = false, error = ErrorMapper.map(e))
                }
            }
        }
    }

    fun toggleContextSelection(contextName: String) {
        _kubeconfigState.update { state ->
            val selected = state.selectedContexts.toMutableSet()
            if (contextName in selected) selected.remove(contextName) else selected.add(contextName)
            state.copy(selectedContexts = selected)
        }
    }

    fun importSelectedContexts(): List<ClusterConnection> {
        val state = _kubeconfigState.value
        val rawContent = state.rawContent ?: return emptyList()

        val connections = state.selectedContexts.map { contextName ->
            val context = state.contexts.first { it.name == contextName }
            ClusterConnection(
                id = UUID.randomUUID().toString(),
                name = context.name,
                server = context.cluster,
                authMethod = AuthMethod.Kubeconfig(
                    contextName = contextName,
                    rawKubeconfig = rawContent,
                ),
            )
        }

        connections.forEach { connectionStore.saveConnection(it) }
        connections.firstOrNull()?.let { connectionStore.setActiveConnectionId(it.id) }
        return connections
    }

    private fun buildManualConnection(state: ManualConnectionState): ClusterConnection {
        val authMethod = when (state.authType) {
            AuthType.BearerToken -> AuthMethod.BearerToken(state.token)
            AuthType.ClientCertificate -> AuthMethod.ClientCertificate(
                clientCertData = state.clientCertData,
                clientKeyData = state.clientKeyData,
            )
        }
        return ClusterConnection(
            id = UUID.randomUUID().toString(),
            name = state.name.ifBlank { state.serverUrl },
            server = state.serverUrl,
            authMethod = authMethod,
            certificateAuthorityData = state.caData.ifBlank { null },
            skipTlsVerify = state.skipTlsVerify,
        )
    }

    private fun parseKubeconfigContexts(content: String): List<KubeconfigContext> {
        val config = Config.fromKubeconfig(null, content, null)
        // Parse contexts from the raw YAML since fabric8 Config only loads one at a time.
        // We'll extract context names by re-parsing.
        val contexts = mutableListOf<KubeconfigContext>()
        val lines = content.lines()
        var inContexts = false
        var currentName: String? = null
        var currentCluster: String? = null
        var currentUser: String? = null

        for (line in lines) {
            val trimmed = line.trimStart()
            if (trimmed == "contexts:") {
                inContexts = true
                continue
            }
            if (inContexts) {
                if (trimmed.startsWith("- name:")) {
                    // Save previous context if exists
                    if (currentName != null) {
                        contexts.add(
                            KubeconfigContext(
                                name = currentName,
                                cluster = currentCluster ?: "",
                                user = currentUser ?: "",
                            )
                        )
                    }
                    currentName = trimmed.removePrefix("- name:").trim()
                    currentCluster = null
                    currentUser = null
                } else if (trimmed.startsWith("cluster:")) {
                    currentCluster = trimmed.removePrefix("cluster:").trim()
                } else if (trimmed.startsWith("user:")) {
                    currentUser = trimmed.removePrefix("user:").trim()
                } else if (!trimmed.startsWith("-") && !trimmed.startsWith(" ") && !trimmed.startsWith("context:") && trimmed.isNotEmpty() && !trimmed.startsWith("namespace:")) {
                    // End of contexts section
                    inContexts = false
                }
            }
        }
        // Add last context
        if (currentName != null) {
            contexts.add(
                KubeconfigContext(
                    name = currentName,
                    cluster = currentCluster ?: "",
                    user = currentUser ?: "",
                )
            )
        }

        // Fallback: if parsing didn't find contexts, use the default from fabric8
        if (contexts.isEmpty()) {
            contexts.add(
                KubeconfigContext(
                    name = config.currentContext?.name ?: "default",
                    cluster = config.masterUrl ?: "",
                    user = "",
                )
            )
        }

        return contexts
    }
}
