package dev.nutting.kexplore.ui.screen.portforward

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nutting.kexplore.KexploreApp
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.data.portforward.ForwardStatus
import dev.nutting.kexplore.data.portforward.PortForwardManager
import dev.nutting.kexplore.data.portforward.PortForwardService
import dev.nutting.kexplore.data.portforward.PortForwardSession
import dev.nutting.kexplore.data.portforward.TargetType
import io.fabric8.kubernetes.client.KubernetesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PortForwardUiState(
    val pods: List<PodInfo> = emptyList(),
    val services: List<ServiceInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class PodInfo(
    val name: String,
    val namespace: String,
    val ports: List<Int>,
)

data class ServiceInfo(
    val name: String,
    val namespace: String,
    val ports: List<Int>,
    val selector: Map<String, String>,
)

class PortForwardViewModel(application: Application) : AndroidViewModel(application) {

    private val app = getApplication<KexploreApp>()
    val manager: PortForwardManager = app.portForwardManager

    private val _uiState = MutableStateFlow(PortForwardUiState())
    val uiState: StateFlow<PortForwardUiState> = _uiState.asStateFlow()

    val sessions: StateFlow<List<PortForwardSession>> = manager.sessions

    fun loadResources(repository: KubernetesRepository?, namespace: String) {
        if (repository == null) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val podSummaries = repository.getResourcesRaw(namespace, ResourceType.Pod)
                val pods = podSummaries.filterIsInstance<io.fabric8.kubernetes.api.model.Pod>().map { pod ->
                    val ports = pod.spec?.containers?.flatMap { container ->
                        container.ports?.map { it.containerPort } ?: emptyList()
                    } ?: emptyList()
                    PodInfo(
                        name = pod.metadata.name,
                        namespace = pod.metadata.namespace,
                        ports = ports,
                    )
                }

                val serviceSummaries = repository.getResourcesRaw(namespace, ResourceType.Service)
                val services = serviceSummaries.filterIsInstance<io.fabric8.kubernetes.api.model.Service>().map { svc ->
                    val ports = svc.spec?.ports?.map { it.port } ?: emptyList()
                    ServiceInfo(
                        name = svc.metadata.name,
                        namespace = svc.metadata.namespace,
                        ports = ports,
                        selector = svc.spec?.selector ?: emptyMap(),
                    )
                }

                _uiState.update {
                    it.copy(pods = pods, services = services, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load resources")
                }
            }
        }
    }

    fun startForward(
        client: KubernetesClient,
        connectionId: String,
        namespace: String,
        podName: String,
        remotePort: Int,
        localPort: Int?,
        targetType: TargetType = TargetType.Pod,
        serviceName: String? = null,
    ) {
        manager.startForward(
            client = client,
            connectionId = connectionId,
            namespace = namespace,
            podName = podName,
            remotePort = remotePort,
            localPort = localPort,
            targetType = targetType,
            serviceName = serviceName,
        )
        ensureServiceRunning()
    }

    fun resolveServiceToPod(
        repository: KubernetesRepository?,
        service: ServiceInfo,
        callback: (podName: String?) -> Unit,
    ) {
        if (repository == null) {
            callback(null)
            return
        }
        viewModelScope.launch {
            try {
                val pods = withContext(Dispatchers.IO) {
                    repository.getResourcesRaw(service.namespace, ResourceType.Pod)
                        .filterIsInstance<io.fabric8.kubernetes.api.model.Pod>()
                }
                val matchingPod = pods.find { pod ->
                    val podLabels = pod.metadata.labels ?: emptyMap()
                    service.selector.all { (k, v) -> podLabels[k] == v }
                }
                callback(matchingPod?.metadata?.name)
            } catch (_: Exception) {
                callback(null)
            }
        }
    }

    fun stopForward(sessionId: String) {
        manager.stopForward(sessionId)
    }

    fun removeSession(sessionId: String) {
        manager.removeSession(sessionId)
    }

    private fun ensureServiceRunning() {
        val intent = Intent(app, PortForwardService::class.java)
        app.startForegroundService(intent)
    }
}
