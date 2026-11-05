package com.deepintv.launcher.ui.components

import com.deepintv.launcher.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.deepintv.launcher.data.AVAILABLE_COINS
import com.deepintv.launcher.data.WidgetKind
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary

/** Opens when a widget card itself is clicked: reorder it, remove it, and — for Crypto — pick
 * which coins it tracks. "Tocar cada widget para editarlos por individual." */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WidgetEditSheet(
    kind: WidgetKind,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    selectedCoinIds: List<String>,
    modifier: Modifier = Modifier,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRemove: () -> Unit,
    onToggleCoin: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
    extraContent: @Composable () -> Unit = {},
) {
    val firstRowFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstRowFocusRequester.requestFocus() }

    GlassSurface(modifier = modifier.width(340.dp).heightIn(max = 560.dp), cornerRadius = 20) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .verticalScroll(rememberScrollState())
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusRestorer()
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(kind.labelRes), color = DeepinTextPrimary, fontSize = 18.sp, modifier = Modifier.weight(1f))
                FocusSurface(onClick = onDismiss, shape = RoundedCornerShape(50)) { isFocused ->
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), tint = focusContent(isFocused, DeepinTextPrimary), modifier = Modifier.padding(6.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                EditRow(
                    icon = Icons.Filled.ChevronLeft,
                    label = stringResource(R.string.move_left),
                    enabled = canMoveLeft,
                    modifier = Modifier.weight(1f).focusRequester(firstRowFocusRequester),
                    onClick = onMoveLeft,
                )
                EditRow(
                    icon = Icons.Filled.ChevronRight,
                    label = stringResource(R.string.move_right),
                    enabled = canMoveRight,
                    modifier = Modifier.weight(1f),
                    onClick = onMoveRight,
                )
            }

            EditRow(icon = Icons.Filled.Close, label = stringResource(R.string.edit_remove_widget), enabled = true, onClick = onRemove)

            extraContent()

            if (kind == WidgetKind.CRYPTO) {
                Text(
                    text = stringResource(R.string.edit_coins),
                    color = DeepinTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Column(modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                    AVAILABLE_COINS.forEach { coin ->
                        val isEnabled = coin.id in selectedCoinIds
                        FocusSurface(
                            onClick = { onToggleCoin(coin.id, !isEnabled) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        ) { isFocused ->
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (isEnabled) Icons.Filled.Remove else Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = focusContent(isFocused, if (isEnabled) DeepinTextSecondary else DeepinTextPrimary),
                                    modifier = Modifier.padding(end = 10.dp),
                                )
                                Text(
                                    text = coin.symbol,
                                    color = focusContent(isFocused, if (isEnabled) DeepinTextPrimary else DeepinTextSecondary),
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FocusSurface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) { isFocused ->
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = focusContent(isFocused, if (enabled) DeepinTextPrimary else DeepinTextSecondary),
                modifier = Modifier.padding(end = 10.dp),
            )
            Text(
                text = label,
                color = focusContent(isFocused, if (enabled) DeepinTextPrimary else DeepinTextSecondary),
                fontSize = 13.sp,
            )
        }
    }
}
