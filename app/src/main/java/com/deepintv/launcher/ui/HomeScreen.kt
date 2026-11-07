package com.deepintv.launcher.ui

import com.deepintv.launcher.ui.components.CalendarEditSection
import com.deepintv.launcher.ui.components.CalendarWidget
import com.deepintv.launcher.data.RecentsRepository
import com.deepintv.launcher.data.RefreshPrefsRepository
import com.deepintv.launcher.R
import androidx.compose.ui.res.stringResource
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.deepintv.launcher.data.AppInfo
import com.deepintv.launcher.data.AppRepository
import com.deepintv.launcher.data.CryptoPrefsRepository
import com.deepintv.launcher.data.FavoritesRepository
import com.deepintv.launcher.data.LauncherSettingsRepository
import com.deepintv.launcher.data.RemapAction
import com.deepintv.launcher.data.SafeModeRepository
import com.deepintv.launcher.data.TvInputItem
import com.deepintv.launcher.data.TvInputRepository
import com.deepintv.launcher.data.UsageRepository
import com.deepintv.launcher.data.WallpaperRepository
import com.deepintv.launcher.data.WidgetKind
import com.deepintv.launcher.data.WidgetPrefsRepository
import com.deepintv.launcher.ui.components.AppContextMenu
import com.deepintv.launcher.ui.components.AppDrawer
import com.deepintv.launcher.ui.components.ControlCenterPanel
import com.deepintv.launcher.ui.components.CryptoWidget
import com.deepintv.launcher.ui.components.NotificationsEditSection
import com.deepintv.launcher.ui.components.PairingScreen
import com.deepintv.launcher.ui.components.NotificationsWidget
import com.deepintv.launcher.ui.components.WatchNextEditSection
import com.deepintv.launcher.ui.components.WatchNextWidget
import com.deepintv.launcher.ui.components.Dock
import com.deepintv.launcher.ui.components.InputsDropdown
import com.deepintv.launcher.ui.components.KeyRemapPicker
import com.deepintv.launcher.ui.components.PillsRow
import com.deepintv.launcher.ui.components.SystemMonitorWidget
import com.deepintv.launcher.ui.components.TopBar
import com.deepintv.launcher.ui.components.WallpaperBackground
import com.deepintv.launcher.ui.components.WallpaperPicker
import com.deepintv.launcher.ui.components.WeatherWidget
import com.deepintv.launcher.ui.components.WidgetEditSheet
import com.deepintv.launcher.ui.components.VpnEditSection
import com.deepintv.launcher.ui.components.WeatherEditSection
import com.deepintv.launcher.data.VpnApp
import com.deepintv.launcher.data.VpnAppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.deepintv.launcher.data.WeatherLocationRepository
import com.deepintv.launcher.ui.components.WidgetPicker
import androidx.compose.material.icons.filled.Apps
import com.deepintv.launcher.service.DeepinAccessibilityService

/** Dock slots shown when the person has never customised favourites yet. */
private const val DEFAULT_FAVOURITE_COUNT = 6

/** How long to wait after a package broadcast before reloading the app list. */
private const val PACKAGE_SETTLE_MS = 600L

private enum class Overlay { NONE, INPUTS, CONTROL_CENTER, APP_DRAWER, WIDGET_PICKER, KEY_REMAP, WALLPAPER_PICKER, PAIRING }

