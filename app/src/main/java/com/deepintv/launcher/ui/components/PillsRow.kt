package com.deepintv.launcher.ui.components

import com.deepintv.launcher.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp

/** Top-right icon pills: Inputs, the control center, Widgets, and Power off. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PillsRow(
    onInputsClick: () -> Unit,
    onControlCenterClick: () -> Unit,
    onPowerClick: () -> Unit,
    onWidgetsClick: () -> Unit,
    modifier: Modifier = Modifier,
    inputsRequester: FocusRequester = remember { FocusRequester() },
    controlRequester: FocusRequester = remember { FocusRequester() },
    widgetsRequester: FocusRequester = remember { FocusRequester() },
) {
    Box(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.focusRestorer().focusGroup(),
        ) {
            IconPill(icon = Icons.Filled.Input, contentDescription = stringResource(R.string.pill_inputs), onClick = onInputsClick, modifier = Modifier.focusRequester(inputsRequester))
            // Control center: Wi-Fi, volume, power and Autocleanup all live in that one panel.
            IconPill(
                icon = Icons.Filled.Tune,
                contentDescription = stringResource(R.string.pill_control),
                modifier = Modifier.focusRequester(controlRequester),
                onClick = onControlCenterClick,
            )
            // The widget cards are read-only (no focusable content of their own), so a D-pad
            // path to edit them has to live somewhere always reachable — this toolbar, not
            // buried at the end of a scrolling row nothing else in it can navigate into.
            IconPill(icon = Icons.Filled.Widgets, contentDescription = stringResource(R.string.pill_widgets), onClick = onWidgetsClick, modifier = Modifier.focusRequester(widgetsRequester))
            IconPill(icon = Icons.Filled.PowerSettingsNew, contentDescription = stringResource(R.string.pill_power), onClick = onPowerClick, danger = true)
        }
    }
}
