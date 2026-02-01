package dev.nutting.kexplore.util

import io.fabric8.kubernetes.client.KubernetesClientException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object ErrorMapper {
    fun map(throwable: Throwable): String = when (throwable) {
        is UnknownHostException -> "Cannot resolve server address. Check the URL."
        is ConnectException -> "Cannot connect to cluster. Is the server reachable?"
        is SocketTimeoutException -> "Connection timed out. The server may be unreachable."
        is SSLException -> "TLS/SSL error. Try enabling \"Skip TLS verification\" for this connection."
        is KubernetesClientException -> {
            when (throwable.code) {
                401 -> "Authentication failed. Check your credentials."
                403 -> "Access denied. You don't have permission for this resource."
                404 -> "Resource not found."
                else -> throwable.message ?: "Kubernetes API error (${throwable.code})."
            }
        }
        else -> throwable.message ?: "An unexpected error occurred."
    }
}