private data class ContextMenuTarget(val app: AppInfo, val isInDock: Boolean)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(appRepository: AppRepository) {
    val context = LocalContext.current
    // PackageManager queries and icon loading are slow on a 1 GB TV: do them off the main thread
    // so Home draws immediately and the dock fills in once the lists arrive.
    // Bumped when an app is installed, updated or removed, so the lists below reload by themselves.
    var packagesVersion by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { packagesVersion++ }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    val apps by produceState(emptyList<AppInfo>(), packagesVersion) {
        // An update fires removed + added + replaced in a burst: wait for it to settle, then load once.
        if (packagesVersion > 0) delay(PACKAGE_SETTLE_MS)
        value = withContext(Dispatchers.Default) { appRepository.listLaunchableApps() }
    }
    val appsByPackage = remember(apps) { apps.associateBy { it.packageName } }
    val favoritesRepository = remember { FavoritesRepository(context) }
    val usageRepository = remember { UsageRepository(context) }
    val recentsRepository = remember { RecentsRepository(context) }
    var recentPackages by remember { mutableStateOf(recentsRepository.load()) }
    val settingsRepository = remember { LauncherSettingsRepository(context) }
    var dockTransparency by remember { mutableStateOf(settingsRepository.dockTransparencyPercent()) }
    var dockSize by remember { mutableStateOf(settingsRepository.dockSizePercent()) }
    val widgetPrefsRepository = remember { WidgetPrefsRepository(context) }
    var chosenWidgets by remember { mutableStateOf(widgetPrefsRepository.enabledWidgets()) }
    // After repeated crashes the widgets that fetch from the network are left out (not disabled in
    // the settings) until the launcher has run for a while without crashing.
    val safeModeRepository = remember { SafeModeRepository(context) }
    val safeMode = remember { safeModeRepository.isActive() }
    val enabledWidgets = if (safeMode) chosenWidgets.filter { it == WidgetKind.SYSTEM } else chosenWidgets
    LaunchedEffect(Unit) {
        if (safeMode) com.deepintv.launcher.ui.components.showGlassToast(context, R.string.safe_mode_active, long = true)
        delay(SafeModeRepository.HEALTHY_AFTER_MS)
        safeModeRepository.markHealthy()
    }
    val cryptoPrefsRepository = remember { CryptoPrefsRepository(context) }
    var selectedCoinIds by remember { mutableStateOf(cryptoPrefsRepository.selectedIds()) }
    var editingWidget by remember { mutableStateOf<WidgetKind?>(null) }
    val weatherLocationRepository = remember { WeatherLocationRepository(context) }
    var weatherLocation by remember { mutableStateOf(weatherLocationRepository.saved()) }
    val refreshRepository = remember { RefreshPrefsRepository(context) }
    var refresh by remember { mutableStateOf(refreshRepository.load()) }
    var calendarVersion by remember { mutableStateOf(0) }
    var watchNextVersion by remember { mutableStateOf(0) }
    val vpnAppRepository = remember { VpnAppRepository(context) }
    val vpnApps by produceState(emptyList<VpnApp>(), packagesVersion) {
        if (packagesVersion > 0) delay(PACKAGE_SETTLE_MS)
        value = withContext(Dispatchers.Default) { vpnAppRepository.installed() }
    }
    var selectedVpnPackage by remember { mutableStateOf(vpnAppRepository.selectedPackage()) }
    val vpnApp = remember(vpnApps, selectedVpnPackage) {
        vpnApps.firstOrNull { it.packageName == selectedVpnPackage } ?: vpnApps.firstOrNull()
    }
    val wallpaperRepository = remember { WallpaperRepository(context) }
    var wallpaperSettings by remember { mutableStateOf(wallpaperRepository.settings()) }

    var favoritePackages by remember { mutableStateOf(favoritesRepository.load()) }
    // Nothing customised yet: seed the dock with the first apps, once the list has loaded.
    LaunchedEffect(apps) {
        if (apps.isNotEmpty() && favoritePackages.isEmpty()) {
            val defaults = apps.take(DEFAULT_FAVOURITE_COUNT).map { it.packageName }
            favoritesRepository.save(defaults)
            favoritePackages = defaults
        }
    }
    val favourites = remember(favoritePackages, apps) {
        favoritePackages.mapNotNull { appsByPackage[it] }
    }

