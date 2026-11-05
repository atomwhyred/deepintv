package com.deepintv.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesOrderTest {
    private val order = listOf("a", "b", "c", "d")

    @Test
    fun `moves right and left`() {
        assertEquals(listOf("a", "c", "b", "d"), FavoritesRepository.moved(order, "b", 1))
        assertEquals(listOf("b", "a", "c", "d"), FavoritesRepository.moved(order, "b", -1))
    }

    @Test
    fun `stops at both ends`() {
        assertEquals(order, FavoritesRepository.moved(order, "a", -1))
        assertEquals(order, FavoritesRepository.moved(order, "d", 1))
        assertEquals(listOf("d", "a", "b", "c"), FavoritesRepository.moved(order, "d", -10))
    }

    @Test
    fun `unknown package leaves the order alone`() {
        assertEquals(order, FavoritesRepository.moved(order, "zzz", 1))
    }
}
