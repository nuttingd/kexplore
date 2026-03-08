package dev.nutting.kexplore.ui.screen.exec

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nutting.kexplore.data.kubernetes.ExecSession
import dev.nutting.kexplore.data.kubernetes.KubernetesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.nutting.kexplore.util.ErrorMapper
import kotlin.coroutines.cancellation.CancellationException

private val ANSI_ESCAPE_REGEX = Regex("\u001b\\[[0-9;]*[a-zA-Z]")
private const val MAX_LINES = 5000

private fun stripAnsi(text: String): String = ANSI_ESCAPE_REGEX.replace(text, "")

data class PodExecState(
    val lines: List<String> = emptyList(),
    val commandHistory: List<String> = emptyList(),
    val error: String? = null,
    val isConnected: Boolean = false,
)

class PodExecViewModel : ViewModel() {

    private val _state = MutableStateFlow(PodExecState())
    val state: StateFlow<PodExecState> = _state.asStateFlow()

    private var session: ExecSession? = null

    fun connect(
        repository: KubernetesRepository?,
        namespace: String,
        podName: String,
        container: String?,
    ) {
        if (repository == null) {
            _state.update { it.copy(error = "Not connected") }
            return
        }

        viewModelScope.launch {
            try {
                val execSession = withContext(Dispatchers.IO) {
                    repository.execInteractive(namespace, podName, container)
                }
                session = execSession
                _state.update {
                    it.copy(
                        isConnected = true,
                        lines = it.lines + "Connected to $podName (interactive shell)",
                    )
                }

                // Read stdout in background
                launch(Dispatchers.IO) {
                    execSession.stdout.bufferedReader().use { reader ->
                        val charBuf = CharArray(4096)
                        while (isActive) {
                            val count = try {
                                reader.read(charBuf)
                            } catch (_: Exception) {
                                -1
                            }
                            if (count == -1) break
                            val text = stripAnsi(String(charBuf, 0, count))
                            val newLines = text.split("\n")
                            _state.update { it.copy(lines = (it.lines + newLines).takeLast(MAX_LINES)) }
                        }
                    }
                }

                // Read stderr in background
                launch(Dispatchers.IO) {
                    execSession.stderr.bufferedReader().use { reader ->
                        val charBuf = CharArray(4096)
                        while (isActive) {
                            val count = try {
                                reader.read(charBuf)
                            } catch (_: Exception) {
                                -1
                            }
                            if (count == -1) break
                            val text = stripAnsi(String(charBuf, 0, count))
                            val newLines = text.split("\n")
                            _state.update { it.copy(lines = (it.lines + newLines).takeLast(MAX_LINES)) }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to start shell: ${ErrorMapper.map(e)}") }
            }
        }
    }

    fun sendCommand(command: String) {
        val s = session ?: return
        if (command.isBlank()) return
        _state.update { it.copy(commandHistory = it.commandHistory + command) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                s.stdin.write("$command\n".toByteArray())
                s.stdin.flush()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "Write error: ${ErrorMapper.map(e)}") }
            }
        }
    }

    fun closeSession() {
        session?.close()
        session = null
    }

    override fun onCleared() {
        super.onCleared()
        closeSession()
    }
}
