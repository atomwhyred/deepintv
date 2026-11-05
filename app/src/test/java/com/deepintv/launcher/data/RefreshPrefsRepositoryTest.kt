package com.deepintv.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshPrefsRepositoryTest {
    private val options = RefreshPrefsRepository.WEATHER_OPTIONS

    @Test
    fun `next steps to the following option`() {
        assertEquals(30, RefreshPrefsRepository.next(options, 15))
        assertEquals(60, RefreshPrefsRepository.next(options, 30))
    }

    @Test
    fun `next wraps from the last option to the first`() {
        assertEquals(options.first(), RefreshPrefsRepository.next(options, options.last()))
    }

    @Test
    fun `every default is one of its own options`() {
        assert(RefreshPrefsRepository.DEFAULT_WEATHER in RefreshPrefsRepository.WEATHER_OPTIONS)
        assert(RefreshPrefsRepository.DEFAULT_CRYPTO in RefreshPrefsRepository.CRYPTO_OPTIONS)
        assert(RefreshPrefsRepository.DEFAULT_CALENDAR in RefreshPrefsRepository.CALENDAR_OPTIONS)
        assert(RefreshPrefsRepository.DEFAULT_SYSTEM in RefreshPrefsRepository.SYSTEM_OPTIONS)
        assert(RefreshPrefsRepository.DEFAULT_WATCH_NEXT in RefreshPrefsRepository.WATCH_NEXT_OPTIONS)
    }
}
