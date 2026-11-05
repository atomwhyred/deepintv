package com.deepintv.launcher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.tv.material3.Icon
import com.deepintv.launcher.ui.theme.DeepinRed
import com.deepintv.launcher.ui.theme.DeepinOnFocus
import com.deepintv.launcher.ui.theme.DeepinFocusRing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.deepintv.launcher.ui.theme.DeepinTextPrimary

/** A small frosted-glass pill button: Inputs, Wi-Fi, Settings, Control center. */
@Composable
fun Pill(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(50)

    Surface(
        onClick = onClick,
        modifier = modifier.focusGlow(isFocused, shape),
        interactionSource = interactionSource,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        scale = noFocusScale(),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = BorderStroke(2.dp, DeepinFocusRing),
                shape = shape,
            ),
        ),
    ) {
        Text(
            text = label,
            color = if (isFocused) DeepinOnFocus else DeepinTextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
        )
    }
}

/** The same pill as an icon in a circle: the label becomes the content description. [danger]
 * tints the icon red while focused (power off). */
@Composable
fun IconPill(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = CircleShape

    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp).focusGlow(isFocused, shape),
        interactionSource = interactionSource,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        scale = noFocusScale(),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = BorderStroke(2.dp, DeepinFocusRing),
                shape = shape,
            ),
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = when {
                    isFocused && danger -> DeepinRed
                    isFocused -> DeepinOnFocus
                    else -> DeepinTextPrimary
                },
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
