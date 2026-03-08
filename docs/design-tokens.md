# Kexplore Design Tokens

This document describes the design tokens that form the visual foundation of Kexplore's "technical precision" aesthetic.

## Overview

Kexplore uses design tokens to maintain visual consistency across the app. Tokens are defined in code as compile-time constants to ensure no runtime overhead.

## Design Principles

1. **Technical Precision**: Clean, angular, data-forward design
2. **Category Colors**: Distinct colors for Kubernetes resource categories
3. **Monospace for Technical Data**: UIDs, timestamps, and YAML use monospace fonts
4. **Density Options**: Default and compact modes for power users

---

## Color Tokens

### Category Colors

Each Kubernetes resource category has a distinct color:

| Category | Light Theme | Dark Theme | Hex (Light) | Hex (Dark) |
|----------|-------------|------------|-------------|------------|
| Workloads | Purple | Light Purple | #9C27B0 | #CE93D8 |
| Network | Cyan | Light Cyan | #00ACC1 | #80DEEA |
| Config | Amber | Light Amber | #FFA000 | #FFE082 |
| Storage | Blue | Light Blue | #1976D2 | #64B5F6 |
| Cluster | Teal | Light Teal | #00796B | #80CBC4 |

**Usage**: Apply to icons as tints, not backgrounds. This ensures WCAG AA contrast compliance.

### Status Colors

| Status | Color | Hex |
|--------|-------|-----|
| Running | Green | #4CAF50 |
| Pending | Yellow | #FFC107 |
| Failed | Red | #F44336 |
| Succeeded | Blue | #2196F3 |
| Terminating | Orange | #FF9800 |
| Unknown | Gray | #9E9E9E |

---

## Typography Tokens

### Font Families

- **Default**: System default (Roboto on Android)
- **Monospace**: System monospace for technical data

### Usage

| Style | Font | Size | Use Case |
|-------|------|------|----------|
| bodySmall | Monospace | 12sp | UIDs, timestamps |
| labelMedium | Monospace | 12sp | Labels, tags |
| titleMedium | Default | 16sp | List item titles |
| bodyMedium | Default | 14sp | Main content |

### Monospace Use Cases

The following should always use monospace:
- Resource UIDs
- Timestamps (created, age)
- YAML content
- Labels and annotations keys/values

---

## Spacing Tokens

| Token | Value | Usage |
|-------|-------|-------|
| spacingXs | 4dp | Tight spacing |
| spacingSm | 8dp | Default component padding |
| spacingMd | 12dp | Between related elements |
| spacingLg | 16dp | Section padding |
| spacingXl | 24dp | Large gaps |
| spacingXxl | 32dp | Major sections |

---

## Corner Radius Tokens

| Token | Value | Feel |
|-------|-------|------|
| radiusSm | 4dp | Subtle |
| radiusMd | 8dp | Technical (default) |
| radiusLg | 12dp | Soft |
| radiusFull | 50dp | Pills, FABs |

**Note**: We use 8dp (radiusMd) as the default for a more technical feel, rather than Material3's 12dp.

---

## Elevation Tokens

| Token | Value | Use Case |
|-------|-------|----------|
| elevationLevel0 | 0dp | Flat surfaces |
| elevationLevel1 | 1dp | Cards, list items |
| elevationLevel2 | 3dp | FABs |
| elevationLevel3 | 6dp | Dialogs |
| elevationLevel4 | 8dp | Navigation drawers |

---

## Density Modes

### Default Mode

- Standard Material3 spacing
- Comfortable touch targets (48dp minimum)
- Standard typography sizes

### Compact Mode

- 25% reduction in vertical spacing
- 12.5% reduction in horizontal spacing
- 12.5% reduction in icon sizes
- Slightly smaller typography

**Use Cases**: 
- Power users who want more content visible
- Large screens with limited space

---

## Accessibility

All color combinations meet WCAG 2.1 AA requirements:

- **Text**: Minimum 4.5:1 contrast ratio
- **Large Text**: Minimum 3:1 contrast ratio
- **UI Components**: Minimum 3:1 contrast ratio

Category colors are safe for icon tints but should not be used as text colors or large backgrounds without contrast testing.

---

## Files

- `DesignTokens.kt` - Core spacing, radius, elevation, icon sizes
- `CategoryColors.kt` - Resource category colors
- `KexploreTypography.kt` - Typography scale and text styles
- `KexploreColorScheme.kt` - Custom color scheme
- `Theme.kt` - Theme composition with density support

---

## Usage Examples

### Applying Category Colors to Icons

```kotlin
Icon(
    imageVector = resourceType.icon,
    tint = resourceType.category.getColor(isDarkTheme),
)
```

### Monospace for Technical Data

```kotlin
Text(
    text = resourceUid,
    style = KexploreTextStyles.uid,
)
```

### Compact Spacing

```kotlin
val densityMode = LocalDensityMode.current
val verticalPadding = when (densityMode) {
    DensityMode.Default -> 12.dp
    DensityMode.Compact -> 9.dp
}
```
