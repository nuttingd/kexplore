package dev.nutting.kexplore.ui.screen.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import dev.nutting.kexplore.util.DateFormatter
import dev.nutting.kexplore.util.ErrorMapper
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class EventItem(
    val uid: String,
    val type: String,
    val reason: String,
    val message: String,
    val involvedObject: String,
    val namespace: String,
    val timestamp: String,
    val count: Int,
    val age: String,
)

data class EventFilter(
    val type: String? = null, // "Warning", "Normal", or null for all
    val namespace: String? = null,
    val searchQuery: String = "",
)

data class EventStreamState(
    val events: List<EventItem> = emptyList(),
    val filter: EventFilter = EventFilter(),
    val isStreaming: Boolean = false,
    val autoScroll: Boolean = true,
    val error: String? = null,
)

class EventStreamViewModel : ViewModel() {

    companion object {
        private const val MAX_EVENTS = 500
        private const val BATCH_INTERVAL_MS = 50L
    }

    private val _state = MutableStateFlow(EventStreamState())
    val state: StateFlow<EventStreamState> = _state.asStateFlow()

    private var watchJob: Job? = null
    private var batchJob: Job? = null
    private val eventBuffer = Channel<EventItem>(Channel.UNLIMITED)

    fun start(repository: KubernetesRepository, namespace: String?) {
        watchJob?.cancel()
        _state.update { it.copy(isStreaming = true, error = null, events = emptyList()) }

        watchJob = viewModelScope.launch {
            startBatchConsumer()

            try {
                repository.watchEvents(namespace).collect { update ->
                    val event = update.event
                    val item = EventItem(
                        uid = event.metadata?.uid ?: "",
                        type = event.type ?: "Unknown",
                        reason = event.reason ?: "",
                        message = event.message ?: "",
                        involvedObject = buildString {
                            event.involvedObject?.let { obj ->
                                append(obj.kind ?: "")
                                append("/")
                                append(obj.name ?: "")
                            }
                        },
                        namespace = event.metadata?.namespace ?: "",
                        timestamp = event.lastTimestamp ?: event.metadata?.creationTimestamp ?: "",
                        count = event.count ?: 1,
                        age = DateFormatter.age(event.lastTimestamp ?: event.metadata?.creationTimestamp),
                    )
                    eventBuffer.send(item)
                }
                _state.update { it.copy(isStreaming = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isStreaming = false, error = ErrorMapper.map(e)) }
            }
        }
    }

    fun setFilter(filter: EventFilter) {
        _state.update { it.copy(filter = filter) }
    }

    fun setTypeFilter(type: String?) {
        _state.update { it.copy(filter = it.filter.copy(type = type)) }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(filter = it.filter.copy(searchQuery = query)) }
    }

    fun toggleAutoScroll() {
        _state.update { it.copy(autoScroll = !it.autoScroll) }
    }

    private fun startBatchConsumer() {
        batchJob?.cancel()
        batchJob = viewModelScope.launch {
            while (isActive) {
                delay(BATCH_INTERVAL_MS)
                val batch = mutableListOf<EventItem>()
                while (true) {
                    val item = eventBuffer.tryReceive().getOrNull() ?: break
                    batch.add(item)
                }
                if (batch.isNotEmpty()) {
                    _state.update {
                        it.copy(events = (it.events + batch).takeLast(MAX_EVENTS))
                    }
                }
            }
        }
    }

    fun stop() {
        watchJob?.cancel()
        batchJob?.cancel()
        _state.update { it.copy(isStreaming = false) }
    }

    override fun onCleared() {
        super.onCleared()
        watchJob?.cancel()
        batchJob?.cancel()
    }
}
