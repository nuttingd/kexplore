package dev.nutting.kexplore.ui.screen.resources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nutting.kexplore.data.cache.ResourceCache
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
    val lastUpdated: Long? = null,
    val selectedItems: Set<String> = emptySet(),
    val selectionMode: Boolean = false,
    val bulkDeleteInProgress: Boolean = false,
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
        cache: ResourceCache? = null,
        connectionId: String? = null,
    ) {
        loadJob?.cancel()
        if (!isConnected || repository == null) {
            // Try showing cached data even when disconnected
            if (cache != null && connectionId != null) {
                val cached = cache.get(connectionId, namespace, type)
                if (cached != null) {
                    _state.update {
                        it.copy(
                            resources = ContentState.Success(cached.items),
                            isRefreshing = false,
                            lastUpdated = cached.lastUpdatedMillis,
                        )
                    }
                    return
                }
            }
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

        // Show cached data immediately while refreshing
        if (!isRefresh && cache != null && connectionId != null) {
            val cached = cache.get(connectionId, namespace, type)
            if (cached != null) {
                _state.update {
                    it.copy(
                        resources = ContentState.Success(cached.items),
                        isRefreshing = true,
                        lastUpdated = cached.lastUpdatedMillis,
                    )
                }
            } else {
                _state.update { it.copy(resources = ContentState.Loading, isRefreshing = false) }
            }
        } else {
            _state.update {
                it.copy(
                    resources = if (isRefresh) it.resources else ContentState.Loading,
                    isRefreshing = isRefresh,
                )
            }
        }

        loadJob = viewModelScope.launch {
            try {
                val effectiveNamespace = if (type.isClusterScoped) "" else namespace
                val result = repository.getResources(effectiveNamespace, type)
                val now = System.currentTimeMillis()

                // Update cache
                if (cache != null && connectionId != null) {
                    cache.put(connectionId, namespace, type, result)
                }

                _state.update {
                    it.copy(
                        resources = ContentState.Success(result),
                        isRefreshing = false,
                        lastUpdated = now,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // If we have cached data, keep showing it
                val current = _state.value
                if (current.resources is ContentState.Success) {
                    _state.update { it.copy(isRefreshing = false) }
                } else {
                    _state.update {
                        it.copy(
                            resources = ContentState.Error(ErrorMapper.map(e)),
                            isRefreshing = false,
                        )
                    }
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
        cache: ResourceCache? = null,
        connectionId: String? = null,
    ) {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                loadResources(repository, namespace, type, isConnected, connectionError, isRefresh = true, cache = cache, connectionId = connectionId)
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
    }

    fun enterSelectionMode(key: String) {
        _state.update {
            it.copy(selectionMode = true, selectedItems = setOf(key))
        }
    }

    fun toggleSelection(key: String) {
        _state.update {
            val newSet = if (key in it.selectedItems) {
                it.selectedItems - key
            } else {
                it.selectedItems + key
            }
            if (newSet.isEmpty()) {
                it.copy(selectedItems = newSet, selectionMode = false)
            } else {
                it.copy(selectedItems = newSet)
            }
        }
    }

    fun selectAll(items: List<ResourceSummary>) {
        _state.update {
            it.copy(selectedItems = items.map { s -> "${s.namespace}/${s.name}" }.toSet())
        }
    }

    fun clearSelection() {
        _state.update {
            it.copy(selectedItems = emptySet(), selectionMode = false)
        }
    }

    fun bulkDelete(
        repository: KubernetesRepository?,
        items: List<ResourceSummary>,
        onComplete: (success: Int, fail: Int) -> Unit,
    ) {
        if (repository == null) return
        viewModelScope.launch {
            _state.update { it.copy(bulkDeleteInProgress = true) }
            var success = 0
            var fail = 0
            for (item in items) {
                try {
                    repository.deleteResource(item.namespace, item.kind, item.name)
                    success++
                } catch (_: Exception) {
                    fail++
                }
            }
            _state.update {
                it.copy(
                    bulkDeleteInProgress = false,
                    selectedItems = emptySet(),
                    selectionMode = false,
                )
            }
            onComplete(success, fail)
        }
    }
}
