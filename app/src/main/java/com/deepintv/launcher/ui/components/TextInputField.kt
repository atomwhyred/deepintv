package com.deepintv.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import com.deepintv.launcher.ui.theme.DeepinFocusRing
import com.deepintv.launcher.ui.theme.DeepinWash
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary

/** A one-line text box for the remote. It behaves like any other row for D-pad focus; pressing OK
 * on it starts editing and opens the TV's on-screen keyboard. The real text field only takes
 * focus while editing: a text field that took focus on arrival would pop the keyboard up at once
 * and, on the TCL TV's keyboard, the following OK press types a stray "1" into it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Search,
    onSubmit: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val boxRequester = remember { FocusRequester() }
    val textRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var editing by remember { mutableStateOf(false) }
    val highlighted = isFocused || editing
    val shape = RoundedCornerShape(10.dp)
    // Focused or editing, the box is always light grey (#E6E1E5) whatever the theme, so the typed
    // text and cursor go black to stay readable; unfocused they follow the theme.
    val contentColor = if (highlighted) Color.Black else DeepinTextPrimary

    LaunchedEffect(editing) {
        if (editing) {
            runCatching { textRequester.requestFocus() }
            keyboard?.show()
        }
    }

    // Back on the TV's keyboard only hides it: the field would stay "editing" with the focus, so
    // the D-pad moved the cursor instead of the focus and Back never reached the screen behind.
    // Once the keyboard that this edit opened goes away, leave editing and give the focus back.
    val imeVisible = WindowInsets.isImeVisible
    var imeSeen by remember { mutableStateOf(false) }
    LaunchedEffect(editing, imeVisible) {
        when {
            !editing -> imeSeen = false
            imeVisible -> imeSeen = true
            imeSeen -> {
                imeSeen = false
                editing = false
                runCatching { boxRequester.requestFocus() }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(boxRequester)
            .background(if (highlighted) Color(0xFFE6E1E5) else DeepinWash, shape)
            .border(2.dp, if (highlighted) DeepinFocusRing else DeepinWash, shape)
            .clickable(interactionSource = interactionSource, indication = null) { editing = true }
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = contentColor, fontSize = 14.sp),
            cursorBrush = SolidColor(contentColor),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = {
                keyboard?.hide()
                editing = false
                runCatching { boxRequester.requestFocus() }
                onSubmit()
            }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(textRequester)
                .focusProperties { canFocus = editing }
                .onFocusChanged { if (!it.isFocused && editing) editing = false },
        )
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = if (highlighted) Color(0xFF5B606A) else DeepinTextSecondary,
                fontSize = 14.sp,
            )
        }
    }
}
