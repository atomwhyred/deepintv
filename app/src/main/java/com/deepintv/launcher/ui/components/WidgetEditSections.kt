package com.deepintv.launcher.ui.components

import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import android.Manifest
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.deepintv.launcher.R
import com.deepintv.launcher.data.CalendarRepository
import com.deepintv.launcher.data.CityResult
import com.deepintv.launcher.data.CitySearchRepository
import com.deepintv.launcher.data.VpnApp
import com.deepintv.launcher.data.WeatherLocation
import com.deepintv.launcher.data.WeatherLocationRepository
import android.provider.Settings
import com.deepintv.launcher.data.WatchNextItem
import com.deepintv.launcher.service.DeepinNotificationService
import com.deepintv.launcher.data.WatchNextRepository
import com.deepintv.launcher.data.WeatherRepository
import com.deepintv.launcher.ui.safeStartActivity
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val RowShape = RoundedCornerShape(10.dp)

/** A row that is removed or replaced while it has focus takes the focus with it, and the sheet's
 * focus trap cannot hold what it no longer contains: so after such a change, hand focus to a row
 * that stays. */
private fun CoroutineScope.refocus(target: FocusRequester) {
    launch {
        withFrameNanos { }
        runCatching { target.requestFocus() }
    }
}

/** Weather's part of the edit sheet: current location, a typed or spoken place, or the default. */
@Composable
fun WeatherEditSection(
    location: WeatherLocation?,
    onLocationChange: (WeatherLocation?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { WeatherLocationRepository(context) }
    val citySearch = remember { CitySearchRepository() }
    var status by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CityResult>>(emptyList()) }
    val anchor = remember { FocusRequester() }

    fun choose(found: WeatherLocation) {
        repository.save(found)
        onLocationChange(found)
        results = emptyList()
        query = ""
        status = null
        scope.refocus(anchor)
    }

    fun detect() {
        status = context.getString(R.string.weather_locating)
        scope.launch {
            val found = withContext(Dispatchers.IO) { repository.detectCurrent() }
            if (found == null) status = context.getString(R.string.weather_location_failed) else choose(found)
        }
    }

    fun search(text: String) {
        if (text.isBlank()) return
        status = context.getString(R.string.weather_searching)
        scope.launch {
            val found = withContext(Dispatchers.IO) { citySearch.search(text) }
            results = found
            status = if (found.isEmpty()) context.getString(R.string.weather_no_results) else null
        }
    }

    // The permission only improves the result: with or without it, detect() still falls back to
    // an approximate place from the IP, so a refusal is not an error.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { detect() }

    val voiceIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.weather_voice_prompt))
    }
    // Google TV offers two recognisers (the voice assistant and the text-to-speech app); asking
    // the person to pick one every time would be a poor way to start talking, so choose the
    // assistant when it is the only candidate once the speech-synthesis app is set aside.
    val voiceAvailable = remember {
        val handlers = context.packageManager.queryIntentActivities(voiceIntent, 0).map { it.activityInfo.packageName }
        handlers.singleOrNull { !it.endsWith(".tts") }?.let { voiceIntent.setPackage(it) }
        handlers.isNotEmpty()
    }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            query = spoken
            search(spoken)
        }
    }

    Text(text = stringResource(R.string.weather_location), color = DeepinTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    Text(
        text = when {
            location == null -> stringResource(R.string.weather_default_place, WeatherRepository.DEFAULT_CITY)
            location.approximate -> stringResource(R.string.weather_approx_place, location.label)
            else -> location.label
        },
        color = DeepinTextPrimary,
        fontSize = 14.sp,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextInputField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.weather_search_hint),
            modifier = Modifier.weight(1f),
            onSubmit = { search(query) },
        )
        FocusSurface(onClick = { search(query) }, shape = RowShape) { isFocused ->
            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search), tint = focusContent(isFocused, DeepinTextPrimary), modifier = Modifier.padding(10.dp))
        }
        if (voiceAvailable) {
            FocusSurface(onClick = { voiceLauncher.launch(voiceIntent) }, shape = RowShape) { isFocused ->
                Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.weather_search_voice), tint = focusContent(isFocused, DeepinTextPrimary), modifier = Modifier.padding(10.dp))
            }
        }
    }
    results.forEach { city ->
        FocusSurface(onClick = { choose(city.location) }, shape = RowShape, modifier = Modifier.fillMaxWidth()) { isFocused ->
            Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                Text(text = city.location.label, color = focusContent(isFocused, DeepinTextPrimary), fontSize = 14.sp)
                if (city.detail.isNotBlank()) {
                    Text(text = city.detail, color = focusContent(isFocused, DeepinTextSecondary), fontSize = 11.sp)
                }
            }
        }
    }

    MenuAction(
        icon = Icons.Filled.LocationOn,
        label = stringResource(R.string.weather_use_current),
        modifier = Modifier.focusRequester(anchor),
        onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
    )
    if (location != null) {
        MenuAction(
            icon = Icons.Filled.Refresh,
            label = stringResource(R.string.weather_back_to_default, WeatherRepository.DEFAULT_CITY),
            onClick = {
                repository.clear()
                onLocationChange(null)
                status = null
                scope.refocus(anchor)
            },
        )
    }
    status?.let { Text(text = it, color = DeepinTextSecondary, fontSize = 12.sp) }
}

