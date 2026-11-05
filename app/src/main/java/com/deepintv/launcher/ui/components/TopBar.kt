package com.deepintv.launcher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Large thin clock and date, top-left, refreshed once a minute. No network, no polling of anything else. */
@Composable
fun TopBar(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(30_000)
        }
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }

    Column(modifier = modifier) {
        Text(
            text = timeFormat.format(now),
            color = DeepinTextPrimary,
            fontSize = 44.sp,
            fontWeight = FontWeight.Light,
        )
        Text(
            text = dateFormat.format(now).replaceFirstChar { it.uppercase() },
            color = DeepinTextSecondary,
            fontSize = 16.sp,
        )
    }
}
