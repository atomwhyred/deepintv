package com.deepintv.launcher.data

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.deepintv.launcher.R

enum class BuiltinWallpaper(val id: String, @StringRes val labelRes: Int, @DrawableRes val imageRes: Int? = null) {
    MIDNIGHT("midnight", R.string.wallpaper_midnight),
    AURORA("aurora", R.string.wallpaper_aurora),
    DEEP_OCEAN("deep_ocean", R.string.wallpaper_ocean),
    SUNSET("sunset", R.string.wallpaper_sunset),
    // Deepin's own wallpapers (CC-BY-4.0, (c) UnionTech), scaled to 1080p.
    DEEPIN_THEME("deepin_theme", R.string.wallpaper_deepin_theme, R.drawable.wp_deepin_theme),
    DEEPIN_HAZY("deepin_hazy", R.string.wallpaper_deepin_hazy, R.drawable.wp_deepin_hazy),
    DEEPIN_MACARON("deepin_macaron", R.string.wallpaper_deepin_macaron, R.drawable.wp_deepin_macaron),
}

data class WallpaperSettings(
    val builtinId: String?,
    val customUri: String?,
    val dimPercent: Int,
)

/** The wallpaper the person picked — a built-in gradient or a custom image — plus how much to
 * darken it, so the Home UI stays legible on top of a bright picture. */
class WallpaperRepository(context: Context) {
    private val prefs = context.getSharedPreferences("deepintv_wallpaper", Context.MODE_PRIVATE)

    fun settings(): WallpaperSettings = WallpaperSettings(
        builtinId = prefs.getString(KEY_BUILTIN, BuiltinWallpaper.MIDNIGHT.id),
        customUri = prefs.getString(KEY_CUSTOM_URI, null),
        dimPercent = prefs.getInt(KEY_DIM, 0),
    )

    fun setBuiltin(id: String) {
        prefs.edit().putString(KEY_BUILTIN, id).remove(KEY_CUSTOM_URI).apply()
    }

    fun setCustomUri(uri: String) {
        prefs.edit().putString(KEY_CUSTOM_URI, uri).remove(KEY_BUILTIN).apply()
    }

    fun setDimPercent(percent: Int) {
        prefs.edit().putInt(KEY_DIM, percent.coerceIn(0, 100)).apply()
    }

    companion object {
        private const val KEY_BUILTIN = "builtin_id"
        private const val KEY_CUSTOM_URI = "custom_uri"
        private const val KEY_DIM = "dim_percent"
    }
}
