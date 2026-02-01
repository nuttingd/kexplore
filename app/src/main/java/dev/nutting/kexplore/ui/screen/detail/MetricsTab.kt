package dev.nutting.kexplore.ui.screen.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import dev.nutting.kexplore.data.kubernetes.MetricsRepository
import dev.nutting.kexplore.data.metrics.MetricsCollector
import dev.nutting.kexplore.data.model.ResourceMetricsSnapshot
import dev.nutting.kexplore.data.model.ResourceType

private const val WINDOW_SLOTS = 60 // 60 slots × 5s = 5 min window
private const val POLL_INTERVAL_SEC = 5

@Composable
fun MetricsTab(
    metricsRepository: MetricsRepository?,
    namespace: String,
    resourceName: String,
    resourceType: ResourceType,
) {
    if (metricsRepository == null) {
        CenteredMessage("Not connected")
        return
    }

    val scope = rememberCoroutineScope()
    val collector = remember(metricsRepository, namespace, resourceName, resourceType) {
        MetricsCollector(metricsRepository, scope)
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

    when (metricsAvailable) {
        null -> CenteredMessage("Checking metrics availability...")
        false -> CenteredMessage("Metrics unavailable — install metrics-server")
        true -> {
            if (snapshots.isEmpty()) {
                CenteredMessage("Waiting for metrics data...")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    MetricsChartCard(
                        title = "CPU (millicores)",
                        snapshots = snapshots,
                        valueExtractor = { it.cpuMillicores.toFloat() },
                        valueFormatter = { "${it.toLong()}m" },
                        capacityValue = if (resourceType == ResourceType.Node) nodeCapacity?.cpuCapacity?.toFloat() else null,
                    )

                    Spacer(Modifier.height(16.dp))

                    MetricsChartCard(
                        title = "Memory (MiB)",
                        snapshots = snapshots,
                        valueExtractor = { it.memoryBytes.toFloat() / (1024 * 1024) },
                        valueFormatter = { "${it.toLong()} MiB" },
                        capacityValue = if (resourceType == ResourceType.Node) nodeCapacity?.memoryCapacity?.toFloat()?.div(1024 * 1024) else null,
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
    valueExtractor: (ResourceMetricsSnapshot) -> Float,
    valueFormatter: (Float) -> String,
    capacityValue: Float?,
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    // Build a fixed-width series: WINDOW_SLOTS points where trailing slots
    // without data are 0. Data is right-aligned so new points appear on the right.
    LaunchedEffect(snapshots) {
        if (snapshots.isNotEmpty()) {
            val values = snapshots.map { valueExtractor(it) }
            val padded = if (values.size < WINDOW_SLOTS) {
                // Pad the left side with the earliest known value so the chart
                // has a stable x-axis width and data "scrolls in" from the right.
                val fill = List(WINDOW_SLOTS - values.size) { values.first() }
                fill + values
            } else {
                values.takeLast(WINDOW_SLOTS)
            }
            modelProducer.runTransaction {
                lineSeries { series(padded) }
            }
        }
    }

    // X-axis formatter: show relative time labels (e.g. "-15m", "-10m", "-5m", "now")
    val bottomAxisFormatter = remember {
        CartesianValueFormatter { _, x, _ ->
            val slotIndex = x.toInt()
            val secondsAgo = (WINDOW_SLOTS - 1 - slotIndex) * POLL_INTERVAL_SEC
            when {
                slotIndex == WINDOW_SLOTS - 1 -> "now"
                secondsAgo > 0 && secondsAgo % 60 == 0 -> "-${secondsAgo / 60}m"
                else -> ""
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
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = bottomAxisFormatter,
                    ),
                ),
                modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            )

            Text(
                text = "${snapshots.size} sample${if (snapshots.size != 1) "s" else ""} · polling every ${POLL_INTERVAL_SEC}s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
