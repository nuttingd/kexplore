package dev.nutting.kexplore.ui.screen.connection

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nutting.kexplore.KexploreApp
import dev.nutting.kexplore.data.connection.AuthMethod
import dev.nutting.kexplore.data.connection.ClusterConnection
import dev.nutting.kexplore.data.kubernetes.KubernetesClientFactory
import dev.nutting.kexplore.util.ErrorMapper
import io.fabric8.kubernetes.client.internal.KubeConfigUtils
import io.fabric8.kubernetes.client.utils.Serialization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Base64
import java.util.UUID

data class ManualConnectionState(
    val editingConnectionId: String? = null,
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

    private val connectionStore = getApplication<KexploreApp>().connectionStore

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
        resetManualState()
        return connection
    }

    fun deleteConnection(id: String) {
        connectionStore.deleteConnection(id)
        if (connectionStore.getActiveConnectionId() == id) {
            val remaining = connectionStore.getConnections()
            connectionStore.setActiveConnectionId(remaining.firstOrNull()?.id)
        }
    }

    fun loadConnectionForEdit(connection: ClusterConnection) {
        val authType: AuthType
        val token: String
        val clientCertData: String
        val clientKeyData: String

        when (val auth = connection.authMethod) {
            is AuthMethod.BearerToken -> {
                authType = AuthType.BearerToken
                token = auth.token
                clientCertData = ""
                clientKeyData = ""
            }
            is AuthMethod.ClientCertificate -> {
                authType = AuthType.ClientCertificate
                token = ""
                clientCertData = auth.clientCertData
                clientKeyData = auth.clientKeyData
            }
            is AuthMethod.Kubeconfig -> {
                authType = AuthType.BearerToken
                token = ""
                clientCertData = ""
                clientKeyData = ""
            }
        }

        _manualState.value = ManualConnectionState(
            editingConnectionId = connection.id,
            name = connection.name,
            serverUrl = connection.server,
            skipTlsVerify = connection.skipTlsVerify,
            authType = authType,
            token = token,
            clientCertData = clientCertData,
            clientKeyData = clientKeyData,
            caData = connection.certificateAuthorityData ?: "",
        )
    }

    fun resetManualState() {
        _manualState.value = ManualConnectionState()
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
        val fullConfig = KubeConfigUtils.parseConfigFromString(rawContent)

        val connections = state.selectedContexts.map { contextName ->
            val context = state.contexts.first { it.name == contextName }
            val strippedConfig = stripKubeconfigToContext(fullConfig, contextName)
            ClusterConnection(
                id = UUID.randomUUID().toString(),
                name = context.name,
                server = context.cluster,
                authMethod = AuthMethod.Kubeconfig(
                    contextName = contextName,
                    rawKubeconfig = strippedConfig,
                ),
            )
        }

        connections.forEach { connectionStore.saveConnection(it) }
        connections.firstOrNull()?.let { connectionStore.setActiveConnectionId(it.id) }
        return connections
    }

    private fun stripKubeconfigToContext(
        fullConfig: io.fabric8.kubernetes.api.model.Config,
        contextName: String,
    ): String {
        val namedContext = fullConfig.contexts.first { it.name == contextName }
        val clusterName = namedContext.context.cluster
        val userName = namedContext.context.user

        val stripped = io.fabric8.kubernetes.api.model.Config()
        stripped.apiVersion = fullConfig.apiVersion
        stripped.kind = fullConfig.kind
        stripped.currentContext = contextName
        stripped.contexts = listOf(namedContext)
        stripped.clusters = fullConfig.clusters.filter { it.name == clusterName }
        stripped.users = fullConfig.users.filter { it.name == userName }
        return Serialization.asYaml(stripped)
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
            id = state.editingConnectionId ?: UUID.randomUUID().toString(),
            name = state.name.ifBlank { state.serverUrl },
            server = state.serverUrl,
            authMethod = authMethod,
            certificateAuthorityData = state.caData.ifBlank { null },
            skipTlsVerify = state.skipTlsVerify,
        )
    }

    suspend fun importFromQrPayload(base64Payload: String) = withContext(Dispatchers.IO) {
        val yamlBytes = Base64.decode(base64Payload, Base64.DEFAULT)
        val yamlContent = String(yamlBytes, Charsets.UTF_8)
        val kubeConfig = KubeConfigUtils.parseConfigFromString(yamlContent)
        val contextName = kubeConfig.currentContext
            ?: kubeConfig.contexts.firstOrNull()?.name
            ?: throw IllegalStateException("No context found in kubeconfig")

        val namedContext = kubeConfig.contexts.first { it.name == contextName }
        val clusterName = namedContext.context?.cluster ?: ""
        val cluster = kubeConfig.clusters.firstOrNull { it.name == clusterName }
        val serverUrl = cluster?.cluster?.server ?: ""

        val strippedConfig = stripKubeconfigToContext(kubeConfig, contextName)
        val connection = ClusterConnection(
            id = UUID.randomUUID().toString(),
            name = contextName,
            server = serverUrl,
            authMethod = AuthMethod.Kubeconfig(
                contextName = contextName,
                rawKubeconfig = strippedConfig,
            ),
        )
        connectionStore.saveConnection(connection)
        connectionStore.setActiveConnectionId(connection.id)
    }

    private fun parseKubeconfigContexts(content: String): List<KubeconfigContext> {
        val kubeConfig = KubeConfigUtils.parseConfigFromString(content)
        return kubeConfig.contexts.map { namedContext ->
            KubeconfigContext(
                name = namedContext.name ?: "",
                cluster = namedContext.context?.cluster ?: "",
                user = namedContext.context?.user ?: "",
            )
        }
    }
}
