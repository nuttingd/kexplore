package dev.nutting.kexplore.ui.screen.crd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nutting.kexplore.data.kubernetes.CrdRepository
import dev.nutting.kexplore.data.model.ContentState
import dev.nutting.kexplore.data.model.CrdDefinition
import dev.nutting.kexplore.data.model.CustomResourceSummary
import dev.nutting.kexplore.util.ErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class CrdInstanceListState(
    val crd: ContentState<CrdDefinition> = ContentState.Loading,
    val instances: ContentState<List<CustomResourceSummary>> = ContentState.Loading,
    val isRefreshing: Boolean = false,
)

class CrdInstanceListViewModel : ViewModel() {

    private val _state = MutableStateFlow(CrdInstanceListState())
    val state: StateFlow<CrdInstanceListState> = _state.asStateFlow()

    private var currentCrd: CrdDefinition? = null

    fun loadInstances(
        crdRepository: CrdRepository?,
        crdName: String,
        namespace: String?,
        isRefresh: Boolean = false,
    ) {
        if (crdRepository == null) {
            _state.update {
                it.copy(
                    crd = ContentState.Error("Not connected"),
                    instances = ContentState.Error("Not connected"),
                    isRefreshing = false,
                )
            }
            return
        }

        _state.update {
            it.copy(
                instances = if (isRefresh) it.instances else ContentState.Loading,
                isRefreshing = isRefresh,
            )
        }

        viewModelScope.launch {
            try {
                // Fetch CRD metadata if not already loaded
                val crd = currentCrd ?: run {
                    val crds = crdRepository.listCrds()
                    crds.find { it.name == crdName }
                        ?: throw NoSuchElementException("CRD '$crdName' not found")
                }
                currentCrd = crd
                _state.update { it.copy(crd = ContentState.Success(crd)) }

                val instances = crdRepository.listInstances(crd, namespace)
                _state.update {
                    it.copy(instances = ContentState.Success(instances), isRefreshing = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = ErrorMapper.map(e)
                _state.update {
                    it.copy(
                        crd = if (it.crd is ContentState.Success) it.crd else ContentState.Error(msg),
                        instances = ContentState.Error(msg),
                        isRefreshing = false,
                    )
                }
            }
        }
    }
}
