package com.deepintv.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

class HttpLimitsTest {
    @Test
    fun `reads text up to the limit, multi-byte characters included`() {
        val text = "Año ñ €".repeat(1000)
        val bytes = text.toByteArray(Charsets.UTF_8)
        assertEquals(text, ByteArrayInputStream(bytes).readTextCapped(bytes.size))
    }

    @Test
    fun `gives up past the limit`() {
        val bytes = ByteArray(MAX_API_BYTES + 1) { 'a'.code.toByte() }
        assertThrows(IOException::class.java) { ByteArrayInputStream(bytes).readTextCapped(MAX_API_BYTES) }
    }
}
