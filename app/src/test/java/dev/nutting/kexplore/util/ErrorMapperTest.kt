package dev.nutting.kexplore.util

import io.fabric8.kubernetes.client.KubernetesClientException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class ErrorMapperTest {

    @Test
    fun `maps UnknownHostException to resolve message`() {
        val result = ErrorMapper.map(UnknownHostException("example.com"))
        assertTrue(result.contains("resolve"))
    }

    @Test
    fun `maps ConnectException to cannot connect message`() {
        val result = ErrorMapper.map(ConnectException("Connection refused"))
        assertTrue(result.contains("Cannot connect"))
    }

    @Test
    fun `maps SocketTimeoutException to timeout message`() {
        val result = ErrorMapper.map(SocketTimeoutException("timed out"))
        assertTrue(result.contains("timed out"))
    }

    @Test
    fun `maps SSLException to TLS message`() {
        val result = ErrorMapper.map(SSLException("handshake failed"))
        assertTrue(result.contains("TLS"))
    }

    @Test
    fun `maps KubernetesClientException 401 to auth message`() {
        val result = ErrorMapper.map(KubernetesClientException("Unauthorized", 401, null))
        assertTrue(result.contains("Authentication"))
    }

    @Test
    fun `maps KubernetesClientException 403 to access denied message`() {
        val result = ErrorMapper.map(KubernetesClientException("Forbidden", 403, null))
        assertTrue(result.contains("Access denied"))
    }

    @Test
    fun `maps KubernetesClientException 404 to not found message`() {
        val result = ErrorMapper.map(KubernetesClientException("Not Found", 404, null))
        assertTrue(result.contains("not found"))
    }

    @Test
    fun `maps KubernetesClientException other code to message`() {
        val result = ErrorMapper.map(KubernetesClientException("Server Error", 500, null))
        assertEquals("Server Error", result)
    }

    @Test
    fun `maps unknown exception to message`() {
        val result = ErrorMapper.map(RuntimeException("something broke"))
        assertEquals("something broke", result)
    }

    @Test
    fun `maps unknown exception with null message to fallback`() {
        val result = ErrorMapper.map(RuntimeException())
        assertEquals("An unexpected error occurred.", result)
    }
}
