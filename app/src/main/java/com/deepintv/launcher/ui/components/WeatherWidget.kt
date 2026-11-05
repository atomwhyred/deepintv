package com.deepintv.launcher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.deepintv.launcher.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.deepintv.launcher.data.WeatherLocation
import com.deepintv.launcher.data.WeatherNow
import com.deepintv.launcher.data.WeatherRepository
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun WeatherWidget(location: WeatherLocation?, refreshMinutes: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var weather by remember { mutableStateOf<WeatherNow?>(null) }
    var failed by remember { mutableStateOf(false) }
    var stale by remember { mutableStateOf(false) }

    LaunchedEffect(location, refreshMinutes) {
        failed = false
        val repo = WeatherRepository(context)
        pollWithBackoff(
            intervalMs = refreshMinutes * 60_000L,
            fetch = {
                withContext(Dispatchers.IO) {
                    val now = repo.currentWeather(location, refreshMinutes)
                    Polled(now, stale = now != null && repo.isStale(refreshMinutes))
                }
            },
            onResult = { result ->
                if (result.value != null) weather = result.value else if (weather == null) failed = true
                stale = result.stale
            },
        )
    }

    WidgetCard(title = stringResource(R.string.widget_weather), onClick = onClick, modifier = modifier) {
        val current = weather
        when {
            current != null -> Column {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    WeatherIcon(code = current.code, size = 30.dp)
                    Text(
                        text = "  ${current.tempC}°",
                        color = DeepinTextPrimary,
                        fontSize = 28.sp,
                    )
                }
                Text(text = current.city, color = DeepinTextSecondary, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    current.days.take(4).forEach { day ->
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text(text = day.weekday.take(2), color = DeepinTextSecondary, fontSize = 10.sp)
                            WeatherIcon(code = day.code, size = 16.dp)
                            Text(text = "${day.maxC}°", color = DeepinTextPrimary, fontSize = 11.sp)
                        }
                    }
                }
                if (stale) StaleNote()
            }
            failed -> Text(text = stringResource(R.string.weather_no_data), color = DeepinTextSecondary, fontSize = 12.sp)
            else -> Text(text = stringResource(R.string.loading), color = DeepinTextSecondary, fontSize = 12.sp)
        }
    }
}
