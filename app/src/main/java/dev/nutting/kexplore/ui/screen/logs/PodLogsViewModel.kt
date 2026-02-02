package dev.nutting.kexplore.ui.screen.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.util.ErrorMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PodLogsState(
    val containers: List<String> = emptyList(),
    val selectedContainer: String? = null,
    val isFollowing: Boolean = true,
    val tailLines: Int = 500,
    val lines: List<String> = emptyList(),
    val isStreaming: Boolean = false,
    val error: String? = null,
)

class PodLogsViewModel : ViewModel() {

    companion object {
        private const val MAX_LINES = 5000
        private const val BATCH_INTERVAL_MS = 50L
    }

    private val _state = MutableStateFlow(PodLogsState())
    val state: StateFlow<PodLogsState> = _state.asStateFlow()

    private var streamJob: Job? = null
    private var batchJob: Job? = null
    private val lineBuffer = Channel<String>(Channel.UNLIMITED)

    fun initialize(
        repository: KubernetesRepository,
        namespace: String,
        podName: String,
    ) {
        viewModelScope.launch {
            try {
                val containers = withContext(Dispatchers.IO) {
                    repository.getContainerNames(namespace, podName)
                }
                _state.update {
                    it.copy(
                        containers = containers,
                        selectedContainer = containers.firstOrNull(),
                    )
                }
                startStreaming(repository, namespace, podName)
            } catch (e: Exception) {
                _state.update { it.copy(error = ErrorMapper.map(e)) }
            }
        }
    }

    fun selectContainer(
        container: String,
        repository: KubernetesRepository,
        namespace: String,
        podName: String,
    ) {
        _state.update { it.copy(selectedContainer = container, lines = emptyList(), error = null) }
        startStreaming(repository, namespace, podName)
    }

    fun setTailLines(
        lines: Int,
        repository: KubernetesRepository,
        namespace: String,
        podName: String,
    ) {
        _state.update { it.copy(tailLines = lines, lines = emptyList(), error = null) }
        startStreaming(repository, namespace, podName)
    }

    fun toggleFollowing() {
        _state.update { it.copy(isFollowing = !it.isFollowing) }
    }

    fun toggleStreaming(
        repository: KubernetesRepository,
        namespace: String,
        podName: String,
    ) {
        if (_state.value.isStreaming) {
            stopStreaming()
        } else {
            startStreaming(repository, namespace, podName)
        }
    }

    private fun startStreaming(
        repository: KubernetesRepository,
        namespace: String,
        podName: String,
    ) {
        streamJob?.cancel()
        val container = _state.value.selectedContainer
        val tailLines = _state.value.tailLines
        _state.update { it.copy(isStreaming = true, error = null) }

        startBatchConsumer()
        streamJob = viewModelScope.launch {
            try {
                repository.streamPodLogs(namespace, podName, container, tailLines).collect { line ->
                    lineBuffer.send(line)
                }
                _state.update { it.copy(isStreaming = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isStreaming = false, error = ErrorMapper.map(e)) }
            }
        }
    }

    private fun startBatchConsumer() {
        batchJob?.cancel()
        batchJob = viewModelScope.launch {
            while (isActive) {
                delay(BATCH_INTERVAL_MS)
                val batch = mutableListOf<String>()
                while (true) {
                    val line = lineBuffer.tryReceive().getOrNull() ?: break
                    batch.add(line)
                }
                if (batch.isNotEmpty()) {
                    _state.update { it.copy(lines = (it.lines + batch).takeLast(MAX_LINES)) }
                }
            }
        }
    }

    private fun stopStreaming() {
        streamJob?.cancel()
        batchJob?.cancel()
        _state.update { it.copy(isStreaming = false) }
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
        batchJob?.cancel()
    }
}
