package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.example.ui.theme.ColorPalettePreset

object LauncherIconManager {

    private val allAliasClassNames = listOf(
        "com.example.LauncherEmerald",
        "com.example.LauncherJade",
        "com.example.LauncherIndigo",
        "com.example.LauncherOcean",
        "com.example.LauncherTeal",
        "com.example.LauncherPurple",
        "com.example.LauncherRuby",
        "com.example.LauncherTerracotta"
    )

    fun aliasClassNameFor(palette: ColorPalettePreset): String {
        return when (palette) {
            ColorPalettePreset.EMERALD_INDIGO_AMBER -> "com.example.LauncherEmerald"
            ColorPalettePreset.JADE_SAPPHIRE_GOLD -> "com.example.LauncherJade"
            ColorPalettePreset.INDIGO_EMERALD_AMBER -> "com.example.LauncherIndigo"
            ColorPalettePreset.OCEAN_MINT_CORAL -> "com.example.LauncherOcean"
            ColorPalettePreset.TEAL_VIOLET_AMBER -> "com.example.LauncherTeal"
            ColorPalettePreset.PURPLE_MINT_ROSE -> "com.example.LauncherPurple"
            ColorPalettePreset.RUBY_TEAL_GOLD -> "com.example.LauncherRuby"
            ColorPalettePreset.TERRACOTTA_FOREST_INDIGO -> "com.example.LauncherTerracotta"
        }
    }

    fun syncLauncherIcon(context: Context, palette: ColorPalettePreset) {
        runCatching {
            val pm = context.packageManager
            val pkg = context.packageName
            val targetClassName = aliasClassNameFor(palette)
            val targetComponent = ComponentName(pkg, targetClassName)

            val currentTargetState = pm.getComponentEnabledSetting(targetComponent)
            if (currentTargetState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                pm.setComponentEnabledSetting(
                    targetComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            for (aliasClass in allAliasClassNames) {
                if (aliasClass == targetClassName) continue
                val comp = ComponentName(pkg, aliasClass)
                val state = pm.getComponentEnabledSetting(comp)
                if (state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    pm.setComponentEnabledSetting(
                        comp,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
        }
    }
}
