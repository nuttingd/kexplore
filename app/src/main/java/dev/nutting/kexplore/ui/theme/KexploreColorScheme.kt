package dev.nutting.kexplore.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Custom color scheme for Kexplore's "technical precision" brand identity.
 * 
 * This provides an alternative to Material3's dynamic colors, giving the app
 * a distinctive Kubernetes-focused look. The scheme is designed to:
 * - Complement category colors (not compete with them)
 * - Provide good contrast for accessibility
 * - Work well in both light and dark themes
 */

// ============================================
// Light Theme Colors
// ============================================

private val KexploreLightPrimary = Color(0xFF6750A4)          // Purple primary
private val KexploreLightOnPrimary = Color(0xFFFFFFFF)
private val KexploreLightPrimaryContainer = Color(0xFFEADDFF)
private val KexploreLightOnPrimaryContainer = Color(0xFF21005D)

private val KexploreLightSecondary = Color(0xFF625B71)
private val KexploreLightOnSecondary = Color(0xFFFFFFFF)
private val KexploreLightSecondaryContainer = Color(0xFFE8DEF8)
private val KexploreLightOnSecondaryContainer = Color(0xFF1D192B)

private val KexploreLightTertiary = Color(0xFF7D5260)
private val KexploreLightOnTertiary = Color(0xFFFFFFFF)
private val KexploreLightTertiaryContainer = Color(0xFFFFD8E4)
private val KexploreLightOnTertiaryContainer = Color(0xFF31111D)

private val KexploreLightError = Color(0xFFB3261E)
private val KexploreLightOnError = Color(0xFFFFFFFF)
private val KexploreLightErrorContainer = Color(0xFFF9DEDC)
private val KexploreLightOnErrorContainer = Color(0xFF410E0B)

private val KexploreLightBackground = Color(0xFFFFFBFE)
private val KexploreLightOnBackground = Color(0xFF1C1B1F)
private val KexploreLightSurface = Color(0xFFFFFBFE)
private val KexploreLightOnSurface = Color(0xFF1C1B1F)
private val KexploreLightSurfaceVariant = Color(0xFFE7E0EC)
private val KexploreLightOnSurfaceVariant = Color(0xFF49454F)

private val KexploreLightOutline = Color(0xFF79747E)
private val KexploreLightOutlineVariant = Color(0xFFCAC4D0)
private val KexploreLightInverseSurface = Color(0xFF313033)
private val KexploreLightInverseOnSurface = Color(0xFFF4EFF4)
private val KexploreLightInversePrimary = Color(0xFFD0BCFF)

// ============================================
// Dark Theme Colors
// ============================================

private val KexploreDarkPrimary = Color(0xFFD0BCFF)
private val KexploreDarkOnPrimary = Color(0xFF381E72)
private val KexploreDarkPrimaryContainer = Color(0xFF4F378B)
private val KexploreDarkOnPrimaryContainer = Color(0xFFEADDFF)

private val KexploreDarkSecondary = Color(0xFFCCC2DC)
private val KexploreDarkOnSecondary = Color(0xFF332D41)
private val KexploreDarkSecondaryContainer = Color(0xFF4A4458)
private val KexploreDarkOnSecondaryContainer = Color(0xFFE8DEF8)

private val KexploreDarkTertiary = Color(0xFFEFB8C8)
private val KexploreDarkOnTertiary = Color(0xFF492532)
private val KexploreDarkTertiaryContainer = Color(0xFF633B48)
private val KexploreDarkOnTertiaryContainer = Color(0xFFFFD8E4)

private val KexploreDarkError = Color(0xFFF2B8B5)
private val KexploreDarkOnError = Color(0xFF601410)
private val KexploreDarkErrorContainer = Color(0xFF8C1D18)
private val KexploreDarkOnErrorContainer = Color(0xFFF9DEDC)

private val KexploreDarkBackground = Color(0xFF1C1B1F)
private val KexploreDarkOnBackground = Color(0xFFE6E1E5)
private val KexploreDarkSurface = Color(0xFF1C1B1F)
private val KexploreDarkOnSurface = Color(0xFFE6E1E5)
private val KexploreDarkSurfaceVariant = Color(0xFF49454F)
private val KexploreDarkOnSurfaceVariant = Color(0xFFCAC4D0)

