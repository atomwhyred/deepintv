package com.deepintv.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.deepintv.launcher.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.deepintv.launcher.data.CoinQuote
import com.deepintv.launcher.data.CryptoRepository
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

private val PriceUpGreen = Color(0xFF34C759)
private val PriceDownRed = Color(0xFFFF453A)

@Composable
fun CryptoWidget(selectedIds: List<String>, refreshMinutes: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var coins by remember { mutableStateOf<List<CoinQuote>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var stale by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIds, refreshMinutes) {
        val repo = CryptoRepository(context)
        pollWithBackoff(
            intervalMs = refreshMinutes * 60_000L,
            fetch = {
                withContext(Dispatchers.IO) {
                    val quotes = repo.quotes(selectedIds, refreshMinutes)
                    Polled(quotes, stale = quotes != null && repo.isStale(refreshMinutes))
                }
            },
            onResult = { result ->
                if (result.value == null && coins == null) failed = true else if (result.value != null) coins = result.value
                stale = result.stale
            },
        )
    }

    WidgetCard(title = stringResource(R.string.widget_crypto), onClick = onClick, modifier = modifier) {
        val quotes = coins
        when {
            quotes != null -> Column {
                quotes.forEach { coin -> CoinRow(coin) }
                if (stale) StaleNote()
            }
            failed -> Text(text = stringResource(R.string.crypto_no_data), color = DeepinTextSecondary, fontSize = 12.sp)
            else -> Text(text = stringResource(R.string.loading), color = DeepinTextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CoinRow(coin: CoinQuote) {
    val rising = coin.change24hPercent >= 0
    val trendColor = if (rising) PriceUpGreen else PriceDownRed

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = coin.symbol, color = DeepinTextPrimary, fontSize = 13.sp)
            Text(
                text = "$" + String.format(Locale.US, if (coin.priceUsd >= 1000) "%,.0f" else "%.2f", coin.priceUsd),
                color = DeepinTextSecondary,
                fontSize = 11.sp,
            )
        }
        Sparkline(
            points = coin.sparkline7d,
            color = trendColor,
            modifier = Modifier.width(54.dp).height(24.dp),
        )
        Text(
            text = String.format(Locale.US, "%+.1f%%", coin.change24hPercent),
            color = trendColor,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 6.dp).width(46.dp),
        )
    }
}

/** A 7-day price trend, drawn natively: a plain polyline over the min/max range, no chart
 * library — this is the whole point of the "vectorial sparkline" requirement. */
@Composable
private fun Sparkline(points: List<Float>, color: Color, modifier: Modifier = Modifier) {
    if (points.size < 2) return
    val minValue = points.min()
    val maxValue = points.max()
    val range = (maxValue - minValue).coerceAtLeast(0.0001f)

    Canvas(modifier = modifier) {
        val stepX = size.width / (points.size - 1)
        val path = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - minValue) / range) * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f, cap = StrokeCap.Round),
        )
    }
}
