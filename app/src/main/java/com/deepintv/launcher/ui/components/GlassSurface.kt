package com.deepintv.launcher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.deepintv.launcher.ui.theme.DeepinBorder
import com.deepintv.launcher.ui.theme.DeepinSurface
import androidx.compose.foundation.background
import androidx.compose.foundation.border

/**
 * A translucent "frosted glass" container in the Deepin style: a flat semi-transparent tint plus
 * a thin border, not a real-time blur. A Gaussian blur behind every tile is too heavy for a 1 GB
 * TV, so the glass look here is approximated with color alone.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 16,
    /** 0..100, overriding the default tint — the control center's "Dock transparency" slider. */
    transparencyPercent: Int? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val tint = if (transparencyPercent != null) {
        DeepinSurface.copy(alpha = transparencyPercent / 100f * 0.6f)
    } else {
        DeepinSurface
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(tint)
            .border(BorderStroke(1.dp, DeepinBorder), shape),
    ) {
        content()
    }
}
