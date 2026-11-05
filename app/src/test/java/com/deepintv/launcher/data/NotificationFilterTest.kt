package com.deepintv.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationFilterTest {
    private fun entry(
        key: String = "k",
        pkg: String = "com.app",
        title: String? = "Title",
        text: String? = "Body",
        time: Long = 1,
        ongoing: Boolean = false,
        summary: Boolean = false,
    ) = notificationEntry(key, pkg, "App", title, text, time, ongoing, summary, ownPackage = "com.deepintv.launcher")

    @Test
    fun `ordinary notification is kept`() {
        val e = entry()!!
        assertEquals("Title", e.title)
        assertEquals("Body", e.text)
        assertEquals("App", e.appLabel)
    }

    @Test
    fun `ongoing, summaries and our own are skipped`() {
        assertNull(entry(ongoing = true))
        assertNull(entry(summary = true))
        assertNull(entry(pkg = "com.deepintv.launcher"))
    }

    @Test
    fun `empty notifications are skipped but one with only a text is kept`() {
        assertNull(entry(title = null, text = null))
        assertNull(entry(title = "  ", text = ""))
        assertEquals("Body", entry(title = null)!!.text)
    }

    @Test
    fun `long text is cut and the label falls back to the package`() {
        val e = notificationEntry("k", "com.x", " ", "T".repeat(500), "B".repeat(500), 1, false, false, "own")!!
        assertEquals(80, e.title.length)
        assertEquals(120, e.text.length)
        assertEquals("com.x", e.appLabel)
    }

    @Test
    fun `newest first, same key replaces, limited`() {
        val a = entry(key = "a", time = 1)!!
        val b = entry(key = "b", time = 5)!!
        val aAgain = entry(key = "a", time = 9, text = "new")!!
        val list = withNotification(withNotification(emptyList(), a), b)
        assertEquals(listOf("b", "a"), list.map { it.key })
        val updated = withNotification(list, aAgain)
        assertEquals(listOf("a", "b"), updated.map { it.key })
        assertEquals("new", updated.first().text)
        assertEquals(1, withNotification(list, aAgain, max = 1).size)
    }

    @Test
    fun `removing a key leaves the rest`() {
        val list = withNotification(withNotification(emptyList(), entry(key = "a")!!), entry(key = "b", time = 2)!!)
        assertEquals(listOf("b"), withoutNotification(list, "a").map { it.key })
        assertEquals(2, withoutNotification(list, "zzz").size)
    }
}
