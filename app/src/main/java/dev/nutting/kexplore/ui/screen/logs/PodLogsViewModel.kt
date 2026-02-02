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
    // Search state
    val searchQuery: String = "",
    val isRegex: Boolean = false,
    val searchMatchIndices: List<Int> = emptyList(),
    val currentMatchIndex: Int = -1,
    val searchVisible: Boolean = false,
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

    // Search methods
    fun toggleSearch() {
        _state.update {
            val newVisible = !it.searchVisible
            if (!newVisible) {
                it.copy(
                    searchVisible = false,
                    searchQuery = "",
                    isRegex = false,
                    searchMatchIndices = emptyList(),
                    currentMatchIndex = -1,
                )
            } else {
                it.copy(searchVisible = true)
            }
        }
    }

    fun setSearch(query: String) {
        _state.update { state ->
            val indices = computeMatchIndices(state.lines, query, state.isRegex)
            state.copy(
                searchQuery = query,
                searchMatchIndices = indices,
                currentMatchIndex = if (indices.isNotEmpty()) 0 else -1,
                isFollowing = if (query.isNotBlank()) false else state.isFollowing,
            )
        }
    }

    fun toggleRegex() {
        _state.update { state ->
            val newIsRegex = !state.isRegex
            val indices = computeMatchIndices(state.lines, state.searchQuery, newIsRegex)
            state.copy(
                isRegex = newIsRegex,
                searchMatchIndices = indices,
                currentMatchIndex = if (indices.isNotEmpty()) 0 else -1,
            )
        }
    }

    fun nextMatch() {
        _state.update { state ->
            if (state.searchMatchIndices.isEmpty()) return@update state
            val next = (state.currentMatchIndex + 1) % state.searchMatchIndices.size
            state.copy(currentMatchIndex = next)
        }
    }

    fun previousMatch() {
        _state.update { state ->
            if (state.searchMatchIndices.isEmpty()) return@update state
            val prev = if (state.currentMatchIndex <= 0) state.searchMatchIndices.size - 1
            else state.currentMatchIndex - 1
            state.copy(currentMatchIndex = prev)
        }
    }

    private fun computeMatchIndices(lines: List<String>, query: String, isRegex: Boolean): List<Int> {
        if (query.isBlank()) return emptyList()
        val pattern = try {
            if (isRegex) Regex(query, RegexOption.IGNORE_CASE)
            else Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
        } catch (_: Exception) {
            return emptyList()
        }
        return lines.indices.filter { pattern.containsMatchIn(lines[it]) }
    }

    private fun recomputeSearchMatches() {
        val currentState = _state.value
        if (currentState.searchQuery.isNotBlank()) {
            val indices = computeMatchIndices(currentState.lines, currentState.searchQuery, currentState.isRegex)
            _state.update { state ->
                state.copy(
                    searchMatchIndices = indices,
                    currentMatchIndex = if (indices.isEmpty()) -1
                    else state.currentMatchIndex.coerceIn(0, indices.size - 1),
                )
            }
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
                    recomputeSearchMatches()
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
