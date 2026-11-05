package com.deepintv.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentsOrderTest {
    @Test
    fun `new package goes to the front`() {
        assertEquals(listOf("c", "a", "b"), RecentsRepository.pushed(listOf("a", "b"), "c"))
    }

    @Test
    fun `relaunching moves it to the front without duplicating`() {
        assertEquals(listOf("b", "a", "c"), RecentsRepository.pushed(listOf("a", "b", "c"), "b"))
    }

    @Test
    fun `list is trimmed to the maximum`() {
        assertEquals(listOf("d", "a", "b"), RecentsRepository.pushed(listOf("a", "b", "c"), "d", max = 3))
    }

    @Test
    fun `empty list`() {
        assertEquals(listOf("a"), RecentsRepository.pushed(emptyList(), "a"))
    }
}
