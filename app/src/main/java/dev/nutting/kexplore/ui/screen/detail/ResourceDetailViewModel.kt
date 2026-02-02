package dev.nutting.kexplore.ui.screen.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nutting.kexplore.data.kubernetes.DependencyResolver
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.data.kubernetes.ResourceMappers
import dev.nutting.kexplore.data.model.DependencyNode
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

sealed interface ActionResult {
    data class Success(val message: String) : ActionResult
    data class Error(val message: String) : ActionResult
}

data class ResourceDetailState(
    val detail: ContentState<ResourceDetail> = ContentState.Loading,
    val yaml: ContentState<String> = ContentState.Loading,
    val dependencies: ContentState<DependencyNode>? = null,
    val actionInProgress: Boolean = false,
    val actionResult: ActionResult? = null,
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

    fun loadDependencies(repository: KubernetesRepository?) {
        val ns = currentNamespace ?: return
        val type = currentType ?: return
        val name = currentName ?: return
        if (repository == null) return

        _state.update { it.copy(dependencies = ContentState.Loading) }
        viewModelScope.launch {
            try {
                val resolver = DependencyResolver(repository)
                val tree = resolver.resolveDependencies(ns, type, name)
                _state.update { it.copy(dependencies = ContentState.Success(tree)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(dependencies = ContentState.Error(ErrorMapper.map(e))) }
            }
        }
    }

    fun retry(repository: KubernetesRepository?) {
        val ns = currentNamespace ?: return
        val type = currentType ?: return
        val name = currentName ?: return
        loadResource(repository, ns, type, name)
    }

    fun dismissActionResult() {
        _state.update { it.copy(actionResult = null) }
    }

    fun deleteResource(repository: KubernetesRepository?, onSuccess: () -> Unit) {
        val ns = currentNamespace ?: return
        val type = currentType ?: return
        val name = currentName ?: return
        if (repository == null) return

        _state.update { it.copy(actionInProgress = true) }
        viewModelScope.launch {
            try {
                repository.deleteResource(ns, type, name)
                _state.update {
                    it.copy(
                        actionInProgress = false,
                        actionResult = ActionResult.Success("${type.displayName} '$name' deleted"),
                    )
                }
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        actionInProgress = false,
                        actionResult = ActionResult.Error(ErrorMapper.map(e)),
                    )
                }
            }
        }
    }

    fun scaleResource(repository: KubernetesRepository?, replicas: Int) {
        val ns = currentNamespace ?: return
        val type = currentType ?: return
        val name = currentName ?: return
        if (repository == null) return

        _state.update { it.copy(actionInProgress = true) }
        viewModelScope.launch {
            try {
                repository.scaleResource(ns, type, name, replicas)
                _state.update {
                    it.copy(
                        actionInProgress = false,
                        actionResult = ActionResult.Success("Scaled to $replicas replicas"),
                    )
                }
                loadResource(repository, ns, type, name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        actionInProgress = false,
                        actionResult = ActionResult.Error(ErrorMapper.map(e)),
                    )
                }
            }
        }
    }

    fun restartResource(repository: KubernetesRepository?) {
        val ns = currentNamespace ?: return
        val type = currentType ?: return
        val name = currentName ?: return
        if (repository == null) return

        _state.update { it.copy(actionInProgress = true) }
        viewModelScope.launch {
            try {
                repository.restartResource(ns, type, name)
                _state.update {
                    it.copy(
                        actionInProgress = false,
                        actionResult = ActionResult.Success("${type.displayName} restarting"),
                    )
                }
                loadResource(repository, ns, type, name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        actionInProgress = false,
                        actionResult = ActionResult.Error(ErrorMapper.map(e)),
                    )
                }
            }
        }
    }

    fun triggerCronJob(repository: KubernetesRepository?) {
        val ns = currentNamespace ?: return
        val name = currentName ?: return
        if (repository == null) return

        _state.update { it.copy(actionInProgress = true) }
        viewModelScope.launch {
            try {
                val jobName = repository.triggerCronJob(ns, name)
                _state.update {
                    it.copy(
                        actionInProgress = false,
                        actionResult = ActionResult.Success("Job '$jobName' created"),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        actionInProgress = false,
                        actionResult = ActionResult.Error(ErrorMapper.map(e)),
                    )
                }
            }
        }
    }

    fun cordonNode(repository: KubernetesRepository?) {
        val name = currentName ?: return
        if (repository == null) return

        _state.update { it.copy(actionInProgress = true) }
        viewModelScope.launch {
            try {
                repository.cordonNode(name)
                _state.update {
                    it.copy(
                        actionInProgress = false,
                        actionResult = ActionResult.Success("Node '$name' cordoned"),
                    )
                }
                loadResource(repository, currentNamespace ?: "", currentType ?: return@launch, name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        actionInProgress = false,
                        actionResult = ActionResult.Error(ErrorMapper.map(e)),
                    )
                }
            }
        }
    }

    fun uncordonNode(repository: KubernetesRepository?) {
        val name = currentName ?: return
        if (repository == null) return

        _state.update { it.copy(actionInProgress = true) }
        viewModelScope.launch {
            try {
                repository.uncordonNode(name)
                _state.update {
                    it.copy(
                        actionInProgress = false,
                        actionResult = ActionResult.Success("Node '$name' uncordoned"),
                    )
                }
                loadResource(repository, currentNamespace ?: "", currentType ?: return@launch, name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        actionInProgress = false,
                        actionResult = ActionResult.Error(ErrorMapper.map(e)),
                    )
                }
            }
        }
    }

    companion object {
        fun parseDesiredReplicas(replicasSpec: String?): Int? {
            if (replicasSpec == null) return null
            val match = Regex("(\\d+)\\s+ready\\s*/\\s*(\\d+)\\s+desired").find(replicasSpec)
            return match?.groupValues?.get(2)?.toIntOrNull()
        }
    }
}
