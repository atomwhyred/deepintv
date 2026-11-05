package com.deepintv.launcher.data

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class CalendarEvent(
    val title: String,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val allDay: Boolean,
)

/** A small iCalendar reader for the "public feed" case (Google Calendar, Outlook, Nextcloud...).
 * It covers what such feeds actually contain: timed and all-day events, time zones, and the
 * common repeat rules (daily, weekly with days, monthly, yearly, with INTERVAL, COUNT, UNTIL and
 * EXDATE). Anything more exotic (ordinal weekdays such as "first Monday") repeats by its start
 * date instead of failing the whole feed. */
object IcsParser {
    private const val MAX_OCCURRENCES = 5000

    private class Raw(val name: String, val params: Map<String, String>, val value: String)

    private class Event(
        val uid: String?,
        val title: String,
        val start: ZonedDateTime,
        val end: ZonedDateTime,
        val allDay: Boolean,
        val rrule: Map<String, String>?,
        val exdates: Set<Instant>,
        val recurrenceId: Instant?,
    )

    /** Events that overlap [from, to), expanded and sorted by start. */
    fun parse(text: String, from: Instant, to: Instant, zone: ZoneId): List<CalendarEvent> {
        val events = mutableListOf<Event>()
        var current: MutableList<Raw>? = null
        for (line in unfold(text)) {
            when {
                line.equals("BEGIN:VEVENT", ignoreCase = true) -> current = mutableListOf()
                line.equals("END:VEVENT", ignoreCase = true) -> {
                    current?.let { props -> buildEvent(props, zone)?.let(events::add) }
                    current = null
                }
                else -> current?.let { props -> parseLine(line)?.let(props::add) }
            }
        }

        // A moved occurrence is its own VEVENT carrying RECURRENCE-ID: drop the original it replaces.
        val replaced = events.filter { it.recurrenceId != null }.map { it.uid to it.recurrenceId }.toSet()
        val result = mutableListOf<CalendarEvent>()
        for (event in events) {
            val duration = Duration.between(event.start, event.end)
            for (start in occurrences(event, to)) {
                if (event.recurrenceId == null && (event.uid to start.toInstant()) in replaced) continue
                if (start.toInstant() in event.exdates) continue
                val end = start.plus(duration)
                if (end.toInstant() <= from || start.toInstant() >= to) continue
                result += CalendarEvent(event.title, start, end, event.allDay)
            }
        }
        return result.sortedBy { it.start }
    }

    private fun unfold(text: String): List<String> {
        val lines = mutableListOf<String>()
        for (raw in text.split("\r\n", "\n")) {
            if ((raw.startsWith(" ") || raw.startsWith("\t")) && lines.isNotEmpty()) {
                lines[lines.lastIndex] = lines.last() + raw.substring(1)
            } else {
                lines += raw
            }
        }
        return lines.filter { it.isNotBlank() }
    }

    private fun parseLine(line: String): Raw? {
        val colon = line.indexOf(':')
        if (colon <= 0) return null
        val head = line.substring(0, colon).split(';')
        val params = head.drop(1).mapNotNull {
            val eq = it.indexOf('=')
            if (eq > 0) it.substring(0, eq).uppercase() to it.substring(eq + 1).trim('"') else null
        }.toMap()
        return Raw(head[0].uppercase(), params, line.substring(colon + 1))
    }

    private fun buildEvent(props: List<Raw>, zone: ZoneId): Event? {
        fun first(name: String) = props.firstOrNull { it.name == name }
        if (first("STATUS")?.value.equals("CANCELLED", ignoreCase = true)) return null
        val dtStart = first("DTSTART") ?: return null
        val (start, allDay) = parseDate(dtStart, zone) ?: return null
        val end = first("DTEND")?.let { parseDate(it, zone)?.first }
            ?: first("DURATION")?.let { runCatching { start.plus(Duration.parse(it.value)) }.getOrNull() }
            ?: if (allDay) start.plusDays(1) else start
        val rrule = first("RRULE")?.value?.split(';')?.mapNotNull {
            val eq = it.indexOf('=')
            if (eq > 0) it.substring(0, eq).uppercase() to it.substring(eq + 1) else null
        }?.toMap()
        val exdates = props.filter { it.name == "EXDATE" }.flatMap { raw ->
            raw.value.split(',').mapNotNull { parseDate(Raw(raw.name, raw.params, it), zone)?.first?.toInstant() }
        }.toSet()
        val recurrenceId = first("RECURRENCE-ID")?.let { parseDate(it, zone)?.first?.toInstant() }
        return Event(
            uid = first("UID")?.value,
            title = unescape(first("SUMMARY")?.value.orEmpty()).ifBlank { "—" },
            start = start,
            end = if (end.isBefore(start)) start else end,
            allDay = allDay,
            rrule = rrule,
            exdates = exdates,
            recurrenceId = recurrenceId,
        )
    }

