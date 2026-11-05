package com.deepintv.launcher.service

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.deepintv.launcher.data.NotificationEntry
import com.deepintv.launcher.data.notificationEntry
import com.deepintv.launcher.data.withNotification
import com.deepintv.launcher.data.withoutNotification
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Lets the Notifications widget list what the TV's apps post. It only runs once the person grants
 * notification access in the TV's settings; until then Android never binds it. It keeps a short
 * list of titles and texts in memory (see [NotificationEntry]) and hands the live notification
 * back by key when one is opened or dismissed.
 */
class DeepinNotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        instance = this
        entries = runCatching { activeNotifications.orEmpty().mapNotNull(::toEntry) }
            .getOrDefault(emptyList())
            .sortedByDescending { it.postTime }
            .take(MAX)
        notifyListeners()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        entries = emptyList()
        notifyListeners()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val entry = toEntry(sbn)
        entries = if (entry != null) withNotification(entries, entry, MAX) else withoutNotification(entries, sbn.key)
        notifyListeners()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        entries = withoutNotification(entries, sbn.key)
        notifyListeners()
    }

    private fun toEntry(sbn: StatusBarNotification): NotificationEntry? {
        val extras = sbn.notification.extras
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrDefault(sbn.packageName)
        return notificationEntry(
            key = sbn.key,
            packageName = sbn.packageName,
            appLabel = label,
            title = extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString(),
            text = extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString(),
            postTime = sbn.postTime,
            ongoing = sbn.isOngoing,
            groupSummary = sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0,
            ownPackage = packageName,
        )
    }

    /** Runs the notification's own action (usually opens the app on it). */
    fun open(key: String) {
        runCatching {
            activeNotifications.firstOrNull { it.key == key }?.notification?.contentIntent?.send()
        }.onFailure { Log.w(TAG, "Could not open notification", it) }
    }

    fun dismiss(key: String) {
        runCatching { cancelNotification(key) }
    }

    fun dismissAll() {
        runCatching { cancelAllNotifications() }
    }

    companion object {
        private const val TAG = "DeepinNotifications"
        private const val MAX = 20

        @Volatile var instance: DeepinNotificationService? = null
            private set

        /** Newest first. Empty while access isn't granted. */
        @Volatile var entries: List<NotificationEntry> = emptyList()
            private set

        private val listeners = CopyOnWriteArraySet<() -> Unit>()

        fun addListener(listener: () -> Unit) { listeners.add(listener) }
        fun removeListener(listener: () -> Unit) { listeners.remove(listener) }
        private fun notifyListeners() = listeners.forEach { it() }

        /** Whether the person has granted notification access to the launcher. */
        fun isEnabled(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
}
