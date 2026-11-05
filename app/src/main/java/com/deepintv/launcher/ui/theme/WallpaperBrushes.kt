package com.deepintv.launcher.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.deepintv.launcher.data.BuiltinWallpaper

/** Built-in wallpapers as gradients rather than bundled images: no asset files to ship, and every
 * one of them already fits the Deepin blue/cyan palette by construction. */
fun brushForWallpaper(builtinId: String?): Brush = when (builtinId) {
    BuiltinWallpaper.AURORA.id -> Brush.linearGradient(
        listOf(Color(0xFF0B0E14), Color(0xFF0F3B5C), Color(0xFF1890FF)),
    )
    BuiltinWallpaper.DEEP_OCEAN.id -> Brush.verticalGradient(
        listOf(Color(0xFF02060C), Color(0xFF0A2B40), Color(0xFF0E5478)),
    )
    BuiltinWallpaper.SUNSET.id -> Brush.verticalGradient(
        listOf(Color(0xFF190F2A), Color(0xFF5B2B4C), Color(0xFFBE6A3C)),
    )
    else -> if (DeepinThemeState.isDark) Brush.verticalGradient(listOf(DeepinBackground, Color(0xFF151B27)))
    else Brush.verticalGradient(listOf(DeepinBackground, Color(0xFFDDE3EE)))
}