    private val DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val DATE_ONLY = DateTimeFormatter.ofPattern("yyyyMMdd")

    private fun parseDate(raw: Raw, zone: ZoneId): Pair<ZonedDateTime, Boolean>? = runCatching {
        val value = raw.value.trim()
        when {
            raw.params["VALUE"] == "DATE" || (value.length == 8 && 'T' !in value) ->
                LocalDate.parse(value, DATE_ONLY).atStartOfDay(zone) to true
            value.endsWith("Z") ->
                LocalDateTime.parse(value.dropLast(1), DATE_TIME).atZone(ZoneOffset.UTC).withZoneSameInstant(zone) to false
            else -> {
                val tz = raw.params["TZID"]?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: zone
                LocalDateTime.parse(value, DATE_TIME).atZone(tz).withZoneSameInstant(zone) to false
            }
        }
    }.getOrNull()

    private fun unescape(text: String) = text
        .replace("\\n", " ").replace("\\N", " ")
        .replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")

    private val DAYS = mapOf(
        "MO" to DayOfWeek.MONDAY, "TU" to DayOfWeek.TUESDAY, "WE" to DayOfWeek.WEDNESDAY,
        "TH" to DayOfWeek.THURSDAY, "FR" to DayOfWeek.FRIDAY, "SA" to DayOfWeek.SATURDAY,
        "SU" to DayOfWeek.SUNDAY,
    )

    /** Every start of [event] before [limit], in order. */
    private fun occurrences(event: Event, limit: Instant): Sequence<ZonedDateTime> {
        val rule = event.rrule ?: return sequenceOf(event.start)
        val interval = (rule["INTERVAL"]?.toIntOrNull() ?: 1).coerceAtLeast(1)
        val count = rule["COUNT"]?.toIntOrNull()
        val until = rule["UNTIL"]?.let {
            parseDate(Raw("UNTIL", emptyMap(), it), event.start.zone)?.first?.toInstant()?.let { u ->
                // A date-only UNTIL is inclusive of that whole day.
                if (it.length == 8) u.plus(1, ChronoUnit.DAYS) else u
            }
        }
        val start = event.start
        val weekdays = rule["BYDAY"]?.split(',')?.mapNotNull { DAYS[it.takeLast(2)] }?.toSet().orEmpty()

        val candidates: Sequence<ZonedDateTime> = when (rule["FREQ"]) {
            "DAILY" -> generateSequence(start) { it.plusDays(interval.toLong()) }
            "WEEKLY" -> {
                val days = weekdays.ifEmpty { setOf(start.dayOfWeek) }
                val weekStart = start.minusDays((start.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
                generateSequence(weekStart) { it.plusWeeks(interval.toLong()) }.flatMap { week ->
                    days.sortedBy { it.value }.asSequence()
                        .map { week.plusDays((it.value - DayOfWeek.MONDAY.value).toLong()) }
                        .filter { !it.isBefore(start) }
                }
            }
            "MONTHLY" -> generateSequence(0L) { it + interval }.mapNotNull { months ->
                val base = start.toLocalDate().withDayOfMonth(1).plusMonths(months)
                val day = rule["BYMONTHDAY"]?.toIntOrNull() ?: start.dayOfMonth
                if (day > base.lengthOfMonth()) null else start.with(base.withDayOfMonth(day))
            }
            "YEARLY" -> generateSequence(0L) { it + interval }.mapNotNull { years ->
                runCatching { start.plusYears(years) }.getOrNull()?.takeIf { it.dayOfMonth == start.dayOfMonth }
            }
            else -> sequenceOf(start)
        }

        var taken = 0
        return candidates
            .take(MAX_OCCURRENCES)
            .takeWhile { it.toInstant() < limit && (until == null || it.toInstant() < until) }
            .takeWhile { count == null || ++taken <= count }
    }
}
