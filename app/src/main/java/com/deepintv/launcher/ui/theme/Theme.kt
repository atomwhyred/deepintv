package com.deepintv.launcher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import com.deepintv.launcher.data.DeviceProfileRepository
import com.deepintv.launcher.data.DeviceTier
import com.deepintv.launcher.data.LauncherSettingsRepository
import com.deepintv.launcher.data.ThemeMode
import kotlinx.coroutines.delay
import java.util.Calendar

/** What the colours below read. They are plain getters over snapshot state, so every composable
 * that uses one recomposes by itself when the style changes: no call site needed touching. */
object DeepinThemeState {
    var isDark by mutableStateOf(true)
    var mode by mutableStateOf(ThemeMode.DARK)
    var lightFromMinutes by mutableIntStateOf(LauncherSettingsRepository.DEFAULT_LIGHT_FROM)
    var lightToMinutes by mutableIntStateOf(LauncherSettingsRepository.DEFAULT_LIGHT_TO)

    fun load(repository: LauncherSettingsRepository) {
        mode = repository.themeMode()
        lightFromMinutes = repository.lightFromMinutes()
        lightToMinutes = repository.lightToMinutes()
    }

    /** Light between [from] and [to] (it may cross midnight), dark outside. */
    fun isLightAt(minuteOfDay: Int): Boolean {
        val from = lightFromMinutes
        val to = lightToMinutes
        return if (from <= to) minuteOfDay in from until to else minuteOfDay >= from || minuteOfDay < to
    }
}

private val DarkBackground = Color(0xFF0B0E14)
// A solid slate-navy card tint, not a see-through tint: the first glass look (a thin white
// wash over the background) read poorly once real content sat behind it — overlapping text was
// reported unreadable on-device. Cards and overlays need a near-opaque backing to stay legible.
private val DarkSurface = Color(0xF0191F2B)
private val LightBackground = Color(0xFFF2F4F8)
private val LightSurface = Color(0xF5FFFFFF)

val DeepinBackground: Color get() = if (DeepinThemeState.isDark) DarkBackground else LightBackground
val DeepinSurface: Color get() = if (DeepinThemeState.isDark) DarkSurface else LightSurface
val DeepinBorder: Color get() = if (DeepinThemeState.isDark) Color(0x3DFFFFFF) else Color(0x24000000)
val DeepinBlue = Color(0xFF0A84FF)
val DeepinCyan = Color(0xFF1890FF)
val DeepinGreen = Color(0xFF30D158)
val DeepinRed = Color(0xFFFF453A)
val DeepinTextPrimary: Color get() = if (DeepinThemeState.isDark) Color(0xFFF5F6F8) else Color(0xFF10141C)
val DeepinTextSecondary: Color get() = if (DeepinThemeState.isDark) Color(0xFF9AA3B2) else Color(0xFF5B6577)

/** Ring and glow of a focused item: white on dark, the accent blue on light (white would vanish). */
val DeepinFocusRing: Color get() = if (DeepinThemeState.isDark) Color.White else DeepinBlue
/** Text and icons on a focused row: its highlight is near-white on dark and near-black on light. */
val DeepinOnFocus: Color get() = if (DeepinThemeState.isDark) Color.Black else Color.White
/** A faint wash for chips, tracks and icon badges that sit on a card. */
val DeepinWash: Color get() = DeepinTextPrimary.copy(alpha = 0.12f)

private val DeepinDarkScheme = darkColorScheme(
    primary = DeepinBlue,
    secondary = DeepinCyan,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = Color(0xFFF5F6F8),
    onSurface = Color(0xFFF5F6F8),
)

private val DeepinLightScheme = lightColorScheme(
    primary = DeepinBlue,
    secondary = DeepinCyan,
    background = LightBackground,
    surface = LightSurface,
    onBackground = Color(0xFF10141C),
    onSurface = Color(0xFF10141C),
)

/** The device tier, read by the few places that can draw less on a weak TV (see DeviceProfileRepository). */
val LocalDeviceTier = staticCompositionLocalOf { DeviceTier.FULL }

@Composable
fun DeepinTVTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val tier = remember { DeviceProfileRepository(context).tier() }
    // Loaded synchronously before the first frame so the theme never flashes the default style.
    remember { DeepinThemeState.load(LauncherSettingsRepository(context)); true }
    // Auto mode re-checks the clock every 30 s, so the switch lands within a moment of the hour.
    LaunchedEffect(DeepinThemeState.mode, DeepinThemeState.lightFromMinutes, DeepinThemeState.lightToMinutes) {
        while (true) {
            DeepinThemeState.isDark = when (DeepinThemeState.mode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.AUTO -> {
                    val now = Calendar.getInstance()
                    !DeepinThemeState.isLightAt(now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE))
                }
            }
            if (DeepinThemeState.mode != ThemeMode.AUTO) break
            delay(30_000)
        }
    }
    CompositionLocalProvider(LocalDeviceTier provides tier) {
        MaterialTheme(
            colorScheme = if (DeepinThemeState.isDark) DeepinDarkScheme else DeepinLightScheme,
            content = content,
        )
    }
}
