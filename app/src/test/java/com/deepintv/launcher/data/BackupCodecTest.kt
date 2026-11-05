package com.deepintv.launcher.data

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupCodecTest {
    /** Records what decode writes, in place of a real SharedPreferences.Editor. */
    private class RecordingEditor : SharedPreferences.Editor {
        val values = mutableMapOf<String, Any?>()
        override fun putString(key: String, value: String?) = apply { values[key] = value }
        override fun putStringSet(key: String, value: MutableSet<String>?) = apply { values[key] = value }
        override fun putInt(key: String, value: Int) = apply { values[key] = value }
        override fun putLong(key: String, value: Long) = apply { values[key] = value }
        override fun putFloat(key: String, value: Float) = apply { values[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { values[key] = value }
        override fun remove(key: String) = apply { values.remove(key) }
        override fun clear() = apply { values.clear() }
        override fun commit() = true
        override fun apply() = Unit
    }

    private fun roundTrip(value: Any): Any? {
        val editor = RecordingEditor()
        BackupRepository.decode(editor, "k", BackupRepository.encode(value)!!)
        return editor.values["k"]
    }

    @Test
    fun `each supported type comes back unchanged`() {
        assertEquals("hola", roundTrip("hola"))
        assertEquals(42, roundTrip(42))
        assertEquals(1_700_000_000_000L, roundTrip(1_700_000_000_000L))
        assertEquals(true, roundTrip(true))
        assertEquals(1.5f, roundTrip(1.5f))
        assertEquals(setOf("a", "b"), roundTrip(setOf("a", "b")))
    }

    @Test
    fun `an unsupported type is not encoded`() {
        assertNull(BackupRepository.encode(Any()))
    }

    @Test
    fun `an Int stays an Int rather than becoming a Long`() {
        assertEquals(Int::class, roundTrip(7)!!::class)
    }
}
