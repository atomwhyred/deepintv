package com.deepintv.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class IcsParserTest {
    private val utc: ZoneId = ZoneOffset.UTC
    private val from = Instant.parse("2026-11-01T00:00:00Z")
    private val to = Instant.parse("2026-12-01T00:00:00Z")

    private fun ics(vararg events: String) =
        "BEGIN:VCALENDAR\r\nVERSION:2.0\r\n" + events.joinToString("") { "BEGIN:VEVENT\r\n$it\r\nEND:VEVENT\r\n" } + "END:VCALENDAR\r\n"

    private fun parse(text: String) = IcsParser.parse(text, from, to, utc)

    @Test
    fun `reads a timed event`() {
        val events = parse(ics("UID:1\r\nSUMMARY:Reunión\r\nDTSTART:20261110T093000Z\r\nDTEND:20261110T103000Z"))
        assertEquals(1, events.size)
        assertEquals("Reunión", events[0].title)
        assertEquals(9, events[0].start.hour)
        assertEquals(false, events[0].allDay)
    }

    @Test
    fun `reads an all-day event`() {
        val events = parse(ics("UID:2\r\nSUMMARY:Festivo\r\nDTSTART;VALUE=DATE:20261112\r\nDTEND;VALUE=DATE:20261113"))
        assertEquals(1, events.size)
        assertTrue(events[0].allDay)
        assertEquals(12, events[0].start.dayOfMonth)
    }

    @Test
    fun `unfolds long lines`() {
        val events = parse(ics("UID:3\r\nSUMMARY:Una reunión muy\r\n  larga\r\nDTSTART:20261110T093000Z\r\nDTEND:20261110T103000Z"))
        assertEquals("Una reunión muy larga", events[0].title)
    }

    @Test
    fun `expands a weekly rule and honours COUNT`() {
        val events = parse(ics("UID:4\r\nSUMMARY:Semanal\r\nDTSTART:20261102T100000Z\r\nDTEND:20261102T110000Z\r\nRRULE:FREQ=WEEKLY;COUNT=3"))
        assertEquals(listOf(2, 9, 16), events.map { it.start.dayOfMonth })
    }

    @Test
    fun `skips an excluded date`() {
        val events = parse(
            ics("UID:5\r\nSUMMARY:Diario\r\nDTSTART:20261102T100000Z\r\nDTEND:20261102T110000Z\r\nRRULE:FREQ=DAILY;COUNT=3\r\nEXDATE:20261103T100000Z"),
        )
        assertEquals(listOf(2, 4), events.map { it.start.dayOfMonth })
    }

    @Test
    fun `stops a rule at UNTIL`() {
        val events = parse(ics("UID:6\r\nSUMMARY:Hasta\r\nDTSTART:20261102T100000Z\r\nDTEND:20261102T110000Z\r\nRRULE:FREQ=DAILY;UNTIL=20261104T235959Z"))
        assertEquals(listOf(2, 3, 4), events.map { it.start.dayOfMonth })
    }

    @Test
    fun `leaves out events outside the window`() {
        val events = parse(ics("UID:7\r\nSUMMARY:Lejos\r\nDTSTART:20270101T100000Z\r\nDTEND:20270101T110000Z"))
        assertTrue(events.isEmpty())
    }

    @Test
    fun `returns events sorted by start`() {
        val events = parse(
            ics(
                "UID:8\r\nSUMMARY:Tarde\r\nDTSTART:20261110T150000Z\r\nDTEND:20261110T160000Z",
                "UID:9\r\nSUMMARY:Pronto\r\nDTSTART:20261110T080000Z\r\nDTEND:20261110T090000Z",
            ),
        )
        assertEquals(listOf("Pronto", "Tarde"), events.map { it.title })
    }

    @Test
    fun `garbage yields no events instead of throwing`() {
        assertTrue(parse("this is not a calendar").isEmpty())
    }
}
