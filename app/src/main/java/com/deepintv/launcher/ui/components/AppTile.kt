package com.deepintv.launcher.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import com.deepintv.launcher.ui.theme.DeepinFocusRing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.deepintv.launcher.data.AppInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Surface
import androidx.tv.material3.ClickableSurfaceDefaults

/** How long OK has to be held to open the context menu. Android's own long-press (~500 ms) fired
 * while the user was still just pressing to launch, so this is deliberately longer. */
private const val LONG_PRESS_MS = 900L

/**
 * One app or dock icon: glows on D-pad focus (no zoom, so nothing gets clipped). Focus
 * is remembered by the parent's focusRestorer(), not by this composable. Holding OK/Enter opens
 * the context menu (add/remove favourite, reorder the dock): there's no touch long-press on a TV
 * remote, so this times the DPAD_CENTER/ENTER key directly (see [LONG_PRESS_MS]).
 */
@Composable
fun AppTile(
    app: AppInfo,
    modifier: Modifier = Modifier,
    sizeDp: Int = 120,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    // Tracks a long press across its KeyDown...KeyUp pair so the matching key-up can be eaten
    // too — otherwise Surface's own click handling (which sees the same key, one layer further in)
    // fires right after, launching the app the instant the context menu opens.
    var longPressTriggered by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier
            .size(sizeDp.dp)
            .focusGlow(isFocused, RoundedCornerShape(16.dp))
            // onPreviewKeyEvent runs in the tunneling (parent-first) phase, ahead of Surface's own
            // onKeyEvent-based click handling further down the chain — the only way to actually
            // intercept DPAD_CENTER before it turns into a click.
            .onPreviewKeyEvent { event ->
                val isSelectKey = event.key == Key.DirectionCenter || event.key == Key.Enter
                if (!isSelectKey || onLongClick == null) return@onPreviewKeyEvent false
                when (event.type) {
                    KeyEventType.KeyDown -> {
                        val native = event.nativeKeyEvent
                        // A fresh press starts clean: the key-up of the previous long press goes to
                        // the menu that opened (focus left this tile), so it never reset the flag.
                        if (native.repeatCount == 0) longPressTriggered = false
                        // Repeats keep coming while the key is held; the first one past the
                        // threshold opens the menu.
                        if (!longPressTriggered && native.repeatCount > 0 &&
                            native.eventTime - native.downTime >= LONG_PRESS_MS
                        ) {
                            longPressTriggered = true
                            onLongClick()
                        }
                        longPressTriggered
                    }
                    KeyEventType.KeyUp -> {
                        if (longPressTriggered) {
                            longPressTriggered = false
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            },
        interactionSource = interactionSource,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF1B2030),
            focusedContainerColor = Color(0xFF1B2030),
            pressedContainerColor = Color(0xFF1B2030),
        ),
        scale = noFocusScale(),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = BorderStroke(2.dp, DeepinFocusRing),
                shape = RoundedCornerShape(16.dp),
            ),
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(10.dp).clip(RoundedCornerShape(12.dp))) {
            Image(
                bitmap = rememberAppIcon(app.packageName, app.icon),
                contentDescription = app.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
