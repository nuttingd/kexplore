package dev.nutting.kexplore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nutting.kexplore.data.connection.ClusterConnection
import dev.nutting.kexplore.data.kubernetes.AnomalyChecker
import dev.nutting.kexplore.data.kubernetes.KubernetesClientFactory
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.data.kubernetes.MetricsRepository
import dev.nutting.kexplore.data.kubernetes.TabAnomalies
import dev.nutting.kexplore.data.model.ContentState
import dev.nutting.kexplore.ui.navigation.BottomTab
import dev.nutting.kexplore.util.ErrorMapper
import io.fabric8.kubernetes.client.KubernetesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

data class UiState(
    val connections: List<ClusterConnection> = emptyList(),
    val activeConnectionId: String? = null,
    val namespaces: ContentState<List<String>> = ContentState.Loading,
    val activeNamespace: String = "default",
    val selectedTab: BottomTab = BottomTab.Workloads,
    val isConnected: Boolean = false,
    val connectionError: String? = null,
    val tabAnomalies: TabAnomalies = TabAnomalies(),
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val connectionStore = getApplication<KexploreApp>().connectionStore

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var connectJob: Job? = null
    private var anomalyJob: Job? = null
    private var client: KubernetesClient? = null

    private val _repository = MutableStateFlow<KubernetesRepository?>(null)
    val repository: StateFlow<KubernetesRepository?> = _repository.asStateFlow()

    private val _metricsRepository = MutableStateFlow<MetricsRepository?>(null)
    val metricsRepository: StateFlow<MetricsRepository?> = _metricsRepository.asStateFlow()

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

        connectJob?.cancel()
        connectJob = viewModelScope.launch {
            try {
                val newClient = withContext(Dispatchers.IO) {
                    KubernetesClientFactory.createClient(connection)
                }
                client?.close()
                client = newClient
                _repository.value = KubernetesRepository(newClient)
                _metricsRepository.value = MetricsRepository(newClient)

                val namespaces = withContext(Dispatchers.IO) {
                    newClient.namespaces().list().items.map { it.metadata.name }
                }

                val savedNamespace = connectionStore.getActiveNamespace()
                val activeNs = when {
                    // Restore saved namespace (empty string = all namespaces)
                    savedNamespace != null && (savedNamespace.isEmpty() || namespaces.contains(savedNamespace)) -> savedNamespace
                    namespaces.contains("default") -> "default"
                    else -> namespaces.firstOrNull() ?: "default"
                }

                _uiState.update {
                    it.copy(
                        isConnected = true,
                        namespaces = ContentState.Success(namespaces),
                        activeNamespace = activeNs,
                    )
                }
                startAnomalyPolling()
            } catch (e: CancellationException) {
                throw e
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
        connectionStore.setActiveNamespace(namespace)
        startAnomalyPolling()
    }

    private fun startAnomalyPolling() {
        anomalyJob?.cancel()
        val repo = _repository.value ?: return
        val checker = AnomalyChecker(repo)
        anomalyJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val namespace = _uiState.value.activeNamespace
                    val anomalies = withContext(Dispatchers.IO) {
                        checker.check(namespace)
                    }
                    _uiState.update { it.copy(tabAnomalies = anomalies) }
                } catch (_: CancellationException) {
                    throw CancellationException()
                } catch (_: Exception) {
                    // Ignore errors in background polling
                }
                delay(60_000)
            }
        }
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
        anomalyJob?.cancel()
        client?.close()
    }
}
