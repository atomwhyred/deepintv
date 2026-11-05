package com.deepintv.launcher.data

import android.content.Context
import com.deepintv.launcher.R

/** What a remapped remote button should do. Vendor buttons (Netflix, Prime Video...) send
 * TV-specific key codes with no public constant, so there is no fixed enum of "the Netflix key" —
 * the person presses the real button and we capture whatever code it sends (see
 * [com.deepintv.launcher.service.DeepinAccessibilityService.learnModeCallback]). */
sealed class RemapAction {
    object Home : RemapAction()
    object ControlCenter : RemapAction()
    object AppDrawer : RemapAction()
    data class Launch(val packageName: String) : RemapAction()

    fun serialize(): String = when (this) {
        Home -> "home"
        ControlCenter -> "control_center"
        AppDrawer -> "drawer"
        is Launch -> "app:$packageName"
    }

    fun describe(context: Context, apps: List<AppInfo>): String = when (this) {
        Home -> context.getString(R.string.remap_home)
        ControlCenter -> context.getString(R.string.remap_control_center)
        AppDrawer -> context.getString(R.string.remap_all_apps)
        is Launch -> apps.firstOrNull { it.packageName == packageName }?.label ?: packageName
    }

    companion object {
        fun parse(raw: String): RemapAction? = when {
            raw == "home" -> Home
            raw == "control_center" -> ControlCenter
            raw == "drawer" -> AppDrawer
            raw.startsWith("app:") -> Launch(raw.removePrefix("app:"))
            else -> null
        }
    }
}

/** Persists which raw key codes open an app or a launcher overlay instead of their stock
 * behaviour — the "remapeo de teclas" the person asked for. */
class KeyRemapRepository(context: Context) {
    private val prefs = context.getSharedPreferences("deepintv_keymap", Context.MODE_PRIVATE)

    fun all(): Map<Int, RemapAction> =
        prefs.all.mapNotNull { (key, value) ->
            val code = key.toIntOrNull() ?: return@mapNotNull null
            val action = (value as? String)?.let { RemapAction.parse(it) } ?: return@mapNotNull null
            code to action
        }.toMap()

    fun set(keyCode: Int, action: RemapAction) {
        prefs.edit().putString(keyCode.toString(), action.serialize()).apply()
    }

    fun remove(keyCode: Int) {
        prefs.edit().remove(keyCode.toString()).apply()
    }
}
