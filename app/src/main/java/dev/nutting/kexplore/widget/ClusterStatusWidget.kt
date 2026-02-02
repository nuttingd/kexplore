package dev.nutting.kexplore.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClusterStatusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadState(context)

        provideContent {
            GlanceTheme {
                WidgetContent(state)
            }
        }
    }

    private fun loadState(context: Context): WidgetState {
        val prefs = context.getSharedPreferences(WidgetRefreshWorker.WIDGET_PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(WidgetRefreshWorker.KEY_WIDGET_STATE, null) ?: return WidgetState()
        return try {
            Json.decodeFromString(json)
        } catch (_: Exception) {
            WidgetState()
        }
    }
}

@Composable
private fun WidgetContent(state: WidgetState) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp),
    ) {
        Text(
            text = state.clusterName.ifEmpty { "No cluster" },
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = GlanceTheme.colors.onSurface,
            ),
            maxLines = 1,
        )

        Spacer(GlanceModifier.height(8.dp))

        if (state.error != null) {
            Text(
                text = state.error,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.error,
                ),
                maxLines = 2,
            )
        } else if (state.lastUpdated == 0L) {
            Text(
                text = "Waiting for data...",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
            )
        } else {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StatBlock(
                    label = "Pods",
                    value = "${state.runningPods}/${state.totalPods}",
                    modifier = GlanceModifier.defaultWeight(),
                )
                Spacer(GlanceModifier.width(8.dp))
                StatBlock(
                    label = "Failed",
                    value = "${state.failedPods}",
                    modifier = GlanceModifier.defaultWeight(),
                )
                Spacer(GlanceModifier.width(8.dp))
                StatBlock(
                    label = "Nodes",
                    value = "${state.readyNodes}/${state.totalNodes}",
                    modifier = GlanceModifier.defaultWeight(),
                )
            }

            Spacer(GlanceModifier.height(8.dp))

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            Text(
                text = "Updated ${timeFormat.format(Date(state.lastUpdated))}",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun StatBlock(
    label: String,
    value: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GlanceTheme.colors.onSurface,
            ),
        )
        Text(
            text = label,
            style = TextStyle(
                fontSize = 10.sp,
                color = GlanceTheme.colors.onSurfaceVariant,
            ),
        )
    }
}
