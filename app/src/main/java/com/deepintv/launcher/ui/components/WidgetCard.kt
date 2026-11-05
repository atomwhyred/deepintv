package com.deepintv.launcher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.deepintv.launcher.ui.theme.DeepinFocusRing
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.deepintv.launcher.ui.theme.DeepinBorder
import com.deepintv.launcher.ui.theme.DeepinSurface
import com.deepintv.launcher.ui.theme.DeepinTextPrimary

/** The frosted-glass widget card shared by Weather, VPN status, System monitor and Crypto: a
 * small title label, then whatever content the widget wants underneath. Focusable and clickable
 * — Down from the top pills needs somewhere to land, and each widget needs to be reachable and
 * editable on its own ("tocar cada widget para editarlos por individual"). */
@Composable
fun WidgetCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.width(208.dp).fillMaxHeight().focusGlow(isFocused, shape),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DeepinSurface,
            contentColor = DeepinTextPrimary,
            focusedContainerColor = DeepinSurface,
            focusedContentColor = DeepinTextPrimary,
        ),
        scale = noFocusScale(),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(1.dp, DeepinBorder), shape = shape),
            focusedBorder = Border(border = BorderStroke(2.dp, DeepinFocusRing), shape = shape),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = DeepinTextPrimary, fontSize = 13.sp)
            Column(modifier = Modifier.padding(top = 10.dp)) {
                content()
            }
        }
    }
}

/** A quiet line under a widget's content when what it shows is a leftover from before the
 * network went away. Same type size and colour as the other captions, no new styling. */
@Composable
fun StaleNote() {
    Text(
        text = androidx.compose.ui.res.stringResource(com.deepintv.launcher.R.string.stale_data),
        color = com.deepintv.launcher.ui.theme.DeepinTextSecondary,
        fontSize = 10.sp,
        modifier = Modifier.padding(top = 4.dp),
    )
}
