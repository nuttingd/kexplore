package dev.nutting.kexplore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nutting.kexplore.ui.screen.logs.MultiplexLine
import dev.nutting.kexplore.ui.theme.TerminalColors

@Composable
fun MultiplexTerminalView(
    lines: List<MultiplexLine>,
    containers: List<String>,
    autoScroll: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size, autoScroll) {
        if (autoScroll && lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    val containerColorMap = remember(containers) {
        containers.mapIndexed { index, name ->
            name to TerminalColors.containerColors[index % TerminalColors.containerColors.size]
        }.toMap()
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalColors.background)
            .padding(8.dp),
    ) {
        itemsIndexed(lines) { _, line ->
            val color = containerColorMap[line.container] ?: TerminalColors.text
            val annotated = buildAnnotatedString {
                withStyle(SpanStyle(color = color)) {
                    append("[${line.container}] ")
                }
                withStyle(SpanStyle(color = TerminalColors.text)) {
                    append(line.text)
                }
            }
            Text(
                text = annotated,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
        }
    }
}
