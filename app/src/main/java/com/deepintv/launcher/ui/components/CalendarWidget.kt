package com.deepintv.launcher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.deepintv.launcher.R
import com.deepintv.launcher.data.CalendarEvent
import com.deepintv.launcher.data.CalendarRepository
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private const val SHOWN_EVENTS = 3

/** The next few events from the iCalendar feeds added in the widget's editor. [feedsVersion]
 * changes whenever the feeds do, to reload without waiting for the next refresh. */
@Composable
fun CalendarWidget(
    refreshMinutes: Int,
    feedsVersion: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var events by remember { mutableStateOf<List<CalendarEvent>?>(null) }
    var hasFeeds by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var stale by remember { mutableStateOf(false) }

    LaunchedEffect(refreshMinutes, feedsVersion) {
        val repo = CalendarRepository(context)
        pollWithBackoff(
            intervalMs = refreshMinutes * 60_000L,
            fetch = {
                hasFeeds = repo.feeds().isNotEmpty()
                withContext(Dispatchers.IO) {
                    val result = repo.upcoming(refreshMinutes)
                    Polled(result, stale = result != null && repo.isStale(refreshMinutes))
                }
            },
            onResult = { result ->
                failed = result.value == null && events == null
                if (result.value != null) events = result.value
                stale = result.stale
            },
        )
    }

    WidgetCard(title = stringResource(R.string.widget_calendar), onClick = onClick, modifier = modifier) {
        val upcoming = events
        when {
            !hasFeeds -> Text(text = stringResource(R.string.calendar_add_hint), color = DeepinTextSecondary, fontSize = 12.sp)
            upcoming != null && upcoming.isEmpty() -> Text(text = stringResource(R.string.calendar_no_events), color = DeepinTextSecondary, fontSize = 12.sp)
            upcoming != null -> Column {
                upcoming.take(SHOWN_EVENTS).forEach { event -> EventRow(event) }
                if (stale) StaleNote()
            }
            failed -> Text(text = stringResource(R.string.calendar_no_data), color = DeepinTextSecondary, fontSize = 12.sp)
            else -> Text(text = stringResource(R.string.loading), color = DeepinTextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent) {
    val today = LocalDate.now()
    val date = event.start.toLocalDate()
    val day = when (date) {
        today -> stringResource(R.string.calendar_today)
        today.plusDays(1) -> stringResource(R.string.calendar_tomorrow)
        else -> date.format(DateTimeFormatter.ofPattern("EEE d", Locale.getDefault())).replaceFirstChar { it.uppercase() }
    }
    val time = if (event.allDay) {
        stringResource(R.string.calendar_all_day)
    } else {
        event.start.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()))
    }
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(text = event.title, color = DeepinTextPrimary, fontSize = 13.sp, maxLines = 1)
        Text(text = "$day · $time", color = DeepinTextSecondary, fontSize = 11.sp)
    }
}