/** System's VPN part: choose which installed VPN client the widget shows. */
@Composable
fun VpnEditSection(
    apps: List<VpnApp>,
    selectedPackage: String?,
    onSelect: (VpnApp) -> Unit,
    onOpen: (VpnApp) -> Unit,
) {
    Text(text = stringResource(R.string.vpn_app), color = DeepinTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    if (apps.isEmpty()) {
        Text(text = stringResource(R.string.vpn_none_installed), color = DeepinTextPrimary, fontSize = 13.sp)
        return
    }
    apps.forEach { app ->
        val selected = app.packageName == selectedPackage
        FocusSurface(onClick = { onSelect(app) }, shape = RowShape, modifier = Modifier.fillMaxWidth()) { isFocused ->
            Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    bitmap = rememberAppIcon(app.packageName, app.icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)),
                )
                Text(
                    text = app.label,
                    color = focusContent(isFocused, DeepinTextPrimary),
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                )
                if (selected) {
                    Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.selected), tint = focusContent(isFocused, DeepinTextPrimary))
                }
            }
        }
    }
    apps.firstOrNull { it.packageName == selectedPackage }?.takeIf { it.launchIntent != null }?.let { app ->
        MenuAction(icon = Icons.Filled.OpenInNew, label = stringResource(R.string.vpn_open, app.label), onClick = { onOpen(app) })
    }
}

/** Calendar's part of the edit sheet: the iCalendar feeds it reads. */
@Composable
fun CalendarEditSection(onFeedsChange: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { CalendarRepository(context) }
    var feeds by remember { mutableStateOf(repository.feeds()) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val anchor = remember { FocusRequester() }

    fun add() {
        if (input.isBlank()) return
        if (repository.addFeed(input)) {
            feeds = repository.feeds()
            input = ""
            error = false
            onFeedsChange()
        } else {
            error = true
        }
    }

    Text(text = stringResource(R.string.calendar_feeds), color = DeepinTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    feeds.forEach { url ->
        FocusSurface(
            onClick = {
                repository.removeFeed(url)
                feeds = repository.feeds()
                onFeedsChange()
                scope.refocus(anchor)
            },
            shape = RowShape,
            modifier = Modifier.fillMaxWidth(),
        ) { isFocused ->
            Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = url.removePrefix("https://"),
                    color = focusContent(isFocused, DeepinTextPrimary),
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.calendar_remove_feed), tint = focusContent(isFocused, DeepinTextSecondary))
            }
        }
    }
    TextInputField(
        value = input,
        onValueChange = {
            input = it
            error = false
        },
        placeholder = stringResource(R.string.calendar_feed_hint),
        keyboardType = KeyboardType.Uri,
        onSubmit = ::add,
    )
    MenuAction(icon = Icons.Filled.Add, label = stringResource(R.string.calendar_add), modifier = Modifier.focusRequester(anchor), onClick = ::add)
    if (error) Text(text = stringResource(R.string.calendar_invalid_url), color = DeepinTextSecondary, fontSize = 12.sp)
    Text(text = stringResource(R.string.calendar_help), color = DeepinTextSecondary, fontSize = 11.sp)
}

/** Continue-watching's part of the edit sheet: a row per entry to resume it, and the permission
 * the TV asks for before it shares that list. [onChanged] makes the card re-read right away. */
