package com.deepintv.launcher.data

/** The little the launcher keeps of a notification: enough to list it, nothing that holds memory
 * (no icons, no bitmaps, no PendingIntent). Opening it looks the live one up by [key]. */
data class NotificationEntry(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postTime: Long,
)

private const val MAX_TITLE = 80
private const val MAX_TEXT = 120
const val MAX_NOTIFICATIONS = 20

/** A posted notification as an entry, or null when it isn't worth listing: ongoing ones (media
 * players, downloads), group summaries, the launcher's own, and empty ones. */
internal fun notificationEntry(
    key: String,
    packageName: String,
    appLabel: String,
    title: String?,
    text: String?,
    postTime: Long,
    ongoing: Boolean,
    groupSummary: Boolean,
    ownPackage: String,
): NotificationEntry? {
    if (ongoing || groupSummary || packageName == ownPackage) return null
    val cleanTitle = title.orEmpty().trim().take(MAX_TITLE)
    val cleanText = text.orEmpty().trim().take(MAX_TEXT)
    if (cleanTitle.isEmpty() && cleanText.isEmpty()) return null
    return NotificationEntry(key, packageName, appLabel.ifBlank { packageName }, cleanTitle, cleanText, postTime)
}

/** [entry] added (or replacing the one with the same key), newest first, at most [max]. */
internal fun withNotification(list: List<NotificationEntry>, entry: NotificationEntry, max: Int = MAX_NOTIFICATIONS): List<NotificationEntry> =
    (list.filter { it.key != entry.key } + entry).sortedByDescending { it.postTime }.take(max)

internal fun withoutNotification(list: List<NotificationEntry>, key: String): List<NotificationEntry> =
    list.filter { it.key != key }
