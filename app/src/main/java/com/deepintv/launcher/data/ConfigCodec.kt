package com.deepintv.launcher.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * The settings a phone may read and change through the pairing page. Every field is optional: a
 * missing one means "leave it as it is". Anything that isn't listed here (calendar feeds, which can
 * be secret URLs; key mappings; safe-mode state; the wallpaper) is not reachable from the phone.
 */
data class RemoteConfig(
    val theme: String? = null,
    val lightFrom: Int? = null,
    val lightTo: Int? = null,
    val dockTransparency: Int? = null,
    val dockSize: Int? = null,
    val widgets: List<String>? = null,
    val coins: List<String>? = null,
    val refreshWeather: Int? = null,
    val refreshCrypto: Int? = null,
    val refreshCalendar: Int? = null,
    val refreshSystem: Int? = null,
    val refreshWatchNext: Int? = null,
)

/** What the phone may pick from: widget and coin names the launcher actually knows. */
class ConfigRules(val widgetNames: Set<String>, val coinIds: Set<String>)

sealed interface ConfigParse {
    data class Ok(val config: RemoteConfig) : ConfigParse
    data class Invalid(val reason: String) : ConfigParse
}

/** Turns what the phone sends into a [RemoteConfig], or says why not. Pure and strict: a wrong
 * type, a value outside the allowed options or a malformed body rejects the whole thing, so
 * nothing is ever half applied. Numbers that merely fall out of range are pulled back in. */
object ConfigCodec {
    const val MAX_BODY_CHARS = 16_384

    fun parse(body: String, rules: ConfigRules): ConfigParse {
        if (body.length > MAX_BODY_CHARS) return ConfigParse.Invalid("too_large")
        val json = try {
            JSONObject(body)
        } catch (e: JSONException) {
            return ConfigParse.Invalid("malformed")
        }
        return try {
            val config = RemoteConfig(
                theme = json.optionalString("theme")?.also {
                    if (ThemeMode.entries.none { mode -> mode.name == it }) throw Bad("theme")
                },
                lightFrom = json.optionalInt("lightFrom")?.coerceIn(0, 1439),
                lightTo = json.optionalInt("lightTo")?.coerceIn(0, 1439),
                dockTransparency = json.optionalInt("dockTransparency")?.coerceIn(0, 100),
                dockSize = json.optionalInt("dockSize")?.coerceIn(LauncherSettingsRepository.MIN_SIZE, LauncherSettingsRepository.MAX_SIZE),
                widgets = json.optionalStringList("widgets")?.filter { it in rules.widgetNames }?.distinct(),
                coins = json.optionalStringList("coins")?.filter { it in rules.coinIds }?.distinct()?.also {
                    if (it.isEmpty()) throw Bad("coins")
                },
                refreshWeather = json.optionalChoice("refreshWeather", RefreshPrefsRepository.WEATHER_OPTIONS),
                refreshCrypto = json.optionalChoice("refreshCrypto", RefreshPrefsRepository.CRYPTO_OPTIONS),
                refreshCalendar = json.optionalChoice("refreshCalendar", RefreshPrefsRepository.CALENDAR_OPTIONS),
                refreshSystem = json.optionalChoice("refreshSystem", RefreshPrefsRepository.SYSTEM_OPTIONS),
                refreshWatchNext = json.optionalChoice("refreshWatchNext", RefreshPrefsRepository.WATCH_NEXT_OPTIONS),
            )
            if (config == RemoteConfig()) ConfigParse.Invalid("nothing") else ConfigParse.Ok(config)
        } catch (e: Bad) {
            ConfigParse.Invalid(e.field)
        }
    }

    /** The current settings as the JSON the page loads. */
    fun toJson(config: RemoteConfig): JSONObject = JSONObject().apply {
        config.theme?.let { put("theme", it) }
        config.lightFrom?.let { put("lightFrom", it) }
        config.lightTo?.let { put("lightTo", it) }
        config.dockTransparency?.let { put("dockTransparency", it) }
        config.dockSize?.let { put("dockSize", it) }
        config.widgets?.let { put("widgets", JSONArray(it)) }
        config.coins?.let { put("coins", JSONArray(it)) }
        config.refreshWeather?.let { put("refreshWeather", it) }
        config.refreshCrypto?.let { put("refreshCrypto", it) }
        config.refreshCalendar?.let { put("refreshCalendar", it) }
        config.refreshSystem?.let { put("refreshSystem", it) }
        config.refreshWatchNext?.let { put("refreshWatchNext", it) }
    }

    private class Bad(val field: String) : Exception(field)

    private fun JSONObject.optionalString(key: String): String? {
        if (!has(key)) return null
        return get(key) as? String ?: throw Bad(key)
    }

    private fun JSONObject.optionalInt(key: String): Int? {
        if (!has(key)) return null
        val value = get(key) as? Number ?: throw Bad(key)
        val d = value.toDouble()
        if (d != Math.floor(d) || d !in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble()) throw Bad(key)
        return value.toInt()
    }

    private fun JSONObject.optionalChoice(key: String, options: List<Int>): Int? =
        optionalInt(key)?.also { if (it !in options) throw Bad(key) }

    private fun JSONObject.optionalStringList(key: String): List<String>? {
        if (!has(key)) return null
        val array = get(key) as? JSONArray ?: throw Bad(key)
        if (array.length() > 64) throw Bad(key)
        return List(array.length()) { index -> array.get(index) as? String ?: throw Bad(key) }
    }
}
