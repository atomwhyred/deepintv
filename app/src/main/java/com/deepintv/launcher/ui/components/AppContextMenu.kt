package com.deepintv.launcher.ui.components

import com.deepintv.launcher.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.deepintv.launcher.data.AppInfo
import com.deepintv.launcher.ui.theme.DeepinTextPrimary

/** The dock's long-press menu: add/remove a favourite, reorder it within the dock. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppContextMenu(
    app: AppInfo,
    isFavorite: Boolean,
    isInDock: Boolean,
    modifier: Modifier = Modifier,
    onToggleFavorite: () -> Unit,
    onMoveLeft: (() -> Unit)? = null,
    onMoveRight: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val firstRowFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstRowFocusRequester.requestFocus() }
    // The menu opens while OK is still held down. Its repeats and the release would land on the
    // first row and count as a click (adding/removing the favourite on their own), so OK/Enter is
    // swallowed until the key is released or a fresh press arrives.
    var armed by remember { mutableStateOf(false) }

    GlassSurface(modifier = modifier.width(240.dp)) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .onPreviewKeyEvent { event ->
                    val isSelectKey = event.key == Key.DirectionCenter || event.key == Key.Enter
                    if (!isSelectKey || armed) return@onPreviewKeyEvent false
                    when {
                        event.type == KeyEventType.KeyUp -> {
                            armed = true
                            true
                        }
                        event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0 -> {
                            armed = true
                            false
                        }
                        else -> true
                    }
                }
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusRestorer()
                .focusGroup(),
        ) {
            Text(
                text = app.label,
                color = DeepinTextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
            MenuRow(
                label = stringResource(if (isFavorite) R.string.ctx_remove_favorite else R.string.ctx_add_favorite),
                modifier = Modifier.focusRequester(firstRowFocusRequester),
            ) {
                onToggleFavorite()
                onDismiss()
            }
            if (isInDock && onMoveLeft != null) {
                MenuRow(stringResource(R.string.move_left)) {
                    onMoveLeft()
                }
            }
            if (isInDock && onMoveRight != null) {
                MenuRow(stringResource(R.string.move_right)) {
                    onMoveRight()
                }
            }
            MenuRow(stringResource(R.string.cancel), onClick = onDismiss)
        }
    }
}

@Composable
private fun MenuRow(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FocusSurface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) { isFocused ->
        Text(
            text = label,
            color = focusContent(isFocused, DeepinTextPrimary),
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
