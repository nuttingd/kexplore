package dev.nutting.kexplore.ui.theme

import androidx.compose.ui.graphics.Color

object TerminalColors {
    val background = Color(0xFF1E1E1E)
    val text = Color(0xFFD4D4D4)
    val lineNumber = Color(0xFF858585)

    // Log syntax coloring
    val timestamp = Color(0xFF4EC9B0)       // Cyan
    val logLevelError = Color(0xFFF44747)   // Red
    val logLevelWarn = Color(0xFFCE9178)    // Orange/Yellow
    val logLevelInfo = Color(0xFF6A9955)    // Green
    val logLevelDebug = Color(0xFF808080)   // Gray

    // Search highlighting
    val searchHighlight = Color(0xFFFFE082)  // Yellow background
    val searchHighlightCurrent = Color(0xFFFF9800) // Orange for current match

    // Container colors for multiplex mode
    val containerColors = listOf(
        Color(0xFF569CD6),  // Blue
        Color(0xFF4EC9B0),  // Teal
        Color(0xFFCE9178),  // Peach
        Color(0xFFDCDCAA),  // Yellow
        Color(0xFFC586C0),  // Purple
        Color(0xFF9CDCFE),  // Light blue
        Color(0xFF4FC1FF),  // Bright blue
        Color(0xFFD7BA7D),  // Gold
    )
}