@Composable
fun WatchNextEditSection(onChanged: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { WatchNextRepository(context) }
    var granted by remember { mutableStateOf(repository.hasPermission()) }
    var items by remember { mutableStateOf<List<WatchNextItem>?>(null) }
    var reload by remember { mutableStateOf(0) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = repository.hasPermission()
        reload++
        onChanged()
    }
    LaunchedEffect(reload) {
        items = withContext(Dispatchers.IO) { repository.load()?.items } ?: emptyList()
    }

    Text(text = stringResource(R.string.widget_watch_next), color = DeepinTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    if (!granted) {
        MenuAction(
            icon = Icons.Filled.Check,
            label = stringResource(R.string.watchnext_grant),
            onClick = { permissionLauncher.launch(WatchNextRepository.PERMISSION) },
        )
        Text(text = stringResource(R.string.watchnext_grant_hint), color = DeepinTextSecondary, fontSize = 11.sp)
        return
    }
    val list = items
    when {
        list == null -> Text(text = stringResource(R.string.loading), color = DeepinTextSecondary, fontSize = 12.sp)
        list.isEmpty() -> Text(text = stringResource(R.string.watchnext_empty), color = DeepinTextPrimary, fontSize = 13.sp)
        else -> list.forEach { item ->
            MenuAction(
                icon = Icons.Filled.PlayArrow,
                label = stringResource(R.string.watchnext_resume, item.title),
                onClick = {
                    val intent = runCatching { Intent.parseUri(item.intentUri, Intent.URI_INTENT_SCHEME) }.getOrNull()
                    if (intent != null) safeStartActivity(context, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
            )
        }
    }
}

/** Notifications' part of the edit sheet: open or dismiss each one, and the access the TV asks for
 * before apps' notifications are shared with the launcher. */
@Composable
fun NotificationsEditSection() {
    val context = LocalContext.current
    val enabled = rememberNotificationAccess()
    val entries = rememberNotificationEntries()
    val scope = rememberCoroutineScope()
    val anchor = remember { FocusRequester() }

    Text(text = stringResource(R.string.widget_notifications), color = DeepinTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    if (!enabled) {
        MenuAction(
            icon = Icons.Filled.OpenInNew,
            label = stringResource(R.string.notif_grant),
            onClick = {
                val granted = runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }.isSuccess
                // Some TVs ship without that screen: the general settings are the next best place.
                if (!granted) safeStartActivity(context, Intent(Settings.ACTION_SETTINGS))
            },
        )
        Text(text = stringResource(R.string.notif_privacy), color = DeepinTextSecondary, fontSize = 11.sp)
        return
    }
    if (entries.isEmpty()) {
        Text(text = stringResource(R.string.notif_empty), color = DeepinTextPrimary, fontSize = 13.sp)
        return
    }
    entries.take(8).forEach { entry ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            FocusSurface(
                onClick = { DeepinNotificationService.instance?.open(entry.key) },
                shape = RowShape,
                modifier = Modifier.weight(1f),
            ) { isFocused ->
                Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                    Text(text = entry.appLabel, color = focusContent(isFocused, DeepinTextSecondary), fontSize = 11.sp, maxLines = 1)
                    Text(text = entry.title.ifEmpty { entry.text }, color = focusContent(isFocused, DeepinTextPrimary), fontSize = 13.sp, maxLines = 1)
                    if (entry.title.isNotEmpty() && entry.text.isNotEmpty()) {
                        Text(text = entry.text, color = focusContent(isFocused, DeepinTextSecondary), fontSize = 11.sp, maxLines = 2)
                    }
                }
            }
            FocusSurface(
                onClick = {
                    DeepinNotificationService.instance?.dismiss(entry.key)
                    scope.refocus(anchor)
                },
                shape = RowShape,
                modifier = Modifier.padding(start = 6.dp),
            ) { isFocused ->
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.notif_dismiss),
                    tint = focusContent(isFocused, DeepinTextSecondary),
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
    MenuAction(
        icon = Icons.Filled.Close,
        label = stringResource(R.string.notif_clear_all),
        modifier = Modifier.focusRequester(anchor),
        onClick = { DeepinNotificationService.instance?.dismissAll() },
    )
}

@Composable
private fun MenuAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FocusSurface(onClick = onClick, shape = RowShape, modifier = modifier.fillMaxWidth()) { isFocused ->
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = focusContent(isFocused, DeepinTextPrimary), modifier = Modifier.padding(end = 10.dp))
            Text(text = label, color = focusContent(isFocused, DeepinTextPrimary), fontSize = 13.sp)
        }
    }
}
