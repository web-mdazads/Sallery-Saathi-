package com.example.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private data class TriPaletteTokens(
    val lightPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val lightSecondary: Color,
    val lightSecondaryContainer: Color,
    val lightOnSecondaryContainer: Color,
    val lightTertiary: Color,
    val lightTertiaryContainer: Color,
    val lightAllowanceSurface: Color,
    val lightAllowanceBorder: Color,
    val lightHeroStart: Color,
    val lightHeroMid: Color,
    val lightHeroEnd: Color,
    val lightHeroGlow: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
    val darkSecondary: Color,
    val darkSecondaryContainer: Color,
    val darkTertiary: Color,
    val darkTertiaryContainer: Color,
    val darkAllowanceSurface: Color,
    val darkAllowanceBorder: Color,
    val darkHeroStart: Color,
    val darkHeroMid: Color,
    val darkHeroEnd: Color,
    val darkHeroGlow: Color
)

private fun paletteTokensFor(preset: ColorPalettePreset): TriPaletteTokens {
    return when (preset) {
        ColorPalettePreset.EMERALD_INDIGO_AMBER -> TriPaletteTokens(
            lightPrimary = Color(0xFF0D9F6E),
            lightPrimaryContainer = Color(0xFFDDF7EA),
            lightOnPrimaryContainer = Color(0xFF053F2F),
            lightSecondary = Color(0xFF4F46E5),
            lightSecondaryContainer = Color(0xFFE0E7FF),
            lightOnSecondaryContainer = Color(0xFF1E1B4B),
            lightTertiary = Color(0xFFD97706),
            lightTertiaryContainer = Color(0xFFFEF3C7),
            lightAllowanceSurface = Color(0xFFF3FBF7),
            lightAllowanceBorder = Color(0xFFC5ECD9),
            lightHeroStart = Color(0xFF064E3B),
            lightHeroMid = Color(0xFF047857),
            lightHeroEnd = Color(0xFF0D9F6E),
            lightHeroGlow = Color(0xFFA7F3D0),
            darkPrimary = Color(0xFF34D399),
            darkOnPrimary = Color(0xFF042B1E),
            darkPrimaryContainer = Color(0xFF12382B),
            darkOnPrimaryContainer = Color(0xFFD1FAE5),
            darkSecondary = Color(0xFF818CF8),
            darkSecondaryContainer = Color(0xFF232856),
            darkTertiary = Color(0xFFFBBF24),
            darkTertiaryContainer = Color(0xFF3A290B),
            darkAllowanceSurface = Color(0xFF122722),
            darkAllowanceBorder = Color(0xFF234B40),
            darkHeroStart = Color(0xFF042F24),
            darkHeroMid = Color(0xFF064E3B),
            darkHeroEnd = Color(0xFF059669),
            darkHeroGlow = Color(0xFF6EE7B7)
        )

        ColorPalettePreset.JADE_SAPPHIRE_GOLD -> TriPaletteTokens(
            lightPrimary = Color(0xFF087F5B),
            lightPrimaryContainer = Color(0xFFDDF7EA),
            lightOnPrimaryContainer = Color(0xFF053F2F),
            lightSecondary = Color(0xFF2563EB),
            lightSecondaryContainer = Color(0xFFDBEAFE),
            lightOnSecondaryContainer = Color(0xFF1E3A8A),
            lightTertiary = Color(0xFFD97706),
            lightTertiaryContainer = Color(0xFFFEF3C7),
            lightAllowanceSurface = Color(0xFFF4FBF8),
            lightAllowanceBorder = Color(0xFFC8EBD9),
            lightHeroStart = Color(0xFF064E3B),
            lightHeroMid = Color(0xFF047857),
            lightHeroEnd = Color(0xFF087F5B),
            lightHeroGlow = Color(0xFFA7F3D0),
            darkPrimary = Color(0xFF34D399),
            darkOnPrimary = Color(0xFF042B1E),
            darkPrimaryContainer = Color(0xFF13382B),
            darkOnPrimaryContainer = Color(0xFFD1FAE5),
            darkSecondary = Color(0xFF60A5FA),
            darkSecondaryContainer = Color(0xFF172C52),
            darkTertiary = Color(0xFFFBBF24),
            darkTertiaryContainer = Color(0xFF3A290B),
            darkAllowanceSurface = Color(0xFF132622),
            darkAllowanceBorder = Color(0xFF234B41),
            darkHeroStart = Color(0xFF042F24),
            darkHeroMid = Color(0xFF064E3B),
            darkHeroEnd = Color(0xFF065F46),
            darkHeroGlow = Color(0xFF6EE7B7)
        )

        ColorPalettePreset.INDIGO_EMERALD_AMBER -> TriPaletteTokens(
            lightPrimary = Color(0xFF4F46E5),
            lightPrimaryContainer = Color(0xFFE0E7FF),
            lightOnPrimaryContainer = Color(0xFF1E1B4B),
            lightSecondary = Color(0xFF0D9F6E),
            lightSecondaryContainer = Color(0xFFDDF7EA),
            lightOnSecondaryContainer = Color(0xFF053F2F),
            lightTertiary = Color(0xFFD97706),
            lightTertiaryContainer = Color(0xFFFEF3C7),
            lightAllowanceSurface = Color(0xFFF8FAFF),
            lightAllowanceBorder = Color(0xFFD9E0FC),
            lightHeroStart = Color(0xFF312E81),
            lightHeroMid = Color(0xFF4338CA),
            lightHeroEnd = Color(0xFF4F46E5),
            lightHeroGlow = Color(0xFFC7D2FE),
            darkPrimary = Color(0xFF818CF8),
            darkOnPrimary = Color(0xFF14163C),
            darkPrimaryContainer = Color(0xFF232856),
            darkOnPrimaryContainer = Color(0xFFE0E7FF),
            darkSecondary = Color(0xFF34D399),
            darkSecondaryContainer = Color(0xFF12362B),
            darkTertiary = Color(0xFFFBBF24),
            darkTertiaryContainer = Color(0xFF3A290B),
            darkAllowanceSurface = Color(0xFF162036),
            darkAllowanceBorder = Color(0xFF2B3A63),
            darkHeroStart = Color(0xFF1E1B4B),
            darkHeroMid = Color(0xFF312E81),
            darkHeroEnd = Color(0xFF3730A3),
            darkHeroGlow = Color(0xFFA5B4FC)
        )

        ColorPalettePreset.OCEAN_MINT_CORAL -> TriPaletteTokens(
            lightPrimary = Color(0xFF0284C7),
            lightPrimaryContainer = Color(0xFFE0F2FE),
            lightOnPrimaryContainer = Color(0xFF082F49),
            lightSecondary = Color(0xFF10B981),
            lightSecondaryContainer = Color(0xFFD1FAE5),
            lightOnSecondaryContainer = Color(0xFF064E3B),
            lightTertiary = Color(0xFFEA580C),
            lightTertiaryContainer = Color(0xFFFFEDD5),
            lightAllowanceSurface = Color(0xFFF5FAFF),
            lightAllowanceBorder = Color(0xFFCCE8FA),
            lightHeroStart = Color(0xFF0C4A6E),
            lightHeroMid = Color(0xFF0369A1),
            lightHeroEnd = Color(0xFF0284C7),
            lightHeroGlow = Color(0xFFBAE6FD),
            darkPrimary = Color(0xFF38BDF8),
            darkOnPrimary = Color(0xFF06283D),
            darkPrimaryContainer = Color(0xFF12344D),
            darkOnPrimaryContainer = Color(0xFFE0F2FE),
            darkSecondary = Color(0xFF34D399),
            darkSecondaryContainer = Color(0xFF11352B),
            darkTertiary = Color(0xFFFB923C),
            darkTertiaryContainer = Color(0xFF3D2112),
            darkAllowanceSurface = Color(0xFF142336),
            darkAllowanceBorder = Color(0xFF234363),
            darkHeroStart = Color(0xFF082F49),
            darkHeroMid = Color(0xFF0C4A6E),
            darkHeroEnd = Color(0xFF075985),
            darkHeroGlow = Color(0xFF7DD3FC)
        )

        ColorPalettePreset.TEAL_VIOLET_AMBER -> TriPaletteTokens(
            lightPrimary = Color(0xFF0D9488),
            lightPrimaryContainer = Color(0xFFCCFBF1),
            lightOnPrimaryContainer = Color(0xFF134E4A),
            lightSecondary = Color(0xFF7C3AED),
            lightSecondaryContainer = Color(0xFFEDE9FE),
            lightOnSecondaryContainer = Color(0xFF2E1065),
            lightTertiary = Color(0xFFD97706),
            lightTertiaryContainer = Color(0xFFFEF3C7),
            lightAllowanceSurface = Color(0xFFF2FBF9),
            lightAllowanceBorder = Color(0xFFBFECE4),
            lightHeroStart = Color(0xFF115E59),
            lightHeroMid = Color(0xFF0F766E),
            lightHeroEnd = Color(0xFF0D9488),
            lightHeroGlow = Color(0xFF99F6E4),
            darkPrimary = Color(0xFF2DD4BF),
            darkOnPrimary = Color(0xFF042F2E),
            darkPrimaryContainer = Color(0xFF113937),
            darkOnPrimaryContainer = Color(0xFFCCFBF1),
            darkSecondary = Color(0xFFA78BFA),
            darkSecondaryContainer = Color(0xFF2E1F57),
            darkTertiary = Color(0xFFFBBF24),
            darkTertiaryContainer = Color(0xFF3A290B),
            darkAllowanceSurface = Color(0xFF122729),
            darkAllowanceBorder = Color(0xFF224C4F),
            darkHeroStart = Color(0xFF042F2E),
            darkHeroMid = Color(0xFF115E59),
            darkHeroEnd = Color(0xFF0F766E),
            darkHeroGlow = Color(0xFF5EEAD4)
        )

        ColorPalettePreset.PURPLE_MINT_ROSE -> TriPaletteTokens(
            lightPrimary = Color(0xFF7C3AED),
            lightPrimaryContainer = Color(0xFFEDE9FE),
            lightOnPrimaryContainer = Color(0xFF2E1065),
            lightSecondary = Color(0xFF0D9488),
            lightSecondaryContainer = Color(0xFFCCFBF1),
            lightOnSecondaryContainer = Color(0xFF134E4A),
            lightTertiary = Color(0xFFDB2777),
            lightTertiaryContainer = Color(0xFFFCE7F3),
            lightAllowanceSurface = Color(0xFFFAF7FF),
            lightAllowanceBorder = Color(0xFFE4D9FC),
            lightHeroStart = Color(0xFF4C1D95),
            lightHeroMid = Color(0xFF6D28D9),
            lightHeroEnd = Color(0xFF7C3AED),
            lightHeroGlow = Color(0xFFDDD6FE),
            darkPrimary = Color(0xFFA78BFA),
            darkOnPrimary = Color(0xFF231148),
            darkPrimaryContainer = Color(0xFF2E1F57),
            darkOnPrimaryContainer = Color(0xFFEDE9FE),
            darkSecondary = Color(0xFF2DD4BF),
            darkSecondaryContainer = Color(0xFF113734),
            darkTertiary = Color(0xFFF472B6),
            darkTertiaryContainer = Color(0xFF3F172B),
            darkAllowanceSurface = Color(0xFF1D1833),
            darkAllowanceBorder = Color(0xFF3A2D63),
            darkHeroStart = Color(0xFF2E1065),
            darkHeroMid = Color(0xFF4C1D95),
            darkHeroEnd = Color(0xFF5B21B6),
            darkHeroGlow = Color(0xFFC4B5FD)
        )

        ColorPalettePreset.RUBY_TEAL_GOLD -> TriPaletteTokens(
            lightPrimary = Color(0xFFE11D48),
            lightPrimaryContainer = Color(0xFFFFE4E6),
            lightOnPrimaryContainer = Color(0xFF4C0519),
            lightSecondary = Color(0xFF0D9488),
            lightSecondaryContainer = Color(0xFFCCFBF1),
            lightOnSecondaryContainer = Color(0xFF134E4A),
            lightTertiary = Color(0xFFD97706),
            lightTertiaryContainer = Color(0xFFFEF3C7),
            lightAllowanceSurface = Color(0xFFFFF5F7),
            lightAllowanceBorder = Color(0xFFFECDD3),
            lightHeroStart = Color(0xFF881337),
            lightHeroMid = Color(0xFFBE123C),
            lightHeroEnd = Color(0xFFE11D48),
            lightHeroGlow = Color(0xFFFECDD3),
            darkPrimary = Color(0xFFFB7185),
            darkOnPrimary = Color(0xFF4C0519),
            darkPrimaryContainer = Color(0xFF4A1525),
            darkOnPrimaryContainer = Color(0xFFFFE4E6),
            darkSecondary = Color(0xFF2DD4BF),
            darkSecondaryContainer = Color(0xFF113734),
            darkTertiary = Color(0xFFFBBF24),
            darkTertiaryContainer = Color(0xFF3A290B),
            darkAllowanceSurface = Color(0xFF291620),
            darkAllowanceBorder = Color(0xFF542538),
            darkHeroStart = Color(0xFF4C0519),
            darkHeroMid = Color(0xFF881337),
            darkHeroEnd = Color(0xFF9F1239),
            darkHeroGlow = Color(0xFFFDA4AF)
        )

        ColorPalettePreset.TERRACOTTA_FOREST_INDIGO -> TriPaletteTokens(
            lightPrimary = Color(0xFFEA580C),
            lightPrimaryContainer = Color(0xFFFFEDD5),
            lightOnPrimaryContainer = Color(0xFF431407),
            lightSecondary = Color(0xFF059669),
            lightSecondaryContainer = Color(0xFFD1FAE5),
            lightOnSecondaryContainer = Color(0xFF064E3B),
            lightTertiary = Color(0xFF4F46E5),
            lightTertiaryContainer = Color(0xFFE0E7FF),
            lightAllowanceSurface = Color(0xFFFFF8F3),
            lightAllowanceBorder = Color(0xFFFED7AA),
            lightHeroStart = Color(0xFF7C2D12),
            lightHeroMid = Color(0xFFC2410C),
            lightHeroEnd = Color(0xFFEA580C),
            lightHeroGlow = Color(0xFFFED7AA),
            darkPrimary = Color(0xFFFB923C),
            darkOnPrimary = Color(0xFF431407),
            darkPrimaryContainer = Color(0xFF462111),
            darkOnPrimaryContainer = Color(0xFFFFEDD5),
            darkSecondary = Color(0xFF34D399),
            darkSecondaryContainer = Color(0xFF12362B),
            darkTertiary = Color(0xFF818CF8),
            darkTertiaryContainer = Color(0xFF232856),
            darkAllowanceSurface = Color(0xFF281B14),
            darkAllowanceBorder = Color(0xFF523320),
            darkHeroStart = Color(0xFF431407),
            darkHeroMid = Color(0xFF7C2D12),
            darkHeroEnd = Color(0xFF9A3412),
            darkHeroGlow = Color(0xFFFDBA74)
        )
    }
}

val SaathiShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SalarySaathiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorPalette: ColorPalettePreset = ColorPalettePreset.EMERALD_INDIGO_AMBER,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val p = paletteTokensFor(colorPalette)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = p.darkPrimary,
            onPrimary = p.darkOnPrimary,
            primaryContainer = p.darkPrimaryContainer,
            onPrimaryContainer = p.darkOnPrimaryContainer,
            secondary = p.darkSecondary,
            onSecondary = Color(0xFF042B1E),
            secondaryContainer = p.darkSecondaryContainer,
            onSecondaryContainer = Color(0xFFD1FAE5),
            tertiary = p.darkTertiary,
            tertiaryContainer = p.darkTertiaryContainer,
            background = DarkBackground,
            onBackground = DarkTextPrimary,
            surface = DarkSurface,
            onSurface = DarkTextPrimary,
            surfaceVariant = DarkSurfaceSubtle,
            onSurfaceVariant = DarkTextSecondary,
            outline = DarkCardBorder,
            error = DarkExpenseSmoothRed,
            errorContainer = DarkExpenseSmoothContainer,
            onError = Color(0xFF3D0A0A),
            onErrorContainer = DarkExpenseSmoothRed
        )
    } else {
        lightColorScheme(
            primary = p.lightPrimary,
            onPrimary = Color.White,
            primaryContainer = p.lightPrimaryContainer,
            onPrimaryContainer = p.lightOnPrimaryContainer,
            secondary = p.lightSecondary,
            onSecondary = Color.White,
            secondaryContainer = p.lightSecondaryContainer,
            onSecondaryContainer = p.lightOnSecondaryContainer,
            tertiary = p.lightTertiary,
            tertiaryContainer = p.lightTertiaryContainer,
            background = LightBackground,
            onBackground = LightTextPrimary,
            surface = LightSurface,
            onSurface = LightTextPrimary,
            surfaceVariant = LightSurfaceSubtle,
            onSurfaceVariant = LightTextSecondary,
            outline = LightCardBorder,
            error = LightExpenseSmoothRed,
            errorContainer = LightExpenseSmoothContainer,
            onError = Color.White,
            onErrorContainer = LightExpenseSmoothRed
        )
    }

    val extendedColors = if (isDark) {
        SaathiExtendedColors(
            cardBorder = DarkCardBorder,
            surfaceElevated = DarkSurfaceElevated,
            surfaceSubtle = DarkSurfaceSubtle,
            textSecondary = DarkTextSecondary,
            healthy = DarkIncomeSmoothGreen,
            healthyContainer = DarkIncomeSmoothContainer,
            warning = DarkWarningAmber,
            warningContainer = DarkWarningContainer,
            danger = DarkExpenseSmoothRed,
            dangerContainer = DarkExpenseSmoothContainer,
            allowanceSurface = p.darkAllowanceSurface,
            allowanceBorder = p.darkAllowanceBorder,
            heroGradientStart = p.darkHeroStart,
            heroGradientMid = p.darkHeroMid,
            heroGradientEnd = p.darkHeroEnd,
            heroGlowAccent = p.darkHeroGlow,
            dockSurface = Color(0xF2151E32),
            dockBorder = Color(0xFF283757),
            accentPrimary = p.darkPrimary,
            accentPrimaryContainer = p.darkPrimaryContainer,
            accentSecondary = p.darkSecondary,
            accentSecondaryContainer = p.darkSecondaryContainer,
            accentTertiary = p.darkTertiary,
            accentTertiaryContainer = p.darkTertiaryContainer,
            expenseRed = DarkExpenseSmoothRed,
            expenseRedContainer = DarkExpenseSmoothContainer,
            incomeGreen = DarkIncomeSmoothGreen,
            incomeGreenContainer = DarkIncomeSmoothContainer
        )
    } else {
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
            allowanceSurface = p.lightAllowanceSurface,
            allowanceBorder = p.lightAllowanceBorder,
            heroGradientStart = p.lightHeroStart,
            heroGradientMid = p.lightHeroMid,
            heroGradientEnd = p.lightHeroEnd,
            heroGlowAccent = p.lightHeroGlow,
            dockSurface = Color(0xF7FFFFFF),
            dockBorder = Color(0xFFDCE2EE),
            accentPrimary = p.lightPrimary,
            accentPrimaryContainer = p.lightPrimaryContainer,
            accentSecondary = p.lightSecondary,
            accentSecondaryContainer = p.lightSecondaryContainer,
            accentTertiary = p.lightTertiary,
            accentTertiaryContainer = p.lightTertiaryContainer,
            expenseRed = LightExpenseSmoothRed,
            expenseRedContainer = LightExpenseSmoothContainer,
            incomeGreen = LightIncomeSmoothGreen,
            incomeGreenContainer = LightIncomeSmoothContainer
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var ctx = view.context
            while (ctx is ContextWrapper && ctx !is Activity) {
                ctx = ctx.baseContext
            }
            (ctx as? Activity)?.window?.let { window ->
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalSaathiColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = SaathiShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    SalarySaathiTheme(
        themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
        content = content
    )
}