private val KexploreDarkOutline = Color(0xFF938F99)
private val KexploreDarkOutlineVariant = Color(0xFF49454F)
private val KexploreDarkInverseSurface = Color(0xFFE6E1E5)
private val KexploreDarkInverseOnSurface = Color(0xFF313033)
private val KexploreDarkInversePrimary = Color(0xFF6750A4)

// ============================================
// Color Schemes
// ============================================

/**
 * Kexplore custom light color scheme.
 * Provides a professional, technical feel distinct from generic Material3 apps.
 */
val KexploreLightColorScheme = lightColorScheme(
    primary = KexploreLightPrimary,
    onPrimary = KexploreLightOnPrimary,
    primaryContainer = KexploreLightPrimaryContainer,
    onPrimaryContainer = KexploreLightOnPrimaryContainer,
    secondary = KexploreLightSecondary,
    onSecondary = KexploreLightOnSecondary,
    secondaryContainer = KexploreLightSecondaryContainer,
    onSecondaryContainer = KexploreLightOnSecondaryContainer,
    tertiary = KexploreLightTertiary,
    onTertiary = KexploreLightOnTertiary,
    tertiaryContainer = KexploreLightTertiaryContainer,
    onTertiaryContainer = KexploreLightOnTertiaryContainer,
    error = KexploreLightError,
    onError = KexploreLightOnError,
    errorContainer = KexploreLightErrorContainer,
    onErrorContainer = KexploreLightOnErrorContainer,
    background = KexploreLightBackground,
    onBackground = KexploreLightOnBackground,
    surface = KexploreLightSurface,
    onSurface = KexploreLightOnSurface,
    surfaceVariant = KexploreLightSurfaceVariant,
    onSurfaceVariant = KexploreLightOnSurfaceVariant,
    outline = KexploreLightOutline,
    outlineVariant = KexploreLightOutlineVariant,
    inverseSurface = KexploreLightInverseSurface,
    inverseOnSurface = KexploreLightInverseOnSurface,
    inversePrimary = KexploreLightInversePrimary,
)

/**
 * Kexplore custom dark color scheme.
 * Provides a professional, technical feel in dark mode.
 */
val KexploreDarkColorScheme = darkColorScheme(
    primary = KexploreDarkPrimary,
    onPrimary = KexploreDarkOnPrimary,
    primaryContainer = KexploreDarkPrimaryContainer,
    onPrimaryContainer = KexploreDarkOnPrimaryContainer,
    secondary = KexploreDarkSecondary,
    onSecondary = KexploreDarkOnSecondary,
    secondaryContainer = KexploreDarkSecondaryContainer,
    onSecondaryContainer = KexploreDarkOnSecondaryContainer,
    tertiary = KexploreDarkTertiary,
    onTertiary = KexploreDarkOnTertiary,
    tertiaryContainer = KexploreDarkTertiaryContainer,
    onTertiaryContainer = KexploreDarkOnTertiaryContainer,
    error = KexploreDarkError,
    onError = KexploreDarkOnError,
    errorContainer = KexploreDarkErrorContainer,
    onErrorContainer = KexploreDarkOnErrorContainer,
    background = KexploreDarkBackground,
    onBackground = KexploreDarkOnBackground,
    surface = KexploreDarkSurface,
    onSurface = KexploreDarkOnSurface,
    surfaceVariant = KexploreDarkSurfaceVariant,
    onSurfaceVariant = KexploreDarkOnSurfaceVariant,
    outline = KexploreDarkOutline,
    outlineVariant = KexploreDarkOutlineVariant,
    inverseSurface = KexploreDarkInverseSurface,
    inverseOnSurface = KexploreDarkInverseOnSurface,
    inversePrimary = KexploreDarkInversePrimary,
)

/**
 * Extended colors for Kexplore-specific use cases.
 * These complement the Material3 color scheme with brand-specific colors.
 */
object KexploreColors {
    // Surface overlays for depth
    val surfaceTertiary = Color(0xFFF3EDF7)
    val surfaceTertiaryDark = Color(0xFF2B2930)
    
    // Accent colors for interactive elements
    val accentPurple = Color(0xFF6750A4)
    val accentCyan = Color(0xFF00ACC1)
    
    // Divider and separator colors
    val dividerLight = Color(0xFFE0E0E0)
    val dividerDark = Color(0xFF3C3C3C)
    
    // Disabled states
    val disabledLight = Color(0xFFBDBDBD)
    val disabledDark = Color(0xFF5C5C5C)
    
    // Scrim for overlays
    val scrim = Color(0xFF000000)
}
