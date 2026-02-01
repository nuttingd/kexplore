package dev.nutting.kexplore.data.kubernetes

import io.fabric8.kubernetes.client.internal.KubeConfigUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class KubeconfigParsingTest {

    private val sampleKubeconfig = """
apiVersion: v1
kind: Config
current-context: dev-context
clusters:
- cluster:
    server: https://dev.example.com:6443
  name: dev-cluster
- cluster:
    server: https://prod.example.com:6443
  name: prod-cluster
contexts:
- context:
    cluster: dev-cluster
    user: dev-user
    namespace: default
  name: dev-context
- context:
    cluster: prod-cluster
    user: prod-user
    namespace: production
  name: prod-context
users:
- name: dev-user
  user:
    token: dev-token-123
- name: prod-user
  user:
    token: prod-token-456
""".trimIndent()

    @Test
    fun `parseConfigFromString extracts all contexts`() {
        val config = KubeConfigUtils.parseConfigFromString(sampleKubeconfig)
        assertEquals(2, config.contexts.size)
    }

    @Test
    fun `parseConfigFromString extracts context names`() {
        val config = KubeConfigUtils.parseConfigFromString(sampleKubeconfig)
        val names = config.contexts.map { it.name }
        assertEquals(listOf("dev-context", "prod-context"), names)
    }

    @Test
    fun `parseConfigFromString extracts cluster references`() {
        val config = KubeConfigUtils.parseConfigFromString(sampleKubeconfig)
        val devContext = config.contexts.find { it.name == "dev-context" }
        assertNotNull(devContext)
        assertEquals("dev-cluster", devContext!!.context.cluster)
    }

    @Test
    fun `parseConfigFromString extracts user references`() {
        val config = KubeConfigUtils.parseConfigFromString(sampleKubeconfig)
        val prodContext = config.contexts.find { it.name == "prod-context" }
        assertNotNull(prodContext)
        assertEquals("prod-user", prodContext!!.context.user)
    }

    @Test
    fun `parseConfigFromString extracts clusters`() {
        val config = KubeConfigUtils.parseConfigFromString(sampleKubeconfig)
        assertEquals(2, config.clusters.size)
        val devCluster = config.clusters.find { it.name == "dev-cluster" }
        assertNotNull(devCluster)
        assertEquals("https://dev.example.com:6443", devCluster!!.cluster.server)
    }

    @Test
    fun `parseConfigFromString extracts users`() {
        val config = KubeConfigUtils.parseConfigFromString(sampleKubeconfig)
        assertEquals(2, config.users.size)
    }

    @Test
    fun `parseConfigFromString extracts current context`() {
        val config = KubeConfigUtils.parseConfigFromString(sampleKubeconfig)
        assertEquals("dev-context", config.currentContext)
    }

    @Test
    fun `parseConfigFromString handles single context`() {
        val singleContext = """
apiVersion: v1
kind: Config
current-context: single
clusters:
- cluster:
    server: https://single.example.com
  name: single-cluster
contexts:
- context:
    cluster: single-cluster
    user: single-user
  name: single
users:
- name: single-user
  user:
    token: abc
""".trimIndent()

        val config = KubeConfigUtils.parseConfigFromString(singleContext)
        assertEquals(1, config.contexts.size)
        assertEquals("single", config.contexts[0].name)
    }
}
