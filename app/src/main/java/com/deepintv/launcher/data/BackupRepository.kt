package com.deepintv.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Saves and restores everything the person set up — dock favourites, widgets, wallpaper, key
 * remaps, weather place, calendar feeds, refresh intervals — as one JSON file. It lives in the
 * app's own folder on shared storage, so it needs no permission and can be copied off the TV
 * (`adb pull /sdcard/Android/data/com.deepintv.launcher/files/deepintv-backup.json`).
 * Caches are left out: they refill by themselves. */
class BackupRepository(private val context: Context) {

    fun file(): File = File(context.getExternalFilesDir(null) ?: context.filesDir, FILE_NAME)

    fun hasBackup(): Boolean = file().isFile

    /** True when the file was written. */
    fun export(): Boolean = runCatching {
        val root = JSONObject().put("version", VERSION)
        val all = JSONObject()
        for (name in PREFS) {
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val skipped = SKIPPED_KEYS[name].orEmpty()
            val values = JSONObject()
            for ((key, value) in prefs.all) {
                if (key in skipped || value == null) continue
                values.put(key, encode(value) ?: continue)
            }
            all.put(name, values)
        }
        file().writeText(root.put("prefs", all).toString(2))
        true
    }.getOrDefault(false)

    /** True when a valid backup was applied; the caller should recreate the UI afterwards. */
    fun restore(): Boolean = runCatching {
        val root = JSONObject(file().readText())
        if (root.optInt("version", 0) != VERSION) return false
        val all = root.getJSONObject("prefs")
        for (name in PREFS) {
            val values = all.optJSONObject(name) ?: continue
            val skipped = SKIPPED_KEYS[name].orEmpty()
            val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit()
            // Wipe only what a backup covers, so settings added since stay at their defaults.
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            prefs.all.keys.filter { it !in skipped }.forEach(editor::remove)
            values.keys().forEach { key -> decode(editor, key, values.getJSONObject(key)) }
            editor.apply()
        }
        true
    }.getOrDefault(false)

    companion object {
        /** One preference value as `{"t": type, "v": value}`; null for a type SharedPreferences
         * can't hold. */
        internal fun encode(value: Any): JSONObject? = when (value) {
            is String -> JSONObject().put("t", "s").put("v", value)
            is Int -> JSONObject().put("t", "i").put("v", value)
            is Long -> JSONObject().put("t", "l").put("v", value)
            is Boolean -> JSONObject().put("t", "b").put("v", value)
            is Float -> JSONObject().put("t", "f").put("v", value.toDouble())
            is Set<*> -> JSONObject().put("t", "set").put("v", JSONArray(value.map { it.toString() }))
            else -> null
        }

        internal fun decode(editor: android.content.SharedPreferences.Editor, key: String, item: JSONObject) {
            when (item.getString("t")) {
                "s" -> editor.putString(key, item.getString("v"))
                "i" -> editor.putInt(key, item.getInt("v"))
                "l" -> editor.putLong(key, item.getLong("v"))
                "b" -> editor.putBoolean(key, item.getBoolean("v"))
                "f" -> editor.putFloat(key, item.getDouble("v").toFloat())
                "set" -> {
                    val array = item.getJSONArray("v")
                    editor.putStringSet(key, (0 until array.length()).map { array.getString(it) }.toSet())
                }
            }
        }

        const val FILE_NAME = "deepintv-backup.json"
        private const val VERSION = 1

        private val PREFS = listOf(
            "deepintv_favorites",
            "deepintv_launcher_settings",
            "deepintv_widgets",
            "deepintv_crypto_prefs",
            "deepintv_keymap",
            "deepintv_wallpaper",
            "deepintv_weather",
            "deepintv_vpn",
            "deepintv_calendar",
            "deepintv_refresh",
            "deepintv_usage",
        )

        /** The weather forecast cache lives in the same file as the chosen place. */
        private val SKIPPED_KEYS = mapOf("deepintv_weather" to setOf("json", "fetched_at", "coords"))
    }
}
