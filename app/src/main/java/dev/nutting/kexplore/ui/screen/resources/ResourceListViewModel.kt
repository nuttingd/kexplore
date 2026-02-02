package dev.nutting.kexplore.ui.screen.resources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.data.model.ContentState
import dev.nutting.kexplore.data.model.ResourceSummary
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

data class ResourceListState(
    val resources: ContentState<List<ResourceSummary>> = ContentState.Loading,
    val isRefreshing: Boolean = false,
)

class ResourceListViewModel : ViewModel() {

    private val _state = MutableStateFlow(ResourceListState())
    val state: StateFlow<ResourceListState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var autoRefreshJob: Job? = null

    fun loadResources(
        repository: KubernetesRepository?,
        namespace: String,
        type: ResourceType,
        isConnected: Boolean,
        connectionError: String?,
        isRefresh: Boolean = false,
    ) {
        loadJob?.cancel()
        if (!isConnected || repository == null) {
            _state.update {
                it.copy(
                    resources = if (connectionError != null) {
                        ContentState.Error(connectionError)
                    } else {
                        ContentState.Loading
                    },
                    isRefreshing = false,
                )
            }
            return
        }

        _state.update {
            it.copy(
                resources = if (isRefresh) it.resources else ContentState.Loading,
                isRefreshing = isRefresh,
            )
        }

        loadJob = viewModelScope.launch {
            try {
                val effectiveNamespace = if (type.isClusterScoped) "" else namespace
                val result = repository.getResources(effectiveNamespace, type)
                _state.update {
                    it.copy(resources = ContentState.Success(result), isRefreshing = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(resources = ContentState.Error(ErrorMapper.map(e)), isRefreshing = false)
                }
            }
        }
    }

    fun startAutoRefresh(
        repository: KubernetesRepository?,
        namespace: String,
        type: ResourceType,
        isConnected: Boolean,
        connectionError: String?,
    ) {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                loadResources(repository, namespace, type, isConnected, connectionError, isRefresh = true)
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
    }
}
