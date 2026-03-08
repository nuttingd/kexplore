package dev.nutting.kexplore.ui.screen.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.data.model.ClusterHealth
import dev.nutting.kexplore.data.model.ContentState
import dev.nutting.kexplore.data.model.ResourceStatus
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.util.ErrorMapper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class HealthDashboardState(
    val health: ContentState<ClusterHealth> = ContentState.Loading,
    val isRefreshing: Boolean = false,
)

class HealthDashboardViewModel : ViewModel() {

    private val _state = MutableStateFlow(HealthDashboardState())
    val state: StateFlow<HealthDashboardState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var autoRefreshJob: Job? = null

    fun loadHealth(
        repository: KubernetesRepository?,
        namespace: String,
        isRefresh: Boolean = false,
    ) {
        loadJob?.cancel()
        if (repository == null) {
            _state.update { it.copy(health = ContentState.Error("Not connected"), isRefreshing = false) }
            return
        }

        _state.update {
            it.copy(
                health = if (isRefresh) it.health else ContentState.Loading,
                isRefreshing = isRefresh,
            )
        }

        loadJob = viewModelScope.launch {
            try {
                val failedPods = try {
                    repository.getResources(namespace, ResourceType.Pod)
                        .filter { it.status == ResourceStatus.Failed }
                } catch (_: Exception) { emptyList() }

                val unhealthyDeployments = try {
                    repository.getResources(namespace, ResourceType.Deployment)
                        .filter { summary ->
                            val ready = summary.readyCount ?: return@filter false
                            val parts = ready.split("/")
                            parts.size == 2 && parts[0].trim() != parts[1].trim()
                        }
                } catch (_: Exception) { emptyList() }

                val unhealthyNodes = try {
                    repository.getResources("", ResourceType.Node)
                        .filter { it.status != ResourceStatus.Running }
                } catch (_: Exception) { emptyList() }

                val warningEvents = try {
                    repository.getResources(namespace, ResourceType.Event)
                        .filter { it.status == ResourceStatus.Failed }
                        .take(50)
                } catch (_: Exception) { emptyList() }

                val health = ClusterHealth(
                    failedPods = failedPods,
                    unhealthyDeployments = unhealthyDeployments,
                    unhealthyNodes = unhealthyNodes,
                    warningEvents = warningEvents,
                )
                _state.update { it.copy(health = ContentState.Success(health), isRefreshing = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(health = ContentState.Error(ErrorMapper.map(e)), isRefreshing = false)
                }
            }
        }
    }

    fun startAutoRefresh(repository: KubernetesRepository?, namespace: String) {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                loadHealth(repository, namespace, isRefresh = true)
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        autoRefreshJob?.cancel()
    }
}