    var overlay by remember { mutableStateOf(Overlay.NONE) }
    // Re-read each time the dropdown opens so the connection state is current (no live callback).
    val inputsOpen = overlay == Overlay.INPUTS
    val inputs by produceState(emptyList<TvInputItem>(), inputsOpen) {
        value = withContext(Dispatchers.Default) { TvInputRepository(context).listPhysicalInputs() }
    }
    var contextMenuTarget by remember { mutableStateOf<ContextMenuTarget?>(null) }
    // Overlays leave composition on close, taking the focused node with them, so Home asks for
    // focus back on whatever opened them (the dock when nothing in particular did).
    val dockRequester = remember { FocusRequester() }
    val appsPillRequester = remember { FocusRequester() }
    val widgetsRowRequester = remember { FocusRequester() }
    val widgetRequesters = remember { WidgetKind.entries.associateWith { FocusRequester() } }
    val inputsPillRequester = remember { FocusRequester() }
    val controlPillRequester = remember { FocusRequester() }
    val widgetsPillRequester = remember { FocusRequester() }
    var returnFocus by remember { mutableStateOf<FocusRequester?>(null) }
    val idle = overlay == Overlay.NONE && contextMenuTarget == null && editingWidget == null
    LaunchedEffect(idle) {
        if (idle) {
            withFrameNanos { }
            // The edited card may be gone (removed), so fall back to the row, then the dock.
            val candidates = returnFocus?.let { listOf(it, widgetsRowRequester, dockRequester) } ?: listOf(dockRequester)
            candidates.firstOrNull { runCatching { it.requestFocus() }.isSuccess }
            returnFocus = null
        }
    }
    BackHandler(enabled = overlay != Overlay.NONE || contextMenuTarget != null || editingWidget != null) {
        when {
            contextMenuTarget != null -> contextMenuTarget = null
            editingWidget != null -> editingWidget = null
            overlay == Overlay.KEY_REMAP || overlay == Overlay.WALLPAPER_PICKER || overlay == Overlay.PAIRING -> overlay = Overlay.CONTROL_CENTER
            else -> overlay = Overlay.NONE
        }
    }

    // A remapped remote button can request an overlay only Compose knows how to draw; the
    // accessibility service holds no UI state of its own, so it calls back into Home instead.
    DisposableEffect(Unit) {
        DeepinAccessibilityService.onOverlayRequest = { action ->
            overlay = when (action) {
                RemapAction.ControlCenter -> Overlay.CONTROL_CENTER
                RemapAction.AppDrawer -> Overlay.APP_DRAWER
                else -> overlay
            }
        }
        onDispose { DeepinAccessibilityService.onOverlayRequest = null }
    }

    fun toggleFavorite(app: AppInfo) {
        favoritePackages = if (app.packageName in favoritePackages) {
            favoritesRepository.remove(app.packageName)
            favoritePackages - app.packageName
        } else {
            favoritesRepository.add(app.packageName)
            favoritePackages + app.packageName
        }
    }

    fun moveFavorite(app: AppInfo, delta: Int) {
        favoritesRepository.move(app.packageName, delta)
        favoritePackages = favoritesRepository.load()
    }

