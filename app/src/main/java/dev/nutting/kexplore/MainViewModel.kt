package dev.nutting.kexplore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nutting.kexplore.data.connection.ClusterConnection
import dev.nutting.kexplore.data.connection.ConnectionStore
import dev.nutting.kexplore.data.kubernetes.KubernetesClientFactory
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.data.model.ContentState
import dev.nutting.kexplore.ui.navigation.BottomTab
import dev.nutting.kexplore.util.ErrorMapper
import io.fabric8.kubernetes.client.KubernetesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val connections: List<ClusterConnection> = emptyList(),
    val activeConnectionId: String? = null,
    val namespaces: ContentState<List<String>> = ContentState.Loading,
    val activeNamespace: String = "default",
    val selectedTab: BottomTab = BottomTab.Workloads,
    val isConnected: Boolean = false,
    val connectionError: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val connectionStore = ConnectionStore(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var client: KubernetesClient? = null
    private var _repository: KubernetesRepository? = null
    val repository: KubernetesRepository? get() = _repository

    init {
        loadConnections()
    }

    fun loadConnections() {
        val connections = connectionStore.getConnections()
        val activeId = connectionStore.getActiveConnectionId()
        _uiState.update {
            it.copy(connections = connections, activeConnectionId = activeId)
        }
        if (activeId != null && connections.any { it.id == activeId }) {
            connectToCluster(activeId)
        }
    }

    fun connectToCluster(connectionId: String) {
        val connection = connectionStore.getConnection(connectionId) ?: return
        _uiState.update {
            it.copy(
                activeConnectionId = connectionId,
                isConnected = false,
                connectionError = null,
                namespaces = ContentState.Loading,
            )
        }
        connectionStore.setActiveConnectionId(connectionId)

        viewModelScope.launch {
            try {
                val newClient = withContext(Dispatchers.IO) {
                    KubernetesClientFactory.createClient(connection)
                }
                client?.close()
                client = newClient
                _repository = KubernetesRepository(newClient)

                val namespaces = withContext(Dispatchers.IO) {
                    newClient.namespaces().list().items.map { it.metadata.name }
                }

                _uiState.update {
                    it.copy(
                        isConnected = true,
                        namespaces = ContentState.Success(namespaces),
                        activeNamespace = if (namespaces.contains("default")) "default"
                        else namespaces.firstOrNull() ?: "default",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isConnected = false,
                        connectionError = ErrorMapper.map(e),
                        namespaces = ContentState.Error(ErrorMapper.map(e)),
                    )
                }
            }
        }
    }

    fun selectNamespace(namespace: String) {
        _uiState.update { it.copy(activeNamespace = namespace) }
    }

    fun selectTab(tab: BottomTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun getActiveConnectionName(): String? {
        val state = _uiState.value
        return state.connections.find { it.id == state.activeConnectionId }?.name
    }

    override fun onCleared() {
        super.onCleared()
        client?.close()
    }
}
