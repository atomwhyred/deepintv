package com.deepintv.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.deepintv.launcher.R
import com.deepintv.launcher.service.ConfigServer
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.delay

/** "Configure from your phone": shows a QR that opens the settings page served by the TV itself.
 * The server runs only while this screen is open. A successful change calls [onApplied]. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PairingScreen(onDismiss: () -> Unit, onApplied: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var applied by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<ConfigServer.Info?>(null) }
    var noNetwork by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf(0L) }
    val closeFocus = remember { FocusRequester() }

    DisposableEffect(Unit) {
        val started = ConfigServer.start(context) { applied = true }
        info = started
        noNetwork = started == null
        onDispose { ConfigServer.stop() }
    }
    LaunchedEffect(Unit) { runCatching { closeFocus.requestFocus() } }
    LaunchedEffect(info) {
        val until = info?.expiresAtMs ?: return@LaunchedEffect
        while (true) {
            remainingSeconds = ((until - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
            delay(1000)
        }
    }
    LaunchedEffect(applied) {
        if (applied) {
            delay(1500)
            onApplied()
        }
    }

    GlassSurface(modifier = modifier.width(640.dp)) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(R.string.pair_title), color = DeepinTextPrimary, fontSize = 22.sp)
            val current = info
            when {
                noNetwork -> Text(text = stringResource(R.string.pair_no_network), color = DeepinTextSecondary, fontSize = 14.sp)
                applied -> Text(text = stringResource(R.string.pair_applied), color = DeepinTextPrimary, fontSize = 16.sp)
                current != null -> Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    // A QR is black on white whatever the theme: that is what scanners expect.
                    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).padding(14.dp)) {
                        QrCode(content = current.url, modifier = Modifier.size(220.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = stringResource(R.string.pair_scan), color = DeepinTextPrimary, fontSize = 14.sp)
                        Text(text = current.url, color = DeepinTextSecondary, fontSize = 11.sp)
                        Text(
                            text = if (remainingSeconds > 0) {
                                stringResource(R.string.pair_expires, "%d:%02d".format(remainingSeconds / 60, remainingSeconds % 60))
                            } else {
                                stringResource(R.string.pair_expired)
                            },
                            color = DeepinTextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
                else -> Text(text = stringResource(R.string.loading), color = DeepinTextSecondary, fontSize = 14.sp)
            }
            FocusSurface(onClick = onDismiss, shape = RoundedCornerShape(50), modifier = Modifier.focusRequester(closeFocus)) { isFocused ->
                Text(
                    text = stringResource(R.string.close),
                    color = focusContent(isFocused, DeepinTextPrimary),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** A QR code drawn straight from the encoder's module matrix: no bitmap is built. */
@Composable
private fun QrCode(content: String, modifier: Modifier = Modifier) {
    val matrix = remember(content) {
        runCatching {
            QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                0,
                0,
                mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
            )
        }.getOrNull()
    }
    Canvas(modifier = modifier) {
        val bits = matrix ?: return@Canvas
        val cell = size.minDimension / bits.width
        for (y in 0 until bits.height) {
            for (x in 0 until bits.width) {
                if (bits[x, y]) {
                    drawRect(Color.Black, topLeft = Offset(x * cell, y * cell), size = Size(cell + 0.5f, cell + 0.5f))
                }
            }
        }
    }
}
