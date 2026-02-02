package dev.nutting.kexplore.data.portforward

import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.LocalPortForward
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.BindException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PortForwardManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _sessions = MutableStateFlow<List<PortForwardSession>>(emptyList())
    val sessions: StateFlow<List<PortForwardSession>> = _sessions.asStateFlow()

    private val activeForwards = ConcurrentHashMap<String, LocalPortForward>()
    private var healthCheckJob: Job? = null

    fun startForward(
        client: KubernetesClient,
        connectionId: String,
        namespace: String,
        podName: String,
        remotePort: Int,
        localPort: Int? = null,
        targetType: TargetType = TargetType.Pod,
        serviceName: String? = null,
    ): String {
        val sessionId = UUID.randomUUID().toString()
        val session = PortForwardSession(
            id = sessionId,
            connectionId = connectionId,
            namespace = namespace,
            podName = podName,
            remotePort = remotePort,
            localPort = localPort ?: 0,
            targetType = targetType,
            serviceName = serviceName,
        )

        _sessions.update { it + session }

        scope.launch {
            try {
                val forward = if (localPort != null && localPort > 0) {
                    client.pods()
                        .inNamespace(namespace)
                        .withName(podName)
                        .portForward(remotePort, localPort)
                } else {
                    client.pods()
                        .inNamespace(namespace)
                        .withName(podName)
                        .portForward(remotePort)
                }

                activeForwards[sessionId] = forward

                _sessions.update { list ->
                    list.map {
                        if (it.id == sessionId) it.copy(
                            status = ForwardStatus.Active,
                            localPort = forward.localPort,
                        ) else it
                    }
                }

                ensureHealthCheck()
            } catch (e: BindException) {
                _sessions.update { list ->
                    list.map {
                        if (it.id == sessionId) it.copy(
                            status = ForwardStatus.Failed,
                            error = "Port ${localPort ?: "auto"} already in use: ${e.message}",
                        ) else it
                    }
                }
            } catch (e: Exception) {
                val errorDetail = buildString {
                    append(e::class.simpleName ?: "Unknown")
                    append(": ")
                    append(e.message ?: "no message")
                    e.cause?.let { cause ->
                        append(" [caused by ${cause::class.simpleName}: ${cause.message}]")
                    }
                }
                _sessions.update { list ->
                    list.map {
                        if (it.id == sessionId) it.copy(
                            status = ForwardStatus.Failed,
                            error = errorDetail,
                        ) else it
                    }
                }
            }
        }

        return sessionId
    }

    fun stopForward(sessionId: String) {
        activeForwards.remove(sessionId)?.let { forward ->
            runCatching { forward.close() }
        }
        _sessions.update { list ->
            list.map {
                if (it.id == sessionId) it.copy(status = ForwardStatus.Stopped) else it
            }
        }
    }

    fun stopAll() {
        healthCheckJob?.cancel()
        activeForwards.forEach { (_, forward) ->
            runCatching { forward.close() }
        }
        activeForwards.clear()
        _sessions.update { list ->
            list.map {
                if (it.status == ForwardStatus.Active || it.status == ForwardStatus.Starting) {
                    it.copy(status = ForwardStatus.Stopped)
                } else it
            }
        }
    }

    fun stopForConnection(connectionId: String) {
        val toStop = _sessions.value.filter { it.connectionId == connectionId && it.status == ForwardStatus.Active }
        toStop.forEach { stopForward(it.id) }
    }

    fun removeSession(sessionId: String) {
        stopForward(sessionId)
        _sessions.update { list -> list.filter { it.id != sessionId } }
    }

    val activeCount: Int
        get() = _sessions.value.count { it.status == ForwardStatus.Active || it.status == ForwardStatus.Starting }

    private fun ensureHealthCheck() {
        if (healthCheckJob?.isActive == true) return
        healthCheckJob = scope.launch {
            while (isActive) {
                delay(5_000)
                val deadSessions = mutableListOf<String>()
                activeForwards.forEach { (id, forward) ->
                    if (!forward.isAlive) {
                        deadSessions.add(id)
                    }
                }
                for (id in deadSessions) {
                    activeForwards.remove(id)?.let { runCatching { it.close() } }
                    _sessions.update { list ->
                        list.map {
                            if (it.id == id) it.copy(
                                status = ForwardStatus.Failed,
                                error = "Connection lost",
                            ) else it
                        }
                    }
                }
                if (activeForwards.isEmpty()) break
            }
        }
    }
}
