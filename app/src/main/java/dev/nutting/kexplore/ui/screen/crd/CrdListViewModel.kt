package dev.nutting.kexplore.ui.screen.crd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nutting.kexplore.data.kubernetes.CrdRepository
import dev.nutting.kexplore.data.model.ContentState
import dev.nutting.kexplore.data.model.CrdDefinition
import dev.nutting.kexplore.util.ErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class CrdListState(
    val crds: ContentState<List<CrdDefinition>> = ContentState.Loading,
    val isRefreshing: Boolean = false,
)

class CrdListViewModel : ViewModel() {

    private val _state = MutableStateFlow(CrdListState())
    val state: StateFlow<CrdListState> = _state.asStateFlow()

    fun loadCrds(crdRepository: CrdRepository?, isRefresh: Boolean = false) {
        if (crdRepository == null) {
            _state.update {
                it.copy(crds = ContentState.Error("Not connected"), isRefreshing = false)
            }
            return
        }

        _state.update {
            it.copy(
                crds = if (isRefresh) it.crds else ContentState.Loading,
                isRefreshing = isRefresh,
            )
        }

        viewModelScope.launch {
            try {
                val crds = crdRepository.listCrds()
                _state.update {
                    it.copy(crds = ContentState.Success(crds), isRefreshing = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(crds = ContentState.Error(ErrorMapper.map(e)), isRefreshing = false)
                }
            }
        }
    }
}
