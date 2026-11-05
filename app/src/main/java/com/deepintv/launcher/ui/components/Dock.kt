package com.deepintv.launcher.ui.components

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.deepintv.launcher.data.AppInfo

/** The bottom dock ("dde-dock"): the user's favourite apps in a frosted-glass band. */
@Composable
fun Dock(
    favourites: List<AppInfo>,
    modifier: Modifier = Modifier,
    transparencyPercent: Int = 50,
    iconSizeDp: Int = 120,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
) {
    GlassSurface(modifier = modifier, cornerRadius = 24, transparencyPercent = transparencyPercent) {
        LazyRow(
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusGroup(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(favourites, key = { it.packageName }) { app ->
                AppTile(
                    app = app,
                    sizeDp = iconSizeDp,
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) },
                )
            }
        }
    }
}
