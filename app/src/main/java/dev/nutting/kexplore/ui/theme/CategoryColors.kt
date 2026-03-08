package dev.nutting.kexplore.ui.theme

import androidx.compose.ui.graphics.Color
import dev.nutting.kexplore.data.model.ResourceCategory

/**
 * Category colors for Kubernetes resource type visualization.
 * 
 * Each category has a distinct color that helps users quickly scan and 
 * identify resource types in lists. Colors are applied to icons (not backgrounds)
 * to ensure WCAG AA contrast compliance.
 * 
 * Color selection rationale:
 * - Workloads (Purple): Most common, primary category
 * - Network (Cyan): Technical, flow-focused
 * - Config (Amber): Important but distinct from data
 * - Storage (Blue): Persistent, physical
 * - Cluster (Teal): Infrastructure, foundational
 */
object CategoryColors {
    // ============================================
    // Category Colors - Light Theme
    // ============================================
    
    /** Workloads - Purple (#9C27B0) */
    val workloadsLight = Color(0xFF9C27B0)
    
    /** Network - Cyan (#00ACC1) */
    val networkLight = Color(0xFF00ACC1)
    
    /** Config - Amber (#FFA000) */
    val configLight = Color(0xFFFFA000)
    
    /** Storage - Blue (#1976D2) */
    val storageLight = Color(0xFF1976D2)
    
    /** Cluster - Teal (#00796B) */
    val clusterLight = Color(0xFF00796B)

    // ============================================
    // Category Colors - Dark Theme
    // ============================================
    
    /** Workloads - Light Purple (#CE93D8) */
    val workloadsDark = Color(0xFFCE93D8)
    
    /** Network - Light Cyan (#80DEEA) */
    val networkDark = Color(0xFF80DEEA)
    
    /** Config - Light Amber (#FFE082) */
    val configDark = Color(0xFFFFE082)
    
    /** Storage - Light Blue (#64B5F6) */
    val storageDark = Color(0xFF64B5F6)
    
    /** Cluster - Light Teal (#80CBC4) */
    val clusterDark = Color(0xFF80CBC4)

    // ============================================
    // Color Accessors
    // ============================================
    
    /**
     * Get the appropriate color for a resource category based on theme.
     * 
     * @param category The Kubernetes resource category
     * @param isDarkTheme Whether dark theme is active
     * @return The category color for the given theme
     */
    fun forCategory(category: ResourceCategory, isDarkTheme: Boolean): Color {
        return when (category) {
            ResourceCategory.Workloads -> if (isDarkTheme) workloadsDark else workloadsLight
            ResourceCategory.Network -> if (isDarkTheme) networkDark else networkLight
            ResourceCategory.Config -> if (isDarkTheme) configDark else configLight
            ResourceCategory.Storage -> if (isDarkTheme) storageDark else storageLight
            ResourceCategory.Cluster -> if (isDarkTheme) clusterDark else clusterLight
        }
    }

    /**
     * Get the color for a resource category (uses light theme as default).
     * Convenience function for cases where theme is not available.
     */
    fun forCategory(category: ResourceCategory): Color {
        return forCategory(category, isDarkTheme = false)
    }

    /**
     * All category colors for iteration/validation.
     */
    val allCategoriesLight = listOf(
        ResourceCategory.Workloads to workloadsLight,
        ResourceCategory.Network to networkLight,
        ResourceCategory.Config to configLight,
        ResourceCategory.Storage to storageLight,
        ResourceCategory.Cluster to clusterLight,
    )

    /**
     * All category colors for dark theme.
     */
    val allCategoriesDark = listOf(
        ResourceCategory.Workloads to workloadsDark,
        ResourceCategory.Network to networkDark,
        ResourceCategory.Config to configDark,
        ResourceCategory.Storage to storageDark,
        ResourceCategory.Cluster to clusterDark,
    )
}

/**
 * Extension property on ResourceCategory for convenient color access.
 */
val ResourceCategory.color: Color
    get() = CategoryColors.forCategory(this)

/**
 * Extension function on ResourceCategory to get color with theme awareness.
 */
fun ResourceCategory.getColor(isDarkTheme: Boolean): Color =
    CategoryColors.forCategory(this, isDarkTheme)
