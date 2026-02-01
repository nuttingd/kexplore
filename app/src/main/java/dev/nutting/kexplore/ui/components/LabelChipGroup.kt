package dev.nutting.kexplore.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabelChipGroup(
    labels: Map<String, String>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
    ) {
        labels.forEach { (key, value) ->
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text = "$key=$value",
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                modifier = Modifier.padding(end = 4.dp, bottom = 4.dp),
            )
        }
    }
}
