package com.deepintv.launcher.ui.components

import com.deepintv.launcher.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.deepintv.launcher.data.WidgetKind
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary

/** Lets the person add, remove and reorder the Home widget cards — "quiero que los widgets sean
 * editables". One row per widget type, always listed in a fixed reference order so removed ones
 * stay easy to find again; enabled ones show their position among themselves. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WidgetPicker(
    enabled: List<WidgetKind>,
    modifier: Modifier = Modifier,
    onToggle: (WidgetKind, Boolean) -> Unit,
    onMove: (WidgetKind, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val firstRowFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstRowFocusRequester.requestFocus() }

    GlassSurface(modifier = modifier.width(340.dp), cornerRadius = 20) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusRestorer()
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.pill_widgets), color = DeepinTextPrimary, fontSize = 18.sp, modifier = Modifier.weight(1f))
                FocusSurface(onClick = onDismiss, shape = RoundedCornerShape(50)) { isFocused ->
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), tint = focusContent(isFocused, DeepinTextPrimary), modifier = Modifier.padding(6.dp))
                }
            }
            WidgetKind.entries.forEachIndexed { index, kind ->
                val isEnabled = kind in enabled
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (index == 0) Modifier.focusRequester(firstRowFocusRequester) else Modifier),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FocusSurface(
                        onClick = { onToggle(kind, !isEnabled) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    ) { isFocused ->
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (isEnabled) Icons.Filled.Remove else Icons.Filled.Add,
                                contentDescription = null,
                                tint = focusContent(isFocused, if (isEnabled) DeepinTextSecondary else DeepinTextPrimary),
                                modifier = Modifier.padding(end = 10.dp),
                            )
                            Text(
                                text = stringResource(kind.labelRes),
                                color = focusContent(isFocused, if (isEnabled) DeepinTextPrimary else DeepinTextSecondary),
                                fontSize = 14.sp,
                            )
                        }
                    }
                    if (isEnabled) {
                        FocusSurface(onClick = { onMove(kind, -1) }, shape = RoundedCornerShape(10.dp)) { isFocused ->
                            Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.move_left), tint = focusContent(isFocused, DeepinTextPrimary), modifier = Modifier.padding(8.dp))
                        }
                        FocusSurface(onClick = { onMove(kind, 1) }, shape = RoundedCornerShape(10.dp)) { isFocused ->
                            Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.move_right), tint = focusContent(isFocused, DeepinTextPrimary), modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}
