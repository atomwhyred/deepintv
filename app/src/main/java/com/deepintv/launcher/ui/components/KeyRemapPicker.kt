package com.deepintv.launcher.ui.components

import com.deepintv.launcher.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.focusable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.deepintv.launcher.data.AppInfo
import com.deepintv.launcher.data.KeyRemapRepository
import com.deepintv.launcher.data.RemapAction
import com.deepintv.launcher.service.DeepinAccessibilityService
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary

private enum class PickerStep { LIST, WAITING_FOR_KEY, CHOOSING_ACTION }

/** Lets the person remap a physical remote button to open the control center, the app drawer, or
 * launch a chosen app. Vendor buttons (a dedicated Netflix or Prime Video key) send TV-specific
 * codes with no public constant, so instead of a fixed list this captures whatever code the next
 * real button press sends — "WAITING_FOR_KEY" — then asks what it should do. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun KeyRemapPicker(
    apps: List<AppInfo>,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { KeyRemapRepository(context) }
    var mappings by remember { mutableStateOf(repository.all()) }
    var step by remember { mutableStateOf(PickerStep.LIST) }
    var pendingKeyCode by remember { mutableStateOf<Int?>(null) }
    val firstRowFocusRequester = remember { FocusRequester() }
    LaunchedEffect(step) { firstRowFocusRequester.requestFocus() }

    DisposableEffect(Unit) {
        onDispose { DeepinAccessibilityService.learnModeCallback = null }
    }

    fun refresh() {
        mappings = repository.all()
        DeepinAccessibilityService.instance?.refreshMappings()
    }

    fun assign(action: RemapAction) {
        val code = pendingKeyCode ?: return
        repository.set(code, action)
        refresh()
        pendingKeyCode = null
        step = PickerStep.LIST
    }

    GlassSurface(modifier = modifier.width(380.dp).heightIn(max = 560.dp), cornerRadius = 20) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusRestorer()
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.remap_title),
                    color = DeepinTextPrimary,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                )
                FocusSurface(onClick = onDismiss, shape = RoundedCornerShape(50)) { isFocused ->
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), tint = focusContent(isFocused, DeepinTextPrimary), modifier = Modifier.padding(6.dp))
                }
            }

            when (step) {
                PickerStep.LIST -> KeyRemapList(
                    mappings = mappings,
                    apps = apps,
                    firstRowFocusRequester = firstRowFocusRequester,
                    onRemove = { code -> repository.remove(code); refresh() },
                    onAddClick = { step = PickerStep.WAITING_FOR_KEY },
                )
                PickerStep.WAITING_FOR_KEY -> {
                    Text(
                        text = stringResource(R.string.remap_press_button),
                        color = DeepinTextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.focusRequester(firstRowFocusRequester).focusable(),
                    )
                    LaunchedEffect(Unit) {
                        DeepinAccessibilityService.learnModeCallback = { code ->
                            pendingKeyCode = code
                            step = PickerStep.CHOOSING_ACTION
                        }
                    }
                }
                PickerStep.CHOOSING_ACTION -> {
                    Text(text = stringResource(R.string.remap_what_to_do), color = DeepinTextPrimary, fontSize = 14.sp)
                    ActionRow(
                        label = stringResource(R.string.remap_control_center),
                        modifier = Modifier.focusRequester(firstRowFocusRequester),
                        onClick = { assign(RemapAction.ControlCenter) },
                    )
                    ActionRow(label = stringResource(R.string.remap_all_apps), onClick = { assign(RemapAction.AppDrawer) })
                    ActionRow(label = stringResource(R.string.remap_home), onClick = { assign(RemapAction.Home) })
                    Text(
                        text = stringResource(R.string.remap_or_app),
                        color = DeepinTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Column(modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                        apps.forEach { app ->
                            ActionRow(label = app.label, onClick = { assign(RemapAction.Launch(app.packageName)) })
                        }
                    }
                }
            }

            if (!DeepinAccessibilityService.isEnabled) {
                Text(
                    text = stringResource(R.string.remap_needs_accessibility),
                    color = DeepinTextSecondary,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun KeyRemapList(
    mappings: Map<Int, RemapAction>,
    apps: List<AppInfo>,
    firstRowFocusRequester: FocusRequester,
    onRemove: (Int) -> Unit,
    onAddClick: () -> Unit,
) {
    if (mappings.isEmpty()) {
        Text(text = stringResource(R.string.remap_none), color = DeepinTextSecondary, fontSize = 13.sp)
    }
    mappings.entries.forEachIndexed { index, (code, action) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (index == 0) Modifier.focusRequester(firstRowFocusRequester) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.remap_code, code), color = DeepinTextPrimary, fontSize = 14.sp)
                Text(text = action.describe(LocalContext.current, apps), color = DeepinTextSecondary, fontSize = 12.sp)
            }
            FocusSurface(onClick = { onRemove(code) }, shape = RoundedCornerShape(10.dp)) { isFocused ->
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove), tint = focusContent(isFocused, DeepinTextSecondary), modifier = Modifier.padding(8.dp))
            }
        }
    }
    FocusSurface(
        onClick = onAddClick,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (mappings.isEmpty()) Modifier.focusRequester(firstRowFocusRequester) else Modifier),
    ) { isFocused ->
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = focusContent(isFocused, DeepinTextPrimary), modifier = Modifier.padding(end = 10.dp))
            Text(text = stringResource(R.string.remap_add), color = focusContent(isFocused, DeepinTextPrimary), fontSize = 14.sp)
        }
    }
}

@Composable
private fun ActionRow(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FocusSurface(onClick = onClick, shape = RoundedCornerShape(10.dp), modifier = modifier.fillMaxWidth()) { isFocused ->
        Text(text = label, color = focusContent(isFocused, DeepinTextPrimary), fontSize = 13.sp, modifier = Modifier.padding(10.dp))
    }
}
