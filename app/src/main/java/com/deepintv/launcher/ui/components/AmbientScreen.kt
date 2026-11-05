package com.deepintv.launcher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.deepintv.launcher.data.CoinQuote
import com.deepintv.launcher.data.CryptoPrefsRepository
import com.deepintv.launcher.data.CryptoRepository
import com.deepintv.launcher.data.RefreshPrefsRepository
import com.deepintv.launcher.data.SafeModeRepository
import com.deepintv.launcher.data.WeatherLocationRepository
import com.deepintv.launcher.data.WeatherNow
import com.deepintv.launcher.data.WeatherRepository
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/** The "Deepin Ambient Display" screensaver surface: a big clock that slowly drifts across the
 * screen (OLED burn-in protection) and dims further the longer it has been showing, with a line
 * of weather and crypto underneath. Hosted by [com.deepintv.launcher.service.DeepinDreamService];
 * dismissal on any key/touch is the system DreamService's default behaviour, not handled here. */
@Composable
fun AmbientScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    var weather by remember { mutableStateOf<WeatherNow?>(null) }
    var coins by remember { mutableStateOf<List<CoinQuote>?>(null) }
    var offsetX by remember { mutableStateOf(0.dp) }
    var offsetY by remember { mutableStateOf(0.dp) }
    val dimAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(30_000)
        }
    }
    // After repeated crashes the screensaver stays a plain clock (see SafeModeRepository).
    val safeMode = remember { SafeModeRepository(context).isActive() }
    LaunchedEffect(Unit) {
        if (safeMode) return@LaunchedEffect
        // The place and freshness the person chose in the weather widget.
        val location = WeatherLocationRepository(context).saved()
        val maxAge = RefreshPrefsRepository(context).load().weatherMinutes
        weather = withContext(Dispatchers.IO) { WeatherRepository(context).currentWeather(location, maxAge) }
    }
    LaunchedEffect(Unit) {
        if (safeMode) return@LaunchedEffect
        // The coins and freshness the person chose in the crypto widget, not a fixed pair.
        val ids = CryptoPrefsRepository(context).selectedIds()
        val maxAge = RefreshPrefsRepository(context).load().cryptoMinutes
        coins = withContext(Dispatchers.IO) { CryptoRepository(context).quotes(ids, maxAge) }
    }
    // Burn-in protection: a small random drift every couple of minutes.
    LaunchedEffect(Unit) {
        while (true) {
            delay(120_000)
            offsetX = Random.nextInt(-40, 40).dp
            offsetY = Random.nextInt(-40, 40).dp
        }
    }
    // Progressive dimming: fades in, then keeps darkening the longer the screensaver stays up.
    LaunchedEffect(Unit) {
        dimAlpha.animateTo(0.15f, animationSpec = tween(4000))
        delay(300_000)
        dimAlpha.animateTo(0.55f, animationSpec = tween(60_000))
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.offset(x = offsetX, y = offsetY),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = timeFormat.format(now),
                color = DeepinTextPrimary,
                fontSize = 120.sp,
                fontWeight = FontWeight.Light,
            )
            Text(
                text = dateFormat.format(now).replaceFirstChar { it.uppercase() },
                color = DeepinTextSecondary,
                fontSize = 22.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(top = 28.dp),
            ) {
                weather?.let { w ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WeatherIcon(code = w.code, size = 20.dp)
                        Text(
                            text = "  ${w.tempC}°  ${w.city}",
                            color = DeepinTextSecondary,
                            fontSize = 16.sp,
                        )
                    }
                }
                coins?.forEach { coin ->
                    Text(
                        text = "${coin.symbol} $${"%,.0f".format(coin.priceUsd)}",
                        color = DeepinTextSecondary,
                        fontSize = 16.sp,
                    )
                }
            }
        }
        // Read in the draw phase: the 60 s fade then redraws this layer instead of recomposing the screen.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Color.Black, alpha = dimAlpha.value) },
        )
    }
}
