package com.deepintv.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.deepintv.launcher.R
import com.deepintv.launcher.data.WatchNextRepository
import com.deepintv.launcher.data.WatchNextResult
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary
import com.deepintv.launcher.ui.theme.DeepinWash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CARD_ITEMS = 3

/** "Continue watching": the last things started in other apps, read from the TV's Watch Next row.
 * Opening the card (OK) shows the full list to resume from. [version] re-reads it right away,
 * e.g. after the permission was just granted. */
@Composable
fun WatchNextWidget(refreshMinutes: Int, version: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var result by remember { mutableStateOf<WatchNextResult?>(null) }

    LaunchedEffect(refreshMinutes, version) {
        val repo = WatchNextRepository(context)
        pollWithBackoff(
            intervalMs = refreshMinutes * 60_000L,
            fetch = { withContext(Dispatchers.IO) { Polled(repo.load(), stale = false) } },
            onResult = { polled -> polled.value?.let { result = it } },
        )
    }

    WidgetCard(title = stringResource(R.string.widget_watch_next), onClick = onClick, modifier = modifier) {
        val current = result
        when {
            current == null -> Text(text = stringResource(R.string.loading), color = DeepinTextSecondary, fontSize = 12.sp)
            current.permissionDenied -> Text(text = stringResource(R.string.watchnext_needs_permission), color = DeepinTextSecondary, fontSize = 12.sp)
            current.items.isEmpty() -> Text(text = stringResource(R.string.watchnext_empty), color = DeepinTextSecondary, fontSize = 12.sp)
            else -> Column {
                current.items.take(CARD_ITEMS).forEach { item ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(text = item.title, color = DeepinTextPrimary, fontSize = 13.sp, maxLines = 1)
                        item.subtitle?.let {
                            Text(text = it, color = DeepinTextSecondary, fontSize = 11.sp, maxLines = 1)
                        }
                        item.progress?.let { progress ->
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(DeepinWash),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .height(3.dp)
                                        .background(DeepinTextPrimary),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
