package dev.nutting.kexplore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nutting.kexplore.ui.theme.TerminalColors

@Composable
fun TerminalView(
    lines: List<String>,
    autoScroll: Boolean = true,
    showLineNumbers: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size, autoScroll) {
        if (autoScroll && lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalColors.background)
            .padding(8.dp),
    ) {
        itemsIndexed(lines) { index, line ->
            if (showLineNumbers) {
                Row {
                    Text(
                        text = "${index + 1}".padStart(5),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TerminalColors.lineNumber,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TerminalColors.text,
                    )
                }
            } else {
                Text(
                    text = line,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TerminalColors.text,
                )
            }
        }
    }
}
