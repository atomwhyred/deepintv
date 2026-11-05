package com.deepintv.launcher.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StalenessTest {
    private val minute = 60_000L
    private val now = 1_000_000_000L

    @Test
    fun `never fetched counts as old`() {
        assertTrue(isOlderThan(0, 60, now))
    }

    @Test
    fun `within the interval is fresh`() {
        assertFalse(isOlderThan(now - 30 * minute, 60, now))
    }

    @Test
    fun `a little past the interval is still tolerated`() {
        assertFalse(isOlderThan(now - 61 * minute, 60, now))
    }

    @Test
    fun `well past the interval is old`() {
        assertTrue(isOlderThan(now - 70 * minute, 60, now))
    }
}
