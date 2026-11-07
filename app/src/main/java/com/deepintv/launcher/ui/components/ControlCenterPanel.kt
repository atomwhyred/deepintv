package com.deepintv.launcher.ui.components

import com.deepintv.launcher.data.BackupRepository
import com.deepintv.launcher.data.RefreshPrefsRepository
import com.deepintv.launcher.data.RefreshSettings
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import com.deepintv.launcher.data.ThemeMode
import com.deepintv.launcher.ui.theme.DeepinThemeState
import androidx.compose.ui.res.stringResource
import com.deepintv.launcher.R
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import com.deepintv.launcher.ui.theme.DeepinWash
import com.deepintv.launcher.ui.theme.DeepinOnFocus
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.deepintv.launcher.data.AppInfo
import com.deepintv.launcher.data.AudioOutputRepository
import com.deepintv.launcher.data.LauncherSettingsRepository
import com.deepintv.launcher.data.MemoryRepository
import com.deepintv.launcher.data.WifiRepository
import com.deepintv.launcher.service.DeepinAccessibilityService
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The right-side "dde-control-center" panel: network, sound, power, memory and the launcher's
 * own display settings (dock transparency/size). */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ControlCenterPanel(
    apps: List<AppInfo>,
    modifier: Modifier = Modifier,
    onDockTransparencyChange: (Int) -> Unit,
    onDockSizeChange: (Int) -> Unit,
    onKeyRemapClick: () -> Unit,
    onWallpaperClick: () -> Unit,
    onPairingClick: () -> Unit,
    refresh: RefreshSettings,
    onRefreshChange: (RefreshSettings) -> Unit,
    onBackupRestored: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val wifiRepository = remember { WifiRepository(context) }
    var wifiStatus by remember { mutableStateOf(wifiRepository.currentStatus()) }
    val audioRepository = remember { AudioOutputRepository(context) }
    var audioOutputs by remember { mutableStateOf(audioRepository.listOutputs()) }
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    val settingsRepository = remember { LauncherSettingsRepository(context) }
    var volume by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    // The panel stays open while the remote's own volume keys, a Wi-Fi drop or a Bluetooth speaker
    // change things underneath it: follow them instead of showing what was true when it opened.
    DisposableEffect(wifiRepository) {
        val stop = wifiRepository.observe { wifiStatus = wifiRepository.currentStatus() }
        onDispose(stop)
    }
    DisposableEffect(audioRepository) {
        val stop = audioRepository.observe { audioOutputs = audioRepository.listOutputs() }
        onDispose(stop)
    }
    DisposableEffect(audioManager) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            }
        }
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    var dockTransparency by remember { mutableIntStateOf(settingsRepository.dockTransparencyPercent()) }
    var dockSize by remember { mutableIntStateOf(settingsRepository.dockSizePercent()) }
    var cleanupMessage by remember { mutableStateOf<String?>(null) }
    var homeOverride by remember { mutableStateOf(settingsRepository.homeOverride()) }
    var consumeVolume by remember { mutableStateOf(settingsRepository.consumeVolumeKeys()) }
    var filterOffStandby by remember { mutableStateOf(settingsRepository.filterOffInStandby()) }
    var homeOnScreenOff by remember { mutableStateOf(settingsRepository.homeOnScreenOff()) }
    val refreshRepository = remember { RefreshPrefsRepository(context) }
    val backupRepository = remember { BackupRepository(context) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var sleepMinutesLeft by remember { mutableIntStateOf(((DeepinAccessibilityService.instance?.sleepRemainingMs() ?: 0L) / 60_000L).toInt().let { if (it == 0 && (DeepinAccessibilityService.instance?.sleepRemainingMs() ?: 0L) > 0L) 1 else it }) }
    var sleepPresetMinutes by remember { mutableIntStateOf(sleepMinutesLeft.takeIf { it > 0 }?.let { left -> listOf(15, 30, 45, 60, 90, 120).firstOrNull { it >= left } ?: 120 } ?: 0) }
    LaunchedEffect(Unit) {
        while (true) {
            val ms = DeepinAccessibilityService.instance?.sleepRemainingMs() ?: 0L
            sleepMinutesLeft = if (ms <= 0L) 0 else ((ms + 59_999L) / 60_000L).toInt()
            if (sleepMinutesLeft == 0) sleepPresetMinutes = 0
            kotlinx.coroutines.delay(15_000)
        }
    }
    val firstRowFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstRowFocusRequester.requestFocus() }

    GlassSurface(modifier = modifier.width(380.dp).heightIn(max = 800.dp), cornerRadius = 20) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                // Traps the D-pad inside the panel: without this, Up/Down/Left/Right at an edge
                // (or a DPadBar at its min/max) can jump focus to the Home screen underneath,
                // which an overlay must never allow — only Back (BackHandler) should close it.
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusRestorer()
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text(stringResource(R.string.control_title), color = DeepinTextPrimary, fontSize = 20.sp)

            Section(icon = Icons.Filled.Wifi, title = stringResource(R.string.section_network)) {
                // Apps can't toggle Wi-Fi on Android 10+, so this opens the real settings page
                // instead of a switch that would silently fail.
                SettingsRow(
                    text = if (wifiStatus.connected) stringResource(R.string.wifi_connected, wifiStatus.ssid ?: stringResource(R.string.wifi_hidden)) else stringResource(R.string.wifi_none),
                    modifier = Modifier.focusRequester(firstRowFocusRequester),
                    onClick = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                )
            }

            Section(icon = Icons.Filled.VolumeUp, title = stringResource(R.string.section_sound)) {
                DPadBar(value = volume, range = 0..maxVolume, onChange = {
                    volume = it
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0)
                })
                // Read-only, same reason as Wi-Fi: a normal app can route communication audio
                // but not force general media playback to a specific output.
                audioOutputs.forEach { output ->
                    SettingsRow(
                        text = output.label,
                        icon = if (output.isBluetooth) Icons.Filled.Bluetooth else Icons.Filled.Speaker,
                        modifier = Modifier.padding(top = 6.dp),
                        onClick = { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) },
                    )
                }
            }

            Section(icon = Icons.Filled.PowerSettingsNew, title = stringResource(R.string.section_power)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    PowerButton(Icons.Filled.Lock, stringResource(R.string.power_lock)) { DeepinAccessibilityService.instance?.lockScreen() }
                    PowerButton(Icons.Filled.PowerSettingsNew, stringResource(R.string.power_off)) { DeepinAccessibilityService.instance?.openPowerDialog() }
                }
                SettingsRow(
                    text = stringResource(R.string.sleep_timer),
                    value = if (sleepMinutesLeft > 0) stringResource(R.string.sleep_remaining, sleepMinutesLeft) else stringResource(R.string.sleep_off),
                    modifier = Modifier.padding(top = 6.dp),
                    onClick = {
                        val service = DeepinAccessibilityService.instance
                        if (service != null) {
                            val options = listOf(0, 15, 30, 45, 60, 90, 120)
                            // Next option above what is set now (rounded to the preset it started from).
                            val next = options.firstOrNull { it > sleepPresetMinutes } ?: 0
                            sleepPresetMinutes = next
                            service.setSleepTimer(next)
                            sleepMinutesLeft = next
                        }
                    },
                )
                if (!DeepinAccessibilityService.isEnabled) {
                    SettingsRow(
                        text = stringResource(R.string.cc_needs_accessibility),
                        small = true,
                        onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    )
                }
            }

            Section(icon = Icons.Filled.CleaningServices, title = stringResource(R.string.section_memory)) {
                SettingsRow(
                    text = cleanupMessage ?: stringResource(R.string.memory_free_ram),
                    onClick = {
                        scope.launch {
                            val freedKb = withContext(Dispatchers.IO) { MemoryRepository(context).autocleanup(apps) }
                            cleanupMessage = if (freedKb > 0) context.getString(R.string.memory_freed, freedKb / 1024) else context.getString(R.string.memory_nothing)
                        }
                    },
                )
            }

            Section(icon = Icons.Filled.Tune, title = stringResource(R.string.section_launcher)) {
                Text(text = stringResource(R.string.dock_transparency), color = DeepinTextSecondary, fontSize = 12.sp)
                DPadBar(value = dockTransparency, range = 0..100, step = 5, onChange = {
                    dockTransparency = it
                    settingsRepository.setDockTransparencyPercent(it)
                    onDockTransparencyChange(it)
                })
                Text(
                    text = stringResource(R.string.icon_size),
                    color = DeepinTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                DPadBar(
                    value = dockSize,
                    range = LauncherSettingsRepository.MIN_SIZE..LauncherSettingsRepository.MAX_SIZE,
                    step = 5,
                    onChange = {
                        dockSize = it
                        settingsRepository.setDockSizePercent(it)
                        onDockSizeChange(it)
                    },
                )
                SettingsRow(
                    text = stringResource(R.string.remap_title),
                    modifier = Modifier.padding(top = 10.dp),
                    onClick = onKeyRemapClick,
                )
                SettingsRow(
                    text = stringResource(R.string.wallpaper_title),
                    modifier = Modifier.padding(top = 6.dp),
                    onClick = onWallpaperClick,
                )
                SettingsRow(
                    text = stringResource(R.string.home_override),
                    value = stringResource(if (homeOverride) R.string.on else R.string.off),
                    modifier = Modifier.padding(top = 6.dp),
                    onClick = {
                        homeOverride = !homeOverride
                        settingsRepository.setHomeOverride(homeOverride)
                    },
                )
                SettingsRow(
                    text = stringResource(R.string.home_on_sleep),
                    value = stringResource(if (homeOnScreenOff) R.string.on else R.string.off),
                    modifier = Modifier.padding(top = 6.dp),
                    onClick = {
                        homeOnScreenOff = !homeOnScreenOff
                        settingsRepository.setHomeOnScreenOff(homeOnScreenOff)
                    },
                )
                SettingsRow(
                    text = stringResource(R.string.volume_ramp),
                    value = stringResource(if (consumeVolume) R.string.on else R.string.off),
                    modifier = Modifier.padding(top = 6.dp),
                    onClick = {
                        consumeVolume = !consumeVolume
                        settingsRepository.setConsumeVolumeKeys(consumeVolume)
                        DeepinAccessibilityService.instance?.updateKeyFilter()
                    },
                )
                SettingsRow(
                    text = stringResource(R.string.filter_off_standby),
                    value = stringResource(if (filterOffStandby) R.string.on else R.string.off),
                    modifier = Modifier.padding(top = 6.dp),
                    onClick = {
                        filterOffStandby = !filterOffStandby
                        settingsRepository.setFilterOffInStandby(filterOffStandby)
                        DeepinAccessibilityService.instance?.updateKeyFilter()
                    },
                )
                if ((homeOverride || homeOnScreenOff || consumeVolume) && !DeepinAccessibilityService.isEnabled) {
                    SettingsRow(
                        text = stringResource(R.string.cc_needs_accessibility),
                        small = true,
                        onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    )
                }
            }

            Section(icon = Icons.Filled.Palette, title = stringResource(R.string.section_theme)) {
                SettingsRow(
                    text = stringResource(R.string.theme_mode),
                    value = stringResource(
                        when (DeepinThemeState.mode) {
                            ThemeMode.DARK -> R.string.theme_dark
                            ThemeMode.LIGHT -> R.string.theme_light
                            ThemeMode.AUTO -> R.string.theme_auto
                        },
                    ),
                    onClick = {
                        val next = ThemeMode.entries[(DeepinThemeState.mode.ordinal + 1) % ThemeMode.entries.size]
                        settingsRepository.setThemeMode(next)
                        DeepinThemeState.mode = next
                    },
                )
                if (DeepinThemeState.mode == ThemeMode.AUTO) {
                    SettingsRow(
                        text = stringResource(R.string.theme_light_from),
                        value = clockLabel(DeepinThemeState.lightFromMinutes),
                        modifier = Modifier.padding(top = 6.dp),
                        onClick = {
                            val next = (DeepinThemeState.lightFromMinutes + 30) % (24 * 60)
                            settingsRepository.setLightMinutes(next, DeepinThemeState.lightToMinutes)
                            DeepinThemeState.lightFromMinutes = next
                        },
                    )
                    SettingsRow(
                        text = stringResource(R.string.theme_light_to),
                        value = clockLabel(DeepinThemeState.lightToMinutes),
                        modifier = Modifier.padding(top = 6.dp),
                        onClick = {
                            val next = (DeepinThemeState.lightToMinutes + 30) % (24 * 60)
                            settingsRepository.setLightMinutes(DeepinThemeState.lightFromMinutes, next)
                            DeepinThemeState.lightToMinutes = next
                        },
                    )
                }
            }

            Section(icon = Icons.Filled.Refresh, title = stringResource(R.string.section_refresh)) {
                SettingsRow(
                    text = stringResource(R.string.widget_weather),
                    value = minutesLabel(refresh.weatherMinutes),
                    onClick = {
                        val next = RefreshPrefsRepository.next(RefreshPrefsRepository.WEATHER_OPTIONS, refresh.weatherMinutes)
                        refreshRepository.setWeatherMinutes(next)
                        onRefreshChange(refresh.copy(weatherMinutes = next))
                    },
                )
                SettingsRow(
                    text = stringResource(R.string.widget_crypto),
                    value = minutesLabel(refresh.cryptoMinutes),
                    modifier = Modifier.padding(top = 6.dp),
                    onClick = {
                        val next = RefreshPrefsRepository.next(RefreshPrefsRepository.CRYPTO_OPTIONS, refresh.cryptoMinutes)
                        refreshRepository.setCryptoMinutes(next)
                        onRefreshChange(refresh.copy(cryptoMinutes = next))
                    },
                )
                SettingsRow(
                    text = stringResource(R.string.widget_calendar),
                    value = minutesLabel(refresh.calendarMinutes),
                    modifier = Modifier.padding(top = 6.dp),
                    onClick = {
                        val next = RefreshPrefsRepository.next(RefreshPrefsRepository.CALENDAR_OPTIONS, refresh.calendarMinutes)
                        refreshRepository.setCalendarMinutes(next)
                        onRefreshChange(refresh.copy(calendarMinutes = next))
                    },
                )
                SettingsRow(
                    text = stringResource(R.string.widget_system),
                    value = stringResource(R.string.refresh_every_sec, refresh.systemSeconds),
                    modifier = Modifier.padding(top = 6.dp),
                    onClick = {
                        val next = RefreshPrefsRepository.next(RefreshPrefsRepository.SYSTEM_OPTIONS, refresh.systemSeconds)
                        refreshRepository.setSystemSeconds(next)
                        onRefreshChange(refresh.copy(systemSeconds = next))
                    },
                )
                SettingsRow(
                    text = stringResource(R.string.widget_watch_next),
                    value = minutesLabel(refresh.watchNextMinutes),
                    modifier = Modifier.padding(top = 6.dp),
                    onClick = {
                        val next = RefreshPrefsRepository.next(RefreshPrefsRepository.WATCH_NEXT_OPTIONS, refresh.watchNextMinutes)
                        refreshRepository.setWatchNextMinutes(next)
                        onRefreshChange(refresh.copy(watchNextMinutes = next))
                    },
                )
            }

            Section(icon = Icons.Filled.Backup, title = stringResource(R.string.section_backup)) {
                SettingsRow(
                    text = stringResource(R.string.backup_export),
                    onClick = {
                        backupMessage = if (backupRepository.export()) {
                            context.getString(R.string.backup_saved, BackupRepository.FILE_NAME)
                        } else {
                            context.getString(R.string.backup_failed)
                        }
                    },
                )
                SettingsRow(
                    text = stringResource(R.string.backup_restore),
                    modifier = Modifier.padding(top = 6.dp),
                    onClick = {
                        if (!backupRepository.hasBackup()) {
                            backupMessage = context.getString(R.string.backup_none)
                        } else if (backupRepository.restore()) {
                            DeepinAccessibilityService.instance?.refreshMappings()
                            onBackupRestored()
                        } else {
                            backupMessage = context.getString(R.string.backup_restore_failed)
                        }
                    },
                )
                SettingsRow(
                    text = stringResource(R.string.pair_row),
                    modifier = Modifier.padding(top = 6.dp),
                    onClick = onPairingClick,
                )
                backupMessage?.let {
                    Text(text = it, color = DeepinTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }

            Section(icon = Icons.Filled.Settings, title = stringResource(R.string.pill_settings)) {
                SettingsRow(
                    text = stringResource(R.string.android_tv_settings),
                    onClick = { context.startActivity(Intent(Settings.ACTION_SETTINGS)) },
                )
            }
        }
    }
}

