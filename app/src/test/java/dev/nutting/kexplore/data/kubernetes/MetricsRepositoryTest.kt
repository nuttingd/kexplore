package dev.nutting.kexplore.data.kubernetes

import io.fabric8.kubernetes.api.model.Quantity
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricsRepositoryTest {

    @Test
    fun `quantityToMillicores returns 0 for null`() {
        assertEquals(0L, MetricsRepository.quantityToMillicores(null))
    }

    @Test
    fun `quantityToMillicores converts millicores string`() {
        val q = Quantity("250m")
        assertEquals(250L, MetricsRepository.quantityToMillicores(q))
    }

    @Test
    fun `quantityToMillicores converts whole cores`() {
        val q = Quantity("2")
        assertEquals(2000L, MetricsRepository.quantityToMillicores(q))
    }

    @Test
    fun `quantityToMillicores converts nanocores`() {
        val q = Quantity("100n")
        // 100n = 0.0001 millicores, truncated to 0
        assertEquals(0L, MetricsRepository.quantityToMillicores(q))
    }

    @Test
    fun `quantityToMillicores converts microcores`() {
        val q = Quantity("500000u")
        assertEquals(500L, MetricsRepository.quantityToMillicores(q))
    }

    @Test
    fun `quantityToBytes returns 0 for null`() {
        assertEquals(0L, MetricsRepository.quantityToBytes(null))
    }

    @Test
    fun `quantityToBytes converts Ki`() {
        val q = Quantity("100Ki")
        assertEquals(102400L, MetricsRepository.quantityToBytes(q))
    }

    @Test
    fun `quantityToBytes converts Mi`() {
        val q = Quantity("256Mi")
        assertEquals(268435456L, MetricsRepository.quantityToBytes(q))
    }

    @Test
    fun `quantityToBytes converts Gi`() {
        val q = Quantity("1Gi")
        assertEquals(1073741824L, MetricsRepository.quantityToBytes(q))
    }

    @Test
    fun `quantityToBytes converts plain bytes`() {
        val q = Quantity("1024")
        assertEquals(1024L, MetricsRepository.quantityToBytes(q))
    }
}
