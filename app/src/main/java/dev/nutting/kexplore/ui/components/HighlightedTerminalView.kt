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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nutting.kexplore.ui.theme.TerminalColors

private val TIMESTAMP_REGEX = Regex("""^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}:\d{2}[^\s]*""")
private val LOG_LEVEL_REGEX = Regex("""\b(ERROR|FATAL|PANIC)\b""", RegexOption.IGNORE_CASE)
private val LOG_WARN_REGEX = Regex("""\b(WARN|WARNING)\b""", RegexOption.IGNORE_CASE)
private val LOG_INFO_REGEX = Regex("""\bINFO\b""", RegexOption.IGNORE_CASE)
private val LOG_DEBUG_REGEX = Regex("""\bDEBUG\b""", RegexOption.IGNORE_CASE)

@Composable
fun HighlightedTerminalView(
    lines: List<String>,
    autoScroll: Boolean = true,
    showLineNumbers: Boolean = false,
    searchQuery: String = "",
    isRegex: Boolean = false,
    searchMatchIndices: List<Int> = emptyList(),
    currentMatchIndex: Int = -1,
    scrollToLine: Int = -1,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size, autoScroll) {
        if (autoScroll && lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    LaunchedEffect(scrollToLine) {
        if (scrollToLine >= 0 && scrollToLine < lines.size) {
            listState.animateScrollToItem(scrollToLine)
        }
    }

    val searchPattern = remember(searchQuery, isRegex) {
        if (searchQuery.isBlank()) null
        else try {
            if (isRegex) Regex(searchQuery, RegexOption.IGNORE_CASE)
            else Regex(Regex.escape(searchQuery), RegexOption.IGNORE_CASE)
        } catch (_: Exception) {
            null
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
            val isCurrentMatch = currentMatchIndex >= 0 &&
                currentMatchIndex < searchMatchIndices.size &&
                searchMatchIndices[currentMatchIndex] == index

            val annotated = remember(line, searchPattern, isCurrentMatch) {
                buildHighlightedLine(line, searchPattern, isCurrentMatch)
            }

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
                        text = annotated,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    )
                }
            } else {
                Text(
                    text = annotated,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

private fun buildHighlightedLine(
    line: String,
    searchPattern: Regex?,
    isCurrentMatch: Boolean,
): AnnotatedString = buildAnnotatedString {
    // Determine base color from log level
    val baseColor = when {
        LOG_LEVEL_REGEX.containsMatchIn(line) -> TerminalColors.logLevelError
        LOG_WARN_REGEX.containsMatchIn(line) -> TerminalColors.logLevelWarn
        LOG_INFO_REGEX.containsMatchIn(line) -> TerminalColors.logLevelInfo
        LOG_DEBUG_REGEX.containsMatchIn(line) -> TerminalColors.logLevelDebug
        else -> TerminalColors.text
    }

    // Check for timestamp prefix
    val tsMatch = TIMESTAMP_REGEX.find(line)

    if (searchPattern != null) {
        // Build with search highlighting
        val matches = searchPattern.findAll(line).toList()
        if (matches.isEmpty()) {
            // No search match in this line, just color it
            appendColoredLine(line, tsMatch, baseColor)
        } else {
            var lastEnd = 0
            for (match in matches) {
                // Text before match
                if (match.range.first > lastEnd) {
                    val segment = line.substring(lastEnd, match.range.first)
                    appendSegment(segment, lastEnd, tsMatch, baseColor)
                }
                // Highlighted match
                val highlightColor = if (isCurrentMatch) TerminalColors.searchHighlightCurrent
                else TerminalColors.searchHighlight
                withStyle(SpanStyle(
                    color = TerminalColors.background,
                    background = highlightColor,
                )) {
                    append(match.value)
                }
                lastEnd = match.range.last + 1
            }
            // Remainder
            if (lastEnd < line.length) {
                appendSegment(line.substring(lastEnd), lastEnd, tsMatch, baseColor)
            }
        }
    } else {
        appendColoredLine(line, tsMatch, baseColor)
    }
}

private fun AnnotatedString.Builder.appendColoredLine(
    line: String,
    tsMatch: MatchResult?,
    baseColor: androidx.compose.ui.graphics.Color,
) {
    if (tsMatch != null) {
        withStyle(SpanStyle(color = TerminalColors.timestamp)) {
            append(tsMatch.value)
        }
        withStyle(SpanStyle(color = baseColor)) {
            append(line.substring(tsMatch.range.last + 1))
        }
    } else {
        withStyle(SpanStyle(color = baseColor)) {
            append(line)
        }
    }
}

private fun AnnotatedString.Builder.appendSegment(
    segment: String,
    startIndex: Int,
    tsMatch: MatchResult?,
    baseColor: androidx.compose.ui.graphics.Color,
) {
    val endIndex = startIndex + segment.length
    if (tsMatch != null && startIndex <= tsMatch.range.last && endIndex > tsMatch.range.first) {
        // Overlaps with timestamp
        withStyle(SpanStyle(color = TerminalColors.timestamp)) {
            append(segment)
        }
    } else if (tsMatch != null && startIndex < tsMatch.range.last + 1) {
        withStyle(SpanStyle(color = TerminalColors.timestamp)) {
            append(segment)
        }
    } else {
        withStyle(SpanStyle(color = baseColor)) {
            append(segment)
        }
    }
}
