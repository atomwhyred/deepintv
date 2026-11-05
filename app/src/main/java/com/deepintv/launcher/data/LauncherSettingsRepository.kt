package com.deepintv.launcher.data

import android.content.Context

enum class ThemeMode { DARK, LIGHT, AUTO }

/** The launcher's own display settings (control center's "Launcher Setup"): dock glass
 * transparency and icon size, both persisted and applied live. */
class LauncherSettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("deepintv_launcher_settings", Context.MODE_PRIVATE)

    fun dockTransparencyPercent(): Int = prefs.getInt(KEY_TRANSPARENCY, DEFAULT_TRANSPARENCY)
    fun setDockTransparencyPercent(value: Int) {
        prefs.edit().putInt(KEY_TRANSPARENCY, value.coerceIn(0, 100)).apply()
    }

    fun dockSizePercent(): Int = prefs.getInt(KEY_SIZE, DEFAULT_SIZE)
    fun setDockSizePercent(value: Int) {
        prefs.edit().putInt(KEY_SIZE, value.coerceIn(MIN_SIZE, MAX_SIZE)).apply()
    }

    /** Whether the Home button brings DeepinTV back when the TV's own launcher opens instead. */
    fun homeOverride(): Boolean = prefs.getBoolean(KEY_HOME_OVERRIDE, true)
    fun setHomeOverride(value: Boolean) {
        prefs.edit().putBoolean(KEY_HOME_OVERRIDE, value).apply()
    }

    /** Whether the screen turning off sends the TV back to Home, so a streaming app left in front
     * can't wake the panel later on its own. Off by default: it also drops the show you were in. */
    fun homeOnScreenOff(): Boolean = prefs.getBoolean(KEY_HOME_ON_SCREEN_OFF, false)
    fun setHomeOnScreenOff(value: Boolean) {
        prefs.edit().putBoolean(KEY_HOME_ON_SCREEN_OFF, value).apply()
    }

    fun themeMode(): ThemeMode = runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: "") }.getOrDefault(ThemeMode.DARK)
    fun setThemeMode(value: ThemeMode) {
        prefs.edit().putString(KEY_THEME, value.name).apply()
    }

    /** Auto mode: light from this minute of the day... */
    fun lightFromMinutes(): Int = prefs.getInt(KEY_LIGHT_FROM, DEFAULT_LIGHT_FROM)
    fun lightToMinutes(): Int = prefs.getInt(KEY_LIGHT_TO, DEFAULT_LIGHT_TO)
    fun setLightMinutes(from: Int, to: Int) {
        prefs.edit().putInt(KEY_LIGHT_FROM, from).putInt(KEY_LIGHT_TO, to).apply()
    }

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LIGHT_FROM = "light_from"
        private const val KEY_LIGHT_TO = "light_to"
        const val DEFAULT_LIGHT_FROM = 7 * 60
        const val DEFAULT_LIGHT_TO = 20 * 60
        private const val KEY_HOME_OVERRIDE = "home_override"
        private const val KEY_HOME_ON_SCREEN_OFF = "home_on_screen_off"
        private const val KEY_TRANSPARENCY = "dock_transparency"
        private const val KEY_SIZE = "dock_size"
        const val DEFAULT_TRANSPARENCY = 50
        const val DEFAULT_SIZE = 100
        const val MIN_SIZE = 70
        const val MAX_SIZE = 140
    }
}
