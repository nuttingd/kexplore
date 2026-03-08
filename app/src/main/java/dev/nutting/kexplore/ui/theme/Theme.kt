package dev.nutting.kexplore.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import dev.nutting.kexplore.data.preferences.DensityMode

/**
 * Local composition for density mode.
 * Access via [LocalDensityMode].
 */
val LocalDensityMode = compositionLocalOf { DensityMode.Default }

/**
 * Color scheme mode for KexploreTheme.
 */
enum class ColorSchemeMode {
    /** Use Material3 dynamic colors (Android 12+) or default */
    Dynamic,
    /** Use Kexplore custom color scheme */
    Kexplore,
}

/**
 * Kexplore theme for the app.
 * 
 * Supports three color modes:
 * - Dynamic colors (default, Android 12+): Uses device wallpaper colors
 * - Kexplore brand colors: Custom color scheme with brand identity
 * 
 * @param darkTheme Whether to use dark theme
 * @param dynamicColor Whether to use dynamic colors (ignored on Android < 12)
 * @param colorSchemeMode Whether to use dynamic or Kexplore custom colors
 * @param densityMode Density setting for UI (default or compact)
 * @param content Composable content
 */
@Composable
fun KexploreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    colorSchemeMode: ColorSchemeMode = ColorSchemeMode.Dynamic,
    densityMode: DensityMode = DensityMode.Default,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        // Dynamic colors (Android 12+)
        dynamicColor && colorSchemeMode == ColorSchemeMode.Dynamic 
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Kexplore custom colors
        colorSchemeMode == ColorSchemeMode.Kexplore -> {
            if (darkTheme) KexploreDarkColorScheme else KexploreLightColorScheme
        }
        // Fallback to default Material3
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val typography = when (densityMode) {
        DensityMode.Default -> KexploreTypography
        DensityMode.Compact -> KexploreTypographyCompact
    }

    CompositionLocalProvider(
        LocalDensityMode provides densityMode,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

/**
 * Legacy color scheme aliases for backward compatibility.
 */
private val LightColorScheme = lightColorScheme()
private val DarkColorScheme = darkColorScheme()
