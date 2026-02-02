package dev.nutting.kexplore.ui.screen.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import dev.nutting.kexplore.data.kubernetes.MetricsRepository
import dev.nutting.kexplore.data.metrics.MetricsCollector
import dev.nutting.kexplore.data.model.ResourceMetricsSnapshot
import dev.nutting.kexplore.data.model.ResourceType
import dev.nutting.kexplore.ui.components.EmptyContent

private val INTERVAL_OPTIONS = listOf(1_000L, 2_000L, 5_000L, 10_000L, 30_000L)

private fun intervalLabel(ms: Long): String = if (ms < 60_000) "${ms / 1000}s" else "${ms / 60_000}m"

private fun windowSlots(intervalMs: Long): Int = ((5 * 60 * 1000) / intervalMs).toInt()

@Composable
fun MetricsTab(
    metricsRepository: MetricsRepository?,
    namespace: String,
    resourceName: String,
    resourceType: ResourceType,
) {
    if (metricsRepository == null) {
        EmptyContent(message = "Not connected")
        return
    }

    var pollIntervalMs by rememberSaveable { mutableLongStateOf(MetricsCollector.DEFAULT_POLL_INTERVAL_MS) }

    val scope = rememberCoroutineScope()
    val collector = remember(metricsRepository, namespace, resourceName, resourceType, pollIntervalMs) {
        MetricsCollector(metricsRepository, scope, pollIntervalMs)
    }

    DisposableEffect(collector) {
        if (resourceType == ResourceType.Node) {
            collector.startNodeMetrics(resourceName)
        } else {
            collector.startPodMetrics(namespace, resourceName)
        }
        onDispose { collector.stop() }
    }

    val snapshots by collector.snapshots.collectAsState()
    val metricsAvailable by collector.metricsAvailable.collectAsState()
    val nodeCapacity by collector.nodeCapacity.collectAsState()
    val slots = windowSlots(pollIntervalMs)

    when (metricsAvailable) {
        null -> EmptyContent(message ="Checking metrics availability...")
        false -> EmptyContent(message ="Metrics unavailable — install metrics-server")
        true -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                // Poll interval picker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Interval:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    INTERVAL_OPTIONS.forEach { option ->
                        FilterChip(
                            selected = pollIntervalMs == option,
                            onClick = { pollIntervalMs = option },
                            label = { Text(intervalLabel(option)) },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (snapshots.isEmpty()) {
                    EmptyContent(message = "Waiting for metrics data...")
                } else {
                    MetricsChartCard(
                        title = "CPU (millicores)",
                        snapshots = snapshots,
                        windowSlots = slots,
                        pollIntervalMs = pollIntervalMs,
                        valueExtractor = { it.cpuMillicores.toDouble() },
                        valueFormatter = { "${it.toLong()}m" },
                        capacityValue = if (resourceType == ResourceType.Node) nodeCapacity?.cpuCapacity?.toDouble() else null,
                    )

                    Spacer(Modifier.height(16.dp))

                    MetricsChartCard(
                        title = "Memory (MiB)",
                        snapshots = snapshots,
                        windowSlots = slots,
                        pollIntervalMs = pollIntervalMs,
                        valueExtractor = { it.memoryBytes.toDouble() / (1024 * 1024) },
                        valueFormatter = { "${it.toLong()} MiB" },
                        capacityValue = if (resourceType == ResourceType.Node) nodeCapacity?.memoryCapacity?.toDouble()?.div(1024 * 1024) else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricsChartCard(
    title: String,
    snapshots: List<ResourceMetricsSnapshot>,
    windowSlots: Int,
    pollIntervalMs: Long,
    valueExtractor: (ResourceMetricsSnapshot) -> Double,
    valueFormatter: (Double) -> String,
    capacityValue: Double?,
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    // Build a fixed-width series: windowSlots points where trailing slots
    // without data are 0. Data is right-aligned so new points appear on the right.
    LaunchedEffect(snapshots) {
        if (snapshots.isNotEmpty()) {
            val values = snapshots.map { valueExtractor(it) }
            val padded = if (values.size < windowSlots) {
                // Pad the left side with zero so the chart has a stable x-axis
                // width and real data "scrolls in" from the right.
                val fill = List(windowSlots - values.size) { 0.0 }
                fill + values
            } else {
                values.takeLast(windowSlots)
            }
            modelProducer.runTransaction {
                lineSeries { series(padded) }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )

            val latestValue = snapshots.lastOrNull()?.let { valueExtractor(it) }
            if (latestValue != null) {
                val label = if (capacityValue != null) {
                    "${valueFormatter(latestValue)} / ${valueFormatter(capacityValue)}"
                } else {
                    "Current: ${valueFormatter(latestValue)}"
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))

            CartesianChartHost(
                rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(),
                ),
                modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                scrollState = rememberVicoScrollState(scrollEnabled = false),
            )

            Text(
                text = "${snapshots.size} sample${if (snapshots.size != 1) "s" else ""} · polling every ${intervalLabel(pollIntervalMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
