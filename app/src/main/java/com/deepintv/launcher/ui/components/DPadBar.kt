package com.deepintv.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import com.deepintv.launcher.ui.theme.DeepinWash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.deepintv.launcher.ui.theme.DeepinBlue
import com.deepintv.launcher.ui.theme.DeepinFocusRing

/** A D-pad-driven thin-track slider with a round thumb: Left/Right step the value by `step`
 * while focused. Used for volume, dock transparency and dock size — none of those have a touch
 * gesture to drive on a TV remote, so this reads DirectionLeft/Right directly instead of using a
 * touch-drag Slider. */
@Composable
fun DPadBar(
    value: Int,
    range: IntRange,
    modifier: Modifier = Modifier,
    step: Int = 1,
    onChange: (Int) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val fraction = ((value - range.first).toFloat() / (range.last - range.first)).coerceIn(0f, 1f)
    val trackColor = if (isFocused) DeepinFocusRing else DeepinBlue

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onChange((value - step).coerceAtLeast(range.first))
                        true
                    }
                    Key.DirectionRight -> {
                        onChange((value + step).coerceAtMost(range.last))
                        true
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // The thin track, full width.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(DeepinWash),
        )
        // The filled portion up to the thumb.
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(trackColor),
        )
        // The round thumb, positioned by padding rather than offset so it stays within bounds.
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(28.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .size(if (isFocused) 20.dp else 16.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}
