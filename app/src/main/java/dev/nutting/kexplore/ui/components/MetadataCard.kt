package dev.nutting.kexplore.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import dev.nutting.kexplore.data.model.ResourceStatus

@Preview
@Composable
private fun MetadataCardPreview() {
    MaterialTheme {
        MetadataCard(
            name = "my-deployment",
            namespace = "default",
            uid = "abc123-def456-ghi789",
            creationTimestamp = "2024-01-15T10:30:00Z",
            status = ResourceStatus.Running,
        )
    }
}

@Composable
fun MetadataCard(
    name: String,
    namespace: String,
    uid: String,
    creationTimestamp: String,
    status: ResourceStatus,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(status = status)
            }
            Spacer(Modifier.height(8.dp))
            if (namespace.isNotEmpty()) {
                InlineKeyValueRow("Namespace", namespace)
            }
            InlineKeyValueRow("UID", uid)
            InlineKeyValueRow("Created", creationTimestamp)
        }
    }
}
