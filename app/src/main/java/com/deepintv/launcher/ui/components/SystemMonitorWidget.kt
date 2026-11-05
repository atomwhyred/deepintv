package com.deepintv.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import com.deepintv.launcher.ui.theme.DeepinWash
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.Text
import com.deepintv.launcher.R
import com.deepintv.launcher.data.StorageStats
import com.deepintv.launcher.data.SystemStats
import com.deepintv.launcher.data.SystemStatsRepository
import com.deepintv.launcher.data.VpnApp
import com.deepintv.launcher.data.VpnStatusRepository
import com.deepintv.launcher.ui.theme.DeepinBlue
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val StorageAmber = Color(0xFFFFB84D)
private val VpnGreen = Color(0xFF34C759)

/** RAM, storage and VPN state in one card. The VPN state comes from the network (any VPN reads
 * as connected); the chosen VPN app only decides the icon and name shown. */
@Composable
fun SystemMonitorWidget(
    vpnApp: VpnApp?,
    refreshSeconds: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var loaded by remember { mutableStateOf(false) }
    var memory by remember { mutableStateOf<SystemStats?>(null) }
    var storage by remember { mutableStateOf<StorageStats?>(null) }
    var vpnConnected by remember { mutableStateOf(false) }

    LaunchedEffect(refreshSeconds) {
        val stats = SystemStatsRepository()
        val vpn = VpnStatusRepository(context)
        while (true) {
            memory = withContext(Dispatchers.IO) { stats.memoryStats() }
            storage = withContext(Dispatchers.IO) { stats.storageStats() }
            vpnConnected = withContext(Dispatchers.IO) { vpn.currentStatus().connected }
            loaded = true
            delay(refreshSeconds * 1_000L)
        }
    }

    WidgetCard(title = stringResource(R.string.widget_system), onClick = onClick, modifier = modifier) {
        if (!loaded) {
            Text(text = stringResource(R.string.checking), color = DeepinTextSecondary, fontSize = 12.sp)
            return@WidgetCard
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            memory?.takeIf { it.totalMemKb > 0 }?.let { mem ->
                UsageRow(
                    label = "RAM",
                    value = usedOfTotalGb(mem.usedMemKb * 1024, mem.totalMemKb * 1024),
                    percent = (100 * mem.usedMemKb / mem.totalMemKb).toInt(),
                    color = DeepinBlue,
                )
            }
            storage?.let { disk ->
                UsageRow(
                    label = stringResource(R.string.system_disk),
                    value = usedOfTotalGb(disk.usedBytes, disk.totalBytes),
                    percent = (100 * disk.usedBytes / disk.totalBytes).toInt(),
                    color = StorageAmber,
                )
            }
            VpnRow(app = vpnApp, connected = vpnConnected)
        }
    }
}

private fun usedOfTotalGb(used: Long, total: Long): String =
    "%.1f / %.1f GB".format(used / 1_073_741_824.0, total / 1_073_741_824.0)

@Composable
private fun VpnRow(app: VpnApp?, connected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        if (app != null) {
            Image(
                bitmap = rememberAppIcon(app.packageName, app.icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = if (app != null) 8.dp else 0.dp)) {
            Text(
                text = app?.label ?: stringResource(R.string.vpn_no_app),
                color = if (app != null) DeepinTextPrimary else DeepinTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (connected) VpnGreen else DeepinTextSecondary),
        )
        Text(
            text = stringResource(if (connected) R.string.vpn_connected else R.string.vpn_disconnected),
            color = DeepinTextPrimary,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}

@Composable
private fun UsageRow(label: String, value: String, percent: Int, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, color = DeepinTextSecondary, fontSize = 11.sp)
            Text(text = value, color = DeepinTextPrimary, fontSize = 11.sp)
        }
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(DeepinWash),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}
