package dev.nutting.kexplore.ui.screen.crd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nutting.kexplore.data.kubernetes.CrdRepository
import dev.nutting.kexplore.data.model.ContentState
import dev.nutting.kexplore.data.model.CrdDefinition
import dev.nutting.kexplore.util.ErrorMapper
import io.fabric8.kubernetes.api.model.GenericKubernetesResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class CrdInstanceDetailState(
    val resource: ContentState<GenericKubernetesResource> = ContentState.Loading,
    val yaml: ContentState<String> = ContentState.Loading,
)

class CrdInstanceDetailViewModel : ViewModel() {

    private val _state = MutableStateFlow(CrdInstanceDetailState())
    val state: StateFlow<CrdInstanceDetailState> = _state.asStateFlow()

    fun loadInstance(
        crdRepository: CrdRepository?,
        crdName: String,
        namespace: String?,
        name: String,
    ) {
        if (crdRepository == null) {
            _state.update {
                it.copy(
                    resource = ContentState.Error("Not connected"),
                    yaml = ContentState.Error("Not connected"),
                )
            }
            return
        }

        _state.update {
            it.copy(resource = ContentState.Loading, yaml = ContentState.Loading)
        }

        viewModelScope.launch {
            try {
                // First fetch CRD metadata
                val crds = crdRepository.listCrds()
                val crd = crds.find { it.name == crdName }
                    ?: throw NoSuchElementException("CRD '$crdName' not found")

                val resource = crdRepository.getInstance(crd, namespace, name)
                val yaml = crdRepository.getInstanceYaml(crd, namespace, name)
                _state.update {
                    it.copy(
                        resource = ContentState.Success(resource),
                        yaml = ContentState.Success(yaml),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = ErrorMapper.map(e)
                _state.update {
                    it.copy(
                        resource = ContentState.Error(msg),
                        yaml = ContentState.Error(msg),
                    )
                }
            }
        }
    }
}
