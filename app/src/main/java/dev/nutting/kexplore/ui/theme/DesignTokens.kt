package dev.nutting.kexplore.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Core design tokens for Kexplore's "technical precision" aesthetic.
 * 
 * These tokens establish the visual foundation for the brand:
 * - Slightly tighter spacing than default Material3
 * - 8dp corners for a technical feel
 * - Consistent elevation levels
 * 
 * Design tokens are compile-time constants, ensuring no runtime overhead.
 */
object DesignTokens {
    // ============================================
    // Spacing Tokens
    // ============================================
    
    /** Base spacing unit - 4dp */
    val spacingUnit: Dp = 4.dp
    
    /** XS spacing - 4dp */
    val spacingXs: Dp = spacingUnit
    
    /** SM spacing - 8dp */
    val spacingSm: Dp = spacingUnit * 2
    
    /** MD spacing - 12dp (slightly tighter than M3's 16dp) */
    val spacingMd: Dp = spacingUnit * 3
    
    /** LG spacing - 16dp */
    val spacingLg: Dp = spacingUnit * 4
    
    /** XL spacing - 24dp */
    val spacingXl: Dp = spacingUnit * 6
    
    /** XXL spacing - 32dp */
    val spacingXxl: Dp = spacingUnit * 8

    // ============================================
    // Corner Radius Tokens
    // ============================================
    
    /** Small radius - 4dp */
    val radiusSm: Dp = 4.dp
    
    /** Medium radius - 8dp (technical feel, not 12dp or 28dp) */
    val radiusMd: Dp = 8.dp
    
    /** Large radius - 12dp */
    val radiusLg: Dp = 12.dp
    
    /** Full radius (pills) */
    val radiusFull: Dp = 50.dp

    // ============================================
    // Elevation Tokens
    // ============================================
    
    /** Level 0 - no elevation (flat surfaces) */
    val elevationLevel0: Dp = 0.dp
    
    /** Level 1 - cards, list items */
    val elevationLevel1: Dp = 1.dp
    
    /** Level 2 - FABs, elevated cards */
    val elevationLevel2: Dp = 3.dp
    
    /** Level 3 - dialogs, modals */
    val elevationLevel3: Dp = 6.dp
    
    /** Level 4 - navigation drawers */
    val elevationLevel4: Dp = 8.dp

    // ============================================
    // Icon Size Tokens
    // ============================================
    
    /** Icon size - small */
    val iconSizeSm: Dp = 16.dp
    
    /** Icon size - medium (default) */
    val iconSizeMd: Dp = 24.dp
    
    /** Icon size - large */
    val iconSizeLg: Dp = 32.dp
    
    /** Icon size - extra large */
    val iconSizeXl: Dp = 48.dp

    // ============================================
    // Touch Target Tokens
    // ============================================
    
    /** Minimum touch target - 48dp (WCAG requirement) */
    val touchTargetMin: Dp = 48.dp
    
    /** Comfortable touch target - 56dp */
    val touchTargetComfortable: Dp = 56.dp

    // ============================================
    // Compact Mode Multipliers
    // ============================================
    
    /** Vertical padding multiplier for compact mode */
    const val compactVerticalMultiplier = 0.75f
    
    /** Horizontal padding multiplier for compact mode */
    const val compactHorizontalMultiplier = 0.875f
    
    /** Icon size multiplier for compact mode */
    const val compactIconMultiplier = 0.875f
}
