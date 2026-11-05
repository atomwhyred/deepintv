package com.deepintv.launcher.data

import android.content.Context

/** Reads the settings the phone may change into a [RemoteConfig], and writes a validated one back
 * through the same repositories the launcher itself uses (so ranges and formats stay theirs). */
class ConfigStore(private val context: Context) {

    fun rules() = ConfigRules(
        widgetNames = WidgetKind.entries.map { it.name }.toSet(),
        coinIds = AVAILABLE_COINS.map { it.id }.toSet(),
    )

    fun read(): RemoteConfig {
        val settings = LauncherSettingsRepository(context)
        val refresh = RefreshPrefsRepository(context).load()
        return RemoteConfig(
            theme = settings.themeMode().name,
            lightFrom = settings.lightFromMinutes(),
            lightTo = settings.lightToMinutes(),
            dockTransparency = settings.dockTransparencyPercent(),
            dockSize = settings.dockSizePercent(),
            widgets = WidgetPrefsRepository(context).enabledWidgets().map { it.name },
            coins = CryptoPrefsRepository(context).selectedIds(),
            refreshWeather = refresh.weatherMinutes,
            refreshCrypto = refresh.cryptoMinutes,
            refreshCalendar = refresh.calendarMinutes,
            refreshSystem = refresh.systemSeconds,
            refreshWatchNext = refresh.watchNextMinutes,
        )
    }

    /** Writes only the fields that are set. [config] must come from [ConfigCodec.parse]. */
    fun apply(config: RemoteConfig) {
        val settings = LauncherSettingsRepository(context)
        config.theme?.let { name -> ThemeMode.entries.firstOrNull { it.name == name }?.let(settings::setThemeMode) }
        if (config.lightFrom != null || config.lightTo != null) {
            settings.setLightMinutes(config.lightFrom ?: settings.lightFromMinutes(), config.lightTo ?: settings.lightToMinutes())
        }
        config.dockTransparency?.let(settings::setDockTransparencyPercent)
        config.dockSize?.let(settings::setDockSizePercent)
        config.widgets?.let { names ->
            WidgetPrefsRepository(context).setOrder(names.mapNotNull { name -> WidgetKind.entries.firstOrNull { it.name == name } })
        }
        config.coins?.let(CryptoPrefsRepository(context)::setSelected)
        val refresh = RefreshPrefsRepository(context)
        config.refreshWeather?.let(refresh::setWeatherMinutes)
        config.refreshCrypto?.let(refresh::setCryptoMinutes)
        config.refreshCalendar?.let(refresh::setCalendarMinutes)
        config.refreshSystem?.let(refresh::setSystemSeconds)
        config.refreshWatchNext?.let(refresh::setWatchNextMinutes)
    }
}
