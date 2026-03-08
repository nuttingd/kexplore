package dev.nutting.kexplore.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
internal fun SectionHeaderPreview() {
    MaterialTheme {
        SectionHeader(title = "Container Details")
    }
}

@Preview
@Composable
internal fun SectionHeaderNoDividerPreview() {
    MaterialTheme {
        SectionHeader(title = "No Divider", showDivider = false)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier) {
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}
