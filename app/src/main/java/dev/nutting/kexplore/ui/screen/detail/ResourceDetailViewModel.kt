package dev.nutting.kexplore.ui.screen.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.data.kubernetes.ResourceMappers
import dev.nutting.kexplore.data.model.ContentState
import dev.nutting.kexplore.data.model.ResourceDetail
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.util.ErrorMapper
import dev.nutting.kexplore.util.YamlSerializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class ResourceDetailState(
    val detail: ContentState<ResourceDetail> = ContentState.Loading,
    val yaml: ContentState<String> = ContentState.Loading,
)

class ResourceDetailViewModel : ViewModel() {

    private val _state = MutableStateFlow(ResourceDetailState())
    val state: StateFlow<ResourceDetailState> = _state.asStateFlow()

    private var currentNamespace: String? = null
    private var currentType: ResourceType? = null
    private var currentName: String? = null

    fun loadResource(
        repository: KubernetesRepository?,
        namespace: String,
        type: ResourceType,
        name: String,
    ) {
        currentNamespace = namespace
        currentType = type
        currentName = name

        if (repository == null) {
            _state.update {
                it.copy(
                    detail = ContentState.Error("Not connected"),
                    yaml = ContentState.Error("Not connected"),
                )
            }
            return
        }

        _state.update {
            it.copy(detail = ContentState.Loading, yaml = ContentState.Loading)
        }

        viewModelScope.launch {
            try {
                val resource = repository.getResource(namespace, type, name)
                val detail = ResourceMappers.toDetail(resource, type)
                val yaml = YamlSerializer.toYaml(resource)
                _state.update {
                    it.copy(
                        detail = ContentState.Success(detail),
                        yaml = ContentState.Success(yaml),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = ErrorMapper.map(e)
                _state.update {
                    it.copy(
                        detail = ContentState.Error(msg),
                        yaml = ContentState.Error(msg),
                    )
                }
            }
        }
    }

    fun retry(repository: KubernetesRepository?) {
        val ns = currentNamespace ?: return
        val type = currentType ?: return
        val name = currentName ?: return
        loadResource(repository, ns, type, name)
    }
}
