package com.deepintv.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSearchTest {
    private val labels = listOf("YouTube", "Google Maps", "Gmail", "Netflix", "Mapas Fáciles", "Play Store", "Reloj")

    private fun search(query: String) = rankByQuery(labels, query) { it }

    @Test
    fun `blank query returns everything untouched`() {
        assertEquals(labels, search(""))
        assertEquals(labels, search("   "))
    }

    @Test
    fun `prefix beats word start beats initials`() {
        // Gmail: prefix. Google Maps: initials.
        assertEquals(listOf("Gmail", "Google Maps"), search("gm"))
        // Mapas Fáciles: prefix. Google Maps: word start.
        assertEquals(listOf("Mapas Fáciles", "Google Maps"), search("map"))
    }

    @Test
    fun `matches initials`() {
        // Play Store: initials. Google Maps only contains "ps" inside "Maps", so it comes last.
        assertEquals(listOf("Play Store", "Google Maps"), search("ps"))
    }

    @Test
    fun `matches camelCase words`() {
        assertEquals(listOf("YouTube"), search("tube"))
        assertEquals(listOf("YouTube"), search("yt"))
    }

    @Test
    fun `ignores case and accents`() {
        assertEquals(listOf("Mapas Fáciles"), search("FACIL"))
        assertEquals(listOf("Mapas Fáciles"), search("fácil"))
    }

    @Test
    fun `query with a space matches across words`() {
        assertEquals(listOf("Google Maps"), search("google m"))
    }

    @Test
    fun `substring is the last resort`() {
        assertEquals(listOf("Netflix"), search("tfl"))
    }

    @Test
    fun `no match returns nothing`() {
        assertEquals(emptyList<String>(), search("zzz"))
    }

    @Test
    fun `ties keep the input order`() {
        val tied = listOf("Beta", "Bravo", "Banco")
        assertEquals(tied, rankByQuery(tied, "b") { it })
    }
}
