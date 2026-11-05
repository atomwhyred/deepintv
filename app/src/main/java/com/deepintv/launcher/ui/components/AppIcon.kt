package com.deepintv.launcher.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.withClip
import androidx.compose.ui.platform.LocalDensity
import android.util.LruCache

private const val MAX_ICON_PX = 256
/** The largest tile an icon is drawn in (the dock at its 140% maximum); bigger bitmaps are wasted RAM. */
private const val TILE_DP = 168
private const val CORNER_FRACTION = 0.22f
private const val BASE_COLOR = 0xFF1B2030.toInt()
private const val CACHE_BYTES = 4 * 1024 * 1024

private val cache = object : LruCache<String, ImageBitmap>(CACHE_BYTES) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
}

/** Drops cached icons under memory pressure; they are rebuilt on demand. */
fun trimAppIconCache(level: Int) {
    if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) cache.evictAll()
}

/** Every app icon as the same rounded square, so adaptive, square and transparent-cornered
 * icons stop looking like different families. Cached by package: the grid recomposes a lot. */
@Composable
fun rememberAppIcon(packageName: String, icon: Drawable): ImageBitmap {
    val iconPx = with(LocalDensity.current) { TILE_DP.dp.roundToPx() }.coerceIn(1, MAX_ICON_PX)
    return remember(packageName, iconPx) {
        val key = "$packageName@$iconPx"
        cache.get(key) ?: icon.toTileBitmap(iconPx).asImageBitmap().also { cache.put(key, it) }
    }
}

private fun Drawable.toTileBitmap(size: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = size * CORNER_FRACTION
    val clip = Path().apply { addRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), radius, radius, Path.Direction.CW) }
    val basePaint = Paint().apply { color = BASE_COLOR }
    canvas.withClip(clip) {
        drawPaint(basePaint)
        if (this@toTileBitmap is AdaptiveIconDrawable) {
            // Layers are 108dp with only the centre 72dp visible: draw them at 1.5x so the visible
            // part fills the tile, instead of letting the system's own mask pick the corners.
            val overscan = (size * 0.25f).toInt()
            background?.let { it.setBounds(-overscan, -overscan, size + overscan, size + overscan); it.draw(this) }
            foreground?.let { it.setBounds(-overscan, -overscan, size + overscan, size + overscan); it.draw(this) }
        } else {
            val inset = (size * 0.14f).toInt()
            setBounds(inset, inset, size - inset, size - inset)
            draw(this)
        }
    }
    return bitmap
}
