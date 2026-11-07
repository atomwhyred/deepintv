package com.deepintv.launcher.service

import android.view.KeyEvent

/** When the accessibility service really needs to see every remote key. Filtering is costly on
 * some TV firmwares (it drops the auto-repeat of held keys, e.g. volume), so it is only on while
 * something depends on it. */
object KeyFilterPolicy {
    fun shouldFilter(hasMappings: Boolean, learning: Boolean, consumeVolume: Boolean): Boolean =
        hasMappings || learning || consumeVolume

    /** Keys the learn mode must not capture: they belong to the system (volume, voice). */
    fun isSystemKey(code: Int): Boolean = code in setOf(
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
        KeyEvent.KEYCODE_VOICE_ASSIST, KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_ASSIST,
    )
}
