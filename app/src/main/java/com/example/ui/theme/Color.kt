package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ColorPalettePreset(
    val title: String,
    val subtitle: String,
    val previewSwatches: List<Color>
) {
    EMERALD_INDIGO_AMBER(
        title = "Emerald Main • Indigo & Amber",
        subtitle = "Default Green • Indigo & Amber",
        previewSwatches = listOf(Color(0xFF0D9F6E), Color(0xFF4F46E5), Color(0xFFD97706))
    ),
    JADE_SAPPHIRE_GOLD(
        title = "Jade Main • Sapphire & Gold",
        subtitle = "Deep Jade • Sapphire & Gold",
        previewSwatches = listOf(Color(0xFF087F5B), Color(0xFF2563EB), Color(0xFFD97706))
    ),
    INDIGO_EMERALD_AMBER(
        title = "Indigo Main • Emerald & Amber",
        subtitle = "Royal Indigo • Emerald & Amber",
        previewSwatches = listOf(Color(0xFF4F46E5), Color(0xFF0D9F6E), Color(0xFFD97706))
    ),
    OCEAN_MINT_CORAL(
        title = "Sapphire Main • Mint & Coral",
        subtitle = "Deep Sapphire • Mint & Coral",
        previewSwatches = listOf(Color(0xFF0284C7), Color(0xFF10B981), Color(0xFFEA580C))
    ),
    TEAL_VIOLET_AMBER(
        title = "Teal Main • Violet & Amber",
        subtitle = "Jewel Teal • Violet & Amber",
        previewSwatches = listOf(Color(0xFF0D9488), Color(0xFF7C3AED), Color(0xFFD97706))
    ),
    PURPLE_MINT_ROSE(
        title = "Amethyst Main • Mint & Rose",
        subtitle = "Royal Amethyst • Mint & Rose",
        previewSwatches = listOf(Color(0xFF7C3AED), Color(0xFF0D9488), Color(0xFFDB2777))
    ),
    RUBY_TEAL_GOLD(
        title = "Ruby Main • Teal & Gold",
        subtitle = "Crimson Ruby • Teal & Gold",
        previewSwatches = listOf(Color(0xFFE11D48), Color(0xFF0D9488), Color(0xFFD97706))
    ),
    TERRACOTTA_FOREST_INDIGO(
        title = "Terracotta Main • Forest & Indigo",
        subtitle = "Warm Terracotta • Forest & Indigo",
        previewSwatches = listOf(Color(0xFFEA580C), Color(0xFF059669), Color(0xFF4F46E5))
    )
}

// Neutral Surface Tokens — Light Porcelain Mist & Dark Midnight Slate
val LightBackground = Color(0xFFF6F7FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFFCFDFF)
val LightSurfaceSubtle = Color(0xFFEEF1F8)
val LightTextPrimary = Color(0xFF121826)
val LightTextSecondary = Color(0xFF5E687A)
val LightCardBorder = Color(0xFFE1E6F0)

val DarkBackground = Color(0xFF0B101B)
val DarkSurface = Color(0xFF131B2E)
val DarkSurfaceElevated = Color(0xFF182238)
val DarkSurfaceSubtle = Color(0xFF1E2942)
val DarkTextPrimary = Color(0xFFF3F6FC)
val DarkTextSecondary = Color(0xFF97A4BA)
val DarkCardBorder = Color(0xFF263452)

// Semantic Transaction & Status Tokens (Smooth Red Expense & Smooth Green Income)
val LightExpenseSmoothRed = Color(0xFFE24A4A)
val LightExpenseSmoothContainer = Color(0xFFFEECEC)
val DarkExpenseSmoothRed = Color(0xFFF87171)
val DarkExpenseSmoothContainer = Color(0xFF341820)

val LightIncomeSmoothGreen = Color(0xFF0D9F6E)
val LightIncomeSmoothContainer = Color(0xFFE3F9F0)
val DarkIncomeSmoothGreen = Color(0xFF34D399)
val DarkIncomeSmoothContainer = Color(0xFF112F28)

val LightWarningAmber = Color(0xFFD97706)
val LightWarningContainer = Color(0xFFFEF3C7)
val DarkWarningAmber = Color(0xFFFBBF24)
val DarkWarningContainer = Color(0xFF3A290B)

// Harmonious Tri-Tone Luxury Fintech Chart Palette
val ChartPalette = listOf(
    Color(0xFF0D9F6E), // Luminous Emerald - Family Support
    Color(0xFF4F46E5), // Royal Indigo - Rent & Housing
    Color(0xFFD97706), // Warm Amber - Groceries
    Color(0xFF0284C7), // Sapphire Blue - Transport
    Color(0xFF7C3AED), // Royal Amethyst - Bills & Utilities
    Color(0xFFE24A4A), // Smooth Coral Red - EMI & Debt
    Color(0xFF10B981), // Mint Jade - Savings & Emergency
    Color(0xFFEC4899), // Berry Rose - Personal Spending
    Color(0xFF0D9488)  // Jewel Teal - Travel & Goals
)

@Immutable
data class SaathiExtendedColors(
    val cardBorder: Color,
    val surfaceElevated: Color,
    val surfaceSubtle: Color,
    val textSecondary: Color,
    val healthy: Color,
    val healthyContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val allowanceSurface: Color,
    val allowanceBorder: Color,
    val heroGradientStart: Color,
    val heroGradientMid: Color,
    val heroGradientEnd: Color,
    val heroGlowAccent: Color,
    val dockSurface: Color,
    val dockBorder: Color,
    // Tri-Color Mix Accents
    val accentPrimary: Color,
    val accentPrimaryContainer: Color,
    val accentSecondary: Color,
    val accentSecondaryContainer: Color,
    val accentTertiary: Color,
    val accentTertiaryContainer: Color,
    // Smooth Transaction Colors
    val expenseRed: Color,
    val expenseRedContainer: Color,
    val incomeGreen: Color,
    val incomeGreenContainer: Color
)

val LocalSaathiColors = staticCompositionLocalOf {
    SaathiExtendedColors(
        cardBorder = LightCardBorder,
        surfaceElevated = LightSurfaceElevated,
        surfaceSubtle = LightSurfaceSubtle,
        textSecondary = LightTextSecondary,
        healthy = LightIncomeSmoothGreen,
        healthyContainer = LightIncomeSmoothContainer,
        warning = LightWarningAmber,
        warningContainer = LightWarningContainer,
        danger = LightExpenseSmoothRed,
        dangerContainer = LightExpenseSmoothContainer,
        allowanceSurface = Color(0xFFF3FBF7),
        allowanceBorder = Color(0xFFC5ECD9),
        heroGradientStart = Color(0xFF065F46),
        heroGradientMid = Color(0xFF047857),
        heroGradientEnd = Color(0xFF0D9F6E),
        heroGlowAccent = Color(0xFFA7F3D0),
        dockSurface = Color(0xF7FFFFFF),
        dockBorder = Color(0xFFDCE2EE),
        accentPrimary = Color(0xFF0D9F6E),
        accentPrimaryContainer = Color(0xFFDDF7EA),
        accentSecondary = Color(0xFF4F46E5),
        accentSecondaryContainer = Color(0xFFE0E7FF),
        accentTertiary = Color(0xFFD97706),
        accentTertiaryContainer = Color(0xFFFEF3C7),
        expenseRed = LightExpenseSmoothRed,
        expenseRedContainer = LightExpenseSmoothContainer,
        incomeGreen = LightIncomeSmoothGreen,
        incomeGreenContainer = LightIncomeSmoothContainer
    )
}