    fun launch(app: AppInfo) {
        // Only count it if it opened: an app removed a moment ago would otherwise gain a launch.
        if (safeStartActivity(context, app.launchIntent)) {
            usageRepository.recordLaunch(app.packageName)
            recentsRepository.push(app.packageName)
            recentPackages = recentsRepository.load()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        WallpaperBackground(settings = wallpaperSettings, modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TopBar()
                PillsRow(
                    inputsRequester = inputsPillRequester,
                    controlRequester = controlPillRequester,
                    widgetsRequester = widgetsPillRequester,
                    onInputsClick = {
                        returnFocus = inputsPillRequester
                        overlay = if (overlay == Overlay.INPUTS) Overlay.NONE else Overlay.INPUTS
                    },
                    onControlCenterClick = {
                        returnFocus = controlPillRequester
                        overlay = if (overlay == Overlay.CONTROL_CENTER) Overlay.NONE else Overlay.CONTROL_CENTER
                    },
                    onPowerClick = {
                        val service = DeepinAccessibilityService.instance
                        if (service != null) service.openPowerDialog()
                        else com.deepintv.launcher.ui.components.showGlassToast(context, R.string.cc_needs_accessibility, long = true)
                    },
                    onWidgetsClick = {
                        returnFocus = widgetsPillRequester
                        overlay = Overlay.WIDGET_PICKER
                    },
                )
            }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(198.dp)
                    .focusRequester(widgetsRowRequester)
                    // No focusRestorer here: on this LazyRow it swallowed focus when the row was
                    // entered from the pills or the dock, leaving nothing focused. Plain
                    // geometric search lands on the nearest card instead.
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                // Room for the focused card's ring, which the row would otherwise clip.
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            ) {
                lazyRowItems(enabledWidgets, key = { it.name }) { kind ->
                    val onClick = {
                        returnFocus = widgetRequesters.getValue(kind)
                        editingWidget = kind
                    }
                    val cardModifier = Modifier.focusRequester(widgetRequesters.getValue(kind))
                    when (kind) {
                        WidgetKind.WEATHER -> WeatherWidget(location = weatherLocation, refreshMinutes = refresh.weatherMinutes, onClick = onClick, modifier = cardModifier)
                        WidgetKind.CALENDAR -> CalendarWidget(refreshMinutes = refresh.calendarMinutes, feedsVersion = calendarVersion, onClick = onClick, modifier = cardModifier)
                        WidgetKind.SYSTEM -> SystemMonitorWidget(vpnApp = vpnApp, refreshSeconds = refresh.systemSeconds, onClick = onClick, modifier = cardModifier)
                        WidgetKind.CRYPTO -> CryptoWidget(selectedIds = selectedCoinIds, refreshMinutes = refresh.cryptoMinutes, onClick = onClick, modifier = cardModifier)
                        WidgetKind.NOTIFICATIONS -> NotificationsWidget(onClick = onClick, modifier = cardModifier)
                        WidgetKind.WATCH_NEXT -> WatchNextWidget(refreshMinutes = refresh.watchNextMinutes, version = watchNextVersion, onClick = onClick, modifier = cardModifier)
                    }
                }
            }
            // No app grid on Home: widgets, the top pills and the dock's favourites are the
            // whole surface. Every app still lives behind "Todas las apps" (Overlay.APP_DRAWER).
            Box(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Dock(
                    favourites = favourites,
                    modifier = Modifier.weight(1f),
                    transparencyPercent = dockTransparency,
                    iconSizeDp = dockSize * 120 / 100,
                    focusRequester = dockRequester,
                    onAppClick = { launch(it) },
                    onAppLongClick = { contextMenuTarget = ContextMenuTarget(it, isInDock = true) },
                )
                com.deepintv.launcher.ui.components.IconPill(
                    icon = androidx.compose.material.icons.Icons.Filled.Apps,
                    contentDescription = stringResource(R.string.pill_apps),
                    modifier = Modifier.focusRequester(appsPillRequester),
                    onClick = {
                        returnFocus = appsPillRequester
                        overlay = Overlay.APP_DRAWER
                    },
                )
            }
        }

        if (overlay == Overlay.INPUTS) {
            Box(modifier = Modifier.fillMaxSize().padding(top = 110.dp, end = 32.dp)) {
                InputsDropdown(
                    inputs = inputs,
                    modifier = Modifier.align(Alignment.TopEnd),
                    onSelect = {
                        safeStartActivity(context, it.switchIntent)
                        overlay = Overlay.NONE
                    },
                )
            }
        }

        if (overlay == Overlay.CONTROL_CENTER) {
            Box(modifier = Modifier.fillMaxSize().padding(top = 110.dp, end = 32.dp, bottom = 16.dp)) {
                ControlCenterPanel(
                    apps = apps,
                    modifier = Modifier.align(Alignment.TopEnd),
                    onDockTransparencyChange = { dockTransparency = it },
                    onDockSizeChange = { dockSize = it },
                    onKeyRemapClick = { overlay = Overlay.KEY_REMAP },
                    onWallpaperClick = { overlay = Overlay.WALLPAPER_PICKER },
                    onPairingClick = { overlay = Overlay.PAIRING },
                    refresh = refresh,
                    onRefreshChange = { refresh = it },
                    onBackupRestored = { (context as? android.app.Activity)?.recreate() },
                )
            }
        }

