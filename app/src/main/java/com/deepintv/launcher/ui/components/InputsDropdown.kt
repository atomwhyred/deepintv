package com.deepintv.launcher.ui.components

import com.deepintv.launcher.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.SettingsInputComposite
import androidx.compose.material.icons.filled.SettingsInputHdmi
import androidx.compose.material.icons.filled.Input
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.deepintv.launcher.data.InputKind
import com.deepintv.launcher.data.InputState
import com.deepintv.launcher.data.TvInputItem
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary

/** The compact glass dropdown under the Inputs pill: HDMI ports, AV and the tuner. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InputsDropdown(
    inputs: List<TvInputItem>,
    modifier: Modifier = Modifier,
    onSelect: (TvInputItem) -> Unit,
) {
    GlassSurface(modifier = modifier.width(260.dp)) {
        if (inputs.isEmpty()) {
            Text(
                text = stringResource(R.string.inputs_none),
                color = DeepinTextPrimary,
                fontSize = 14.sp,
                modifier = Modifier.padding(16.dp),
            )
            return@GlassSurface
        }
        val firstRowFocusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { firstRowFocusRequester.requestFocus() }
        LazyColumn(
            modifier = Modifier
                .padding(8.dp)
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusRestorer()
                .focusGroup(),
        ) {
            itemsIndexed(inputs, key = { _, item -> item.inputId }) { index, input ->
                FocusSurface(
                    onClick = { onSelect(input) },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    modifier = if (index == 0) Modifier.focusRequester(firstRowFocusRequester) else Modifier,
                ) { isFocused ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .width(216.dp),
                    ) {
                        Icon(
                            imageVector = iconFor(input.kind),
                            contentDescription = null,
                            tint = focusContent(isFocused, DeepinTextPrimary),
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = input.label,
                                color = focusContent(isFocused, DeepinTextPrimary),
                                fontSize = 15.sp,
                            )
                            input.state?.let {
                                Text(
                                    text = stringResource(stateLabel(it)),
                                    color = focusContent(isFocused, DeepinTextSecondary),
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun iconFor(kind: InputKind): ImageVector = when (kind) {
    InputKind.HDMI -> Icons.Filled.SettingsInputHdmi
    InputKind.ANALOG_COMPONENT -> Icons.Filled.SettingsInputComponent
    InputKind.ANALOG_COMPOSITE -> Icons.Filled.SettingsInputComposite
    InputKind.TUNER -> Icons.Filled.LiveTv
    InputKind.OTHER -> Icons.Filled.Input
}

private fun stateLabel(state: InputState): Int = when (state) {
    InputState.CONNECTED -> R.string.input_state_connected
    InputState.STANDBY -> R.string.input_state_standby
    InputState.DISCONNECTED -> R.string.input_state_disconnected
}
