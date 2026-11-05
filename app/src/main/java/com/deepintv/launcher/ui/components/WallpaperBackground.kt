package com.deepintv.launcher.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.deepintv.launcher.data.BuiltinWallpaper
import com.deepintv.launcher.data.WallpaperSettings
import com.deepintv.launcher.ui.theme.DeepinThemeState
import com.deepintv.launcher.ui.theme.brushForWallpaper

/** Draws the chosen wallpaper — a built-in gradient or a custom picked image — with a black scrim
 * on top sized by `dimPercent`, so Home stays legible over a bright picture. */
@Composable
fun WallpaperBackground(settings: WallpaperSettings, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        val customUri = settings.customUri
        val imageRes = BuiltinWallpaper.entries.firstOrNull { it.id == settings.builtinId }?.imageRes
        val context = LocalContext.current
        // Decode at screen size, not at the file's: a 4K photo would otherwise cost ~33 MB of heap.
        val screen = context.resources.displayMetrics
        val model: Any? = customUri?.let(Uri::parse) ?: imageRes
        if (model != null) {
            AsyncImage(
                model = remember(model, screen) {
                    ImageRequest.Builder(context)
                        .data(model)
                        .size(screen.widthPixels, screen.heightPixels)
                        .crossfade(false)
                        .build()
                },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(brushForWallpaper(settings.builtinId)))
        }
        if (!DeepinThemeState.isDark) {
            // Dark text on a dark picture would be unreadable: wash light mode out.
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.6f)))
        }
        if (settings.dimPercent > 0 && DeepinThemeState.isDark) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = settings.dimPercent / 100f * 0.8f)),
            )
        }
    }
}