        if (overlay == Overlay.APP_DRAWER) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                AppDrawer(
                    apps = apps,
                    usageRepository = usageRepository,
                    recentPackages = recentPackages,
                    focusEnabled = contextMenuTarget == null,
                    onAppClick = {
                        launch(it)
                        overlay = Overlay.NONE
                    },
                    onAppLongClick = { contextMenuTarget = ContextMenuTarget(it, isInDock = false) },
                )
            }
        }

        if (overlay == Overlay.WIDGET_PICKER) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                WidgetPicker(
                    enabled = chosenWidgets,
                    onToggle = { kind, isEnabled ->
                        widgetPrefsRepository.setEnabled(kind, isEnabled)
                        chosenWidgets = widgetPrefsRepository.enabledWidgets()
                    },
                    onMove = { kind, delta ->
                        widgetPrefsRepository.move(kind, delta)
                        chosenWidgets = widgetPrefsRepository.enabledWidgets()
                    },
                    onDismiss = { overlay = Overlay.NONE },
                )
            }
        }

        if (overlay == Overlay.PAIRING) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                PairingScreen(
                    onDismiss = { overlay = Overlay.CONTROL_CENTER },
                    // The phone changed settings: recreate so Home reads them all again.
                    onApplied = { (context as? android.app.Activity)?.recreate() },
                )
            }
        }

        if (overlay == Overlay.KEY_REMAP) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                KeyRemapPicker(
                    apps = apps,
                    onDismiss = { overlay = Overlay.CONTROL_CENTER },
                )
            }
        }

        if (overlay == Overlay.WALLPAPER_PICKER) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                WallpaperPicker(
                    settings = wallpaperSettings,
                    onPickBuiltin = {
                        wallpaperRepository.setBuiltin(it)
                        wallpaperSettings = wallpaperRepository.settings()
                    },
                    onPickCustomUri = {
                        wallpaperRepository.setCustomUri(it)
                        wallpaperSettings = wallpaperRepository.settings()
                    },
                    onDimChange = {
                        wallpaperRepository.setDimPercent(it)
                        wallpaperSettings = wallpaperRepository.settings()
                    },
                    onDismiss = { overlay = Overlay.CONTROL_CENTER },
                )
            }
        }

        editingWidget?.let { kind ->
            val index = enabledWidgets.indexOf(kind)
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                WidgetEditSheet(
                    kind = kind,
                    canMoveLeft = index > 0,
                    canMoveRight = index in 0 until enabledWidgets.lastIndex,
                    selectedCoinIds = selectedCoinIds,
                    onMoveLeft = {
                        widgetPrefsRepository.move(kind, -1)
                        chosenWidgets = widgetPrefsRepository.enabledWidgets()
                    },
                    onMoveRight = {
                        widgetPrefsRepository.move(kind, 1)
                        chosenWidgets = widgetPrefsRepository.enabledWidgets()
                    },
                    onRemove = {
                        widgetPrefsRepository.setEnabled(kind, false)
                        chosenWidgets = widgetPrefsRepository.enabledWidgets()
                        editingWidget = null
                    },
                    onToggleCoin = { id, enabled ->
                        cryptoPrefsRepository.toggle(id, enabled)
                        selectedCoinIds = cryptoPrefsRepository.selectedIds()
                    },
                    onDismiss = { editingWidget = null },
                    extraContent = {
                        when (kind) {
                            WidgetKind.WEATHER -> WeatherEditSection(
                                location = weatherLocation,
                                onLocationChange = { weatherLocation = it },
                            )
                            WidgetKind.CALENDAR -> CalendarEditSection(onFeedsChange = { calendarVersion++ })
                            WidgetKind.SYSTEM -> VpnEditSection(
                                apps = vpnApps,
                                selectedPackage = vpnApp?.packageName,
                                onSelect = {
                                    vpnAppRepository.select(it.packageName)
                                    selectedVpnPackage = it.packageName
                                },
                                onOpen = { app -> app.launchIntent?.let { safeStartActivity(context, it) } },
                            )
                            WidgetKind.WATCH_NEXT -> WatchNextEditSection(onChanged = { watchNextVersion++ })
                            WidgetKind.NOTIFICATIONS -> NotificationsEditSection()
                            else -> Unit
                        }
                    },
                )
            }
        }

        contextMenuTarget?.let { target ->
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                AppContextMenu(
                    app = target.app,
                    isFavorite = target.app.packageName in favoritePackages,
                    isInDock = target.isInDock,
                    onToggleFavorite = { toggleFavorite(target.app) },
                    onMoveLeft = if (target.isInDock) {
                        { moveFavorite(target.app, -1) }
                    } else null,
                    onMoveRight = if (target.isInDock) {
                        { moveFavorite(target.app, 1) }
                    } else null,
                    onDismiss = { contextMenuTarget = null },
                )
            }
        }
        com.deepintv.launcher.ui.components.GlassToastHost()
    }
}
