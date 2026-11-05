package com.deepintv.launcher.ui.components

import com.deepintv.launcher.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.deepintv.launcher.data.AppInfo
import com.deepintv.launcher.data.UsageRepository
import com.deepintv.launcher.data.rankByQuery
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary

private enum class DrawerSort { ALPHABETICAL, FREQUENCY, RECENT }

/** The full app list (item 4): every launchable app, sorted alphabetically, by how often it's
 * opened or by how recently, and filterable with the search box. A grid like the Home one, just
 * not limited to favourites. [recentPackages] is newest first. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    usageRepository: UsageRepository,
    recentPackages: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    // False while the long-press context menu is open on top. FocusRequester.Cancel on `exit`
    // below doesn't just block directional D-pad moves — it also cancels an explicit
    // requestFocus() call made from outside this subtree (the context menu trying to take focus
    // for itself), so the trap has to come off while that menu wants it.
    focusEnabled: Boolean = true,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
) {
    var sort by remember { mutableStateOf(DrawerSort.ALPHABETICAL) }
    val firstTileFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstTileFocusRequester.requestFocus() }
    var query by remember { mutableStateOf("") }
    val sortedApps = remember(apps, sort, query, recentPackages) {
        val ordered = when (sort) {
            DrawerSort.ALPHABETICAL -> apps.sortedBy { it.label.lowercase() }
            DrawerSort.FREQUENCY -> apps.sortedByDescending { usageRepository.countFor(it.packageName) }
            // Launched apps first, newest first; the rest keep their alphabetical order after them.
            DrawerSort.RECENT -> {
                val rank = recentPackages.withIndex().associate { it.value to it.index }
                apps.sortedBy { it.label.lowercase() }.sortedBy { rank[it.packageName] ?: Int.MAX_VALUE }
            }
        }
        // With a query the best match comes first, whatever the chosen order (ties keep it).
        rankByQuery(ordered, query) { it.label }
    }

    GlassSurface(modifier = modifier.fillMaxSize(), cornerRadius = 24) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
        ) {
            Row(
                // The header and the grid each trap focus except towards the other one; a single
                // trap on the whole column would also block moving between them. Each must be a
                // focusGroup: `exit` is only consulted on groups, so without it a chip would let
                // focus jump out to the Home buttons behind the drawer.
                modifier = Modifier.fillMaxWidth().trapFocusExcept(FocusDirection.Down, focusEnabled).focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.drawer_title), color = DeepinTextPrimary, fontSize = 22.sp)
                Box(modifier = Modifier.weight(1f))
                TextInputField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(R.string.drawer_search_hint),
                    modifier = Modifier.width(300.dp),
                    // Done typing: put the focus on the first match so OK launches it.
                    onSubmit = { runCatching { firstTileFocusRequester.requestFocus() } },
                )
                SortChip("A-Z", sort == DrawerSort.ALPHABETICAL) { sort = DrawerSort.ALPHABETICAL }
                SortChip(stringResource(R.string.drawer_sort_frequency), sort == DrawerSort.FREQUENCY) { sort = DrawerSort.FREQUENCY }
                SortChip(stringResource(R.string.drawer_sort_recent), sort == DrawerSort.RECENT) { sort = DrawerSort.RECENT }
            }
            if (sortedApps.isEmpty()) {
                Text(
                    text = stringResource(R.string.drawer_search_no_results),
                    color = DeepinTextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 30.dp, start = 10.dp),
                )
                return@Column
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp)
                    .trapFocusExcept(FocusDirection.Up, focusEnabled)
                    .focusRestorer()
                    .focusGroup(),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(sortedApps, key = { _, app -> app.packageName }) { index, app ->
                    AppTile(
                        app = app,
                        modifier = if (index == 0) Modifier.focusRequester(firstTileFocusRequester) else Modifier,
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FocusSurface(onClick = onClick, shape = RoundedCornerShape(50)) { isFocused ->
        Text(
            text = label,
            color = focusContent(isFocused, if (selected) DeepinTextPrimary else DeepinTextSecondary),
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/** Keeps the D-pad inside this part of the drawer, letting focus leave only towards [open]. */
@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.trapFocusExcept(open: FocusDirection, enabled: Boolean): Modifier =
    if (!enabled) this
    else focusProperties { exit = { if (it == open) FocusRequester.Default else FocusRequester.Cancel } }
