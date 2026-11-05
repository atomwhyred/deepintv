package com.deepintv.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingSessionTest {
    private var now = 1_000_000L
    private fun session(ttl: Long = 300_000L, maxFailures: Int = 5) =
        PairingSession(nowMs = { now }, ttlMs = ttl, maxFailures = maxFailures)

    @Test
    fun `token is 128 bits of hex and differs between sessions`() {
        val a = session()
        assertEquals(32, a.token.length)
        assertTrue(a.token.all { it in "0123456789abcdef" })
        assertNotEquals(a.token, session().token)
    }

    @Test
    fun `the token can be redeemed only once`() {
        val s = session()
        val id = s.redeem(s.token, "192.168.1.20")
        assertNotNull(id)
        assertNull(s.redeem(s.token, "192.168.1.20"))
        assertNull(s.redeem(s.token, "192.168.1.21"))
    }

    @Test
    fun `a wrong token gives nothing`() {
        val s = session()
        assertNull(s.redeem("nope", "192.168.1.20"))
        assertNull(s.redeem(null, "192.168.1.20"))
        assertNotNull(s.redeem(s.token, "192.168.1.20"))
    }

    @Test
    fun `the session id works only from the phone that redeemed it`() {
        val s = session()
        val id = s.redeem(s.token, "192.168.1.20")
        assertTrue(s.isAuthorized(id, "192.168.1.20"))
        assertFalse(s.isAuthorized(id, "192.168.1.99"))
        assertFalse(s.isAuthorized("other", "192.168.1.20"))
        assertFalse(s.isAuthorized(null, "192.168.1.20"))
    }

    @Test
    fun `an unscanned code expires`() {
        val s = session(ttl = 1_000)
        now += 1_001
        assertTrue(s.isExpired())
        assertNull(s.redeem(s.token, "192.168.1.20"))
    }

    @Test
    fun `a session expires after the ttl`() {
        val s = session(ttl = 10_000)
        now += 5_000
        val id = s.redeem(s.token, "192.168.1.20")
        now += 9_000
        assertTrue(s.isAuthorized(id, "192.168.1.20"))
        now += 2_000
        assertFalse(s.isAuthorized(id, "192.168.1.20"))
    }

    @Test
    fun `too many wrong guesses lock it for good`() {
        val s = session(maxFailures = 3)
        repeat(3) { s.redeem("guess$it", "192.168.1.66") }
        assertTrue(s.isLocked())
        assertNull(s.redeem(s.token, "192.168.1.20"))
    }

    @Test
    fun `remaining time counts down and restarts when the code is redeemed`() {
        val s = session(ttl = 300_000L)
        now += 100_000L
        assertEquals(200_000L, s.remainingMs())
        s.redeem(s.token, "192.168.1.20")
        assertEquals(300_000L, s.remainingMs())
        now += 400_000L
        assertEquals(0L, s.remainingMs())
    }
}
