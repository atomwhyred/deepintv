package com.deepintv.launcher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import com.deepintv.launcher.ui.theme.DeepinOnFocus
import com.deepintv.launcher.ui.theme.LocalDeviceTier
import com.deepintv.launcher.ui.theme.DeepinFocusRing
import androidx.compose.ui.composed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.deepintv.launcher.data.DeviceTier
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface

/** The white focus ring the top pills use, so every menu row reads the same way. */
@Composable
fun focusBorder(shape: Shape) = ClickableSurfaceDefaults.border(
    focusedBorder = Border(border = BorderStroke(2.dp, DeepinFocusRing), shape = shape),
)

/** The Surface's built-in 1.1x focus zoom is off: focus is shown by the ring and glow only. */
@Composable
fun noFocusScale() = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f)

/** Colour for text and icons on a row: the Surface's focused highlight is near-white, so
 * anything that stays light on it is unreadable. Dark while focused, [normal] otherwise. */
fun focusContent(isFocused: Boolean, normal: Color): Color = if (isFocused) DeepinOnFocus else normal

/** A clickable row/chip with the same focus treatment as [Pill] (white ring, dark content on the
 * light highlight). [content] receives whether it is focused so it can pick its colours with
 * [focusContent]. */
@Composable
fun FocusSurface(
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable (isFocused: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        scale = noFocusScale(),
        border = focusBorder(shape),
        interactionSource = interactionSource,
        modifier = modifier,
    ) {
        content(isFocused)
    }
}

/** Soft white glow around a focused item, in place of the old zoom: nothing changes size, so
 * neighbours and clipped lists stay put. Draws up to ~10dp outside the item, so the parent needs
 * that much padding. */
fun Modifier.focusGlow(isFocused: Boolean, shape: Shape): Modifier = composed {
    val strength by animateFloatAsState(if (isFocused) 1f else 0f, label = "focusGlow")
    val glowColor = DeepinFocusRing
    // On a light tier the same 10dp halo is built from half as many rings, twice as thick, each
    // at the mean alpha of the pair it replaces: same look, half the draw calls per frame.
    val step = if (LocalDeviceTier.current == DeviceTier.LIGHT) 2 else 1
    drawBehind {
        if (strength <= 0.01f) return@drawBehind
        val corner = if (shape is RoundedCornerShape) shape.topStart.toPx(size, this) else size.minDimension / 2
        val layers = 8
        for (i in 1..layers step step) {
            val grow = (i + (step - 1) / 2f) * 1.25.dp.toPx()
            val alpha = 0.22f * strength * (1f - (i - 1 + (step - 1) / 2f) / layers.toFloat())
            drawRoundRect(
                color = glowColor.copy(alpha = alpha),
                topLeft = Offset(-grow, -grow),
                size = Size(size.width + grow * 2, size.height + grow * 2),
                cornerRadius = CornerRadius(corner + grow),
                style = Stroke(width = step * 1.25.dp.toPx()),
            )
        }
    }
}
