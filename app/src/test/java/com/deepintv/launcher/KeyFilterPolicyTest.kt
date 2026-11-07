package com.deepintv.launcher

import android.view.KeyEvent
import com.deepintv.launcher.service.KeyFilterPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyFilterPolicyTest {
    @Test fun offWhenNothingNeedsIt() = assertFalse(KeyFilterPolicy.shouldFilter(false, false, false))
    @Test fun onForMappings() = assertTrue(KeyFilterPolicy.shouldFilter(true, false, false))
    @Test fun onWhileLearning() = assertTrue(KeyFilterPolicy.shouldFilter(false, true, false))
    @Test fun onForVolumeRamp() = assertTrue(KeyFilterPolicy.shouldFilter(false, false, true))
    @Test fun systemKeys() {
        assertTrue(KeyFilterPolicy.isSystemKey(KeyEvent.KEYCODE_VOLUME_UP))
        assertTrue(KeyFilterPolicy.isSystemKey(KeyEvent.KEYCODE_VOICE_ASSIST))
        assertFalse(KeyFilterPolicy.isSystemKey(KeyEvent.KEYCODE_BUTTON_A))
    }
}
