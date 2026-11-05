package com.deepintv.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemapActionTest {
    @Test
    fun `every action survives serialize then parse`() {
        val actions = listOf(RemapAction.Home, RemapAction.ControlCenter, RemapAction.AppDrawer, RemapAction.Launch("com.netflix.ninja"))
        actions.forEach { assertEquals(it, RemapAction.parse(it.serialize())) }
    }

    @Test
    fun `unknown text is rejected`() {
        assertNull(RemapAction.parse("reboot"))
        assertNull(RemapAction.parse(""))
    }

    @Test
    fun `launch keeps a package name that contains colons after the prefix`() {
        assertEquals(RemapAction.Launch("a:b"), RemapAction.parse("app:a:b"))
    }
}
