package com.deepintv.launcher.data

import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TvInputKindTest {
    @Test
    fun `maps connector types to kinds`() {
        assertEquals(InputKind.HDMI, inputKindFor(TvInputInfo.TYPE_HDMI))
        assertEquals(InputKind.HDMI, inputKindFor(TvInputInfo.TYPE_DISPLAY_PORT))
        assertEquals(InputKind.ANALOG_COMPONENT, inputKindFor(TvInputInfo.TYPE_COMPONENT))
        assertEquals(InputKind.ANALOG_COMPOSITE, inputKindFor(TvInputInfo.TYPE_COMPOSITE))
        assertEquals(InputKind.TUNER, inputKindFor(TvInputInfo.TYPE_TUNER))
        assertEquals(InputKind.OTHER, inputKindFor(TvInputInfo.TYPE_OTHER))
    }

    @Test
    fun `maps input states`() {
        assertEquals(InputState.CONNECTED, inputStateFor(TvInputManager.INPUT_STATE_CONNECTED))
        assertEquals(InputState.STANDBY, inputStateFor(TvInputManager.INPUT_STATE_CONNECTED_STANDBY))
        assertEquals(InputState.DISCONNECTED, inputStateFor(TvInputManager.INPUT_STATE_DISCONNECTED))
        assertNull(inputStateFor(99))
    }
}
