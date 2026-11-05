package com.deepintv.launcher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.Text
import com.deepintv.launcher.R
import com.deepintv.launcher.data.NotificationEntry
import com.deepintv.launcher.service.DeepinNotificationService
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary

private const val CARD_ITEMS = 3

/** Whether notification access is granted, re-read whenever the launcher comes back to the front
 * (the person grants it in the TV's settings and returns). */
@Composable
fun rememberNotificationAccess(): Boolean {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(DeepinNotificationService.isEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enabled = DeepinNotificationService.isEnabled(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return enabled
}

/** The live list of notifications, updated as the service reports changes. */
@Composable
fun rememberNotificationEntries(): List<NotificationEntry> {
    var entries by remember { mutableStateOf(DeepinNotificationService.entries) }
    DisposableEffect(Unit) {
        val listener = { entries = DeepinNotificationService.entries }
        DeepinNotificationService.addListener(listener)
        listener()
        onDispose { DeepinNotificationService.removeListener(listener) }
    }
    return entries
}

/** The latest notifications posted on the TV. Opening the card lists them to open or dismiss. */
@Composable
fun NotificationsWidget(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val enabled = rememberNotificationAccess()
    val entries = rememberNotificationEntries()

    WidgetCard(title = stringResource(R.string.widget_notifications), onClick = onClick, modifier = modifier) {
        when {
            !enabled -> Text(text = stringResource(R.string.notif_needs_access), color = DeepinTextSecondary, fontSize = 12.sp)
            entries.isEmpty() -> Text(text = stringResource(R.string.notif_empty), color = DeepinTextSecondary, fontSize = 12.sp)
            else -> Column {
                entries.take(CARD_ITEMS).forEach { entry ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(text = entry.appLabel, color = DeepinTextSecondary, fontSize = 11.sp, maxLines = 1)
                        Text(
                            text = entry.title.ifEmpty { entry.text },
                            color = DeepinTextPrimary,
                            fontSize = 13.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
