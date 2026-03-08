package dev.nutting.kexplore.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import dev.nutting.kexplore.ui.theme.KexploreTextStyles

@Preview
@Composable
internal fun KeyValueRowPreview() {
    MaterialTheme {
        KeyValueRow(label = "Name", value = "my-deployment-abc123")
    }
}

@Preview
@Composable
internal fun InlineKeyValueRowPreview() {
    MaterialTheme {
        InlineKeyValueRow(label = "Namespace", value = "default")
    }
}

/**
 * Displays a key-value pair with the label above the value in a vertical layout.
 */
@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Displays a key-value pair inline in a horizontal layout.
 */
@Composable
fun InlineKeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isMonospace: Boolean = false,
) {
    Row(modifier = modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (isMonospace) KexploreTextStyles.uid else MaterialTheme.typography.bodySmall,
        )
    }
}