private fun clockLabel(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

@Composable
private fun Section(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(DeepinWash),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = DeepinTextPrimary, modifier = Modifier.size(13.dp))
            }
            Text(
                text = title,
                color = DeepinTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Column(modifier = Modifier.padding(top = 10.dp)) { content() }
    }
}

@Composable
private fun SettingsRow(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    small: Boolean = false,
    value: String? = null,
    onClick: () -> Unit,
) {
    // Same focus treatment as the top pills: white border, dark content on the light highlight.
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        scale = noFocusScale(),
        border = focusBorder(shape),
        interactionSource = interactionSource,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) DeepinOnFocus else DeepinTextSecondary,
                    modifier = Modifier.size(16.dp).padding(end = 10.dp),
                )
            }
            Text(
                text = text,
                color = if (isFocused) DeepinOnFocus else if (small) DeepinTextSecondary else DeepinTextPrimary,
                fontSize = if (small) 12.sp else 14.sp,
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                Text(
                    text = value,
                    color = focusContent(isFocused, DeepinTextSecondary),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = if (isFocused) DeepinOnFocus else DeepinTextSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun PowerButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
            scale = noFocusScale(),
            border = focusBorder(CircleShape),
            interactionSource = interactionSource,
        ) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isFocused) DeepinOnFocus else DeepinTextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = label,
            color = DeepinTextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun minutesLabel(minutes: Int): String =
    if (minutes % 60 == 0) stringResource(R.string.refresh_every_hour, minutes / 60)
    else stringResource(R.string.refresh_every_min, minutes)
