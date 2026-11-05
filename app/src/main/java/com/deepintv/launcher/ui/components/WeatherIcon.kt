package com.deepintv.launcher.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Grain
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.Icon
import com.deepintv.launcher.ui.theme.DeepinTextPrimary

/** WMO weather codes (Open-Meteo), one vector icon each. These replace emojis, which some TVs'
 * system fonts (the TCL G10 among them) draw as empty boxes. */
fun weatherIconFor(code: Int): ImageVector = when (code) {
    0 -> Icons.Filled.WbSunny
    1, 2 -> Icons.Filled.WbCloudy
    3 -> Icons.Filled.Cloud
    45, 48 -> Icons.Filled.Grain
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Filled.WaterDrop
    71, 73, 75, 77, 85, 86 -> Icons.Filled.AcUnit
    95, 96, 99 -> Icons.Filled.Thunderstorm
    else -> Icons.Filled.Thermostat
}

@Composable
fun WeatherIcon(code: Int, size: Dp, modifier: Modifier = Modifier) {
    Icon(
        imageVector = weatherIconFor(code),
        contentDescription = null,
        tint = DeepinTextPrimary,
        modifier = modifier.size(size),
    )
}
