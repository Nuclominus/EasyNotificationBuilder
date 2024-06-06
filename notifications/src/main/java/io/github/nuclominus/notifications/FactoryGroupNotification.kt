package io.github.nuclominus.notifications

import android.graphics.Bitmap
import android.text.SpannableStringBuilder
import androidx.core.app.NotificationCompat
import io.github.nuclominus.notifications.config.GlobalNotificationConfiguration
import io.github.nuclominus.notifications.entry.NotificationEntry
import io.github.nuclominus.notifications.util.withBoldSpan

/**
 * Factory for group notifications
 *
 * @param T type of notification
 * @property config [GlobalNotificationConfiguration] notification configuration
 *
 */
@Suppress("unused")
abstract class FactoryGroupNotification<T>(config: GlobalNotificationConfiguration) :
    FactoryNotification<T>(config) {

    private val cache = hashMapOf<String, NotificationCompat.Builder>()

    /**
     * Show or update a group of notifications
     *
     * @param entry [NotificationEntry] notification entry
     * @param avatar [Bitmap] notification avatar (optional). If not provided, default icon will be used
     * @param includeSummary [Boolean] include summary notification
     *
     */
    fun showOrUpdate(entry: NotificationEntry, avatar: Bitmap?, includeSummary: Boolean = false) {
        val ntfByChannel = notifications[entry.channelId]

        // add entry to array by channel
        notifications[entry.channelId] = ntfByChannel?.apply {
            add(entry)
        } ?: mutableListOf(entry)

        // create/recreate group
        notifications[entry.channelId]?.let { notifications ->
            notifyGroup(avatar, notifications, includeSummary)
        }
    }

    /**
     * Notify a group of notifications
     *
     * @param avatar [Bitmap] notification avatar (optional). If not provided, default icon will be used
     * @param ntf [MutableList<NotificationEntry>] list of notifications
     * @param includeSummary [Boolean] Set this notification to be the group summary for a group of notifications. See [NotificationCompat.Builder.setGroupSummary]
     */
    private fun notifyGroup(
        avatar: Bitmap?,
        ntf: MutableList<NotificationEntry>,
        includeSummary: Boolean
    ) {
        if (showNotification()) {
            val notif = ntf.last()
            val builder = cache[notif.channelId]
                ?: createBuilder(notif)

            // update notification summary
            val content =
                SpannableStringBuilder("${notif.author} ${notif.content}")
                    .withBoldSpan(notif.author)

            builder.apply {
                setContentTitle(notif.title ?: notif.author)
                setContentText(content)
                setLargeIcon(avatar ?: getDefaultIcon())
                notif.group?.let(::setGroup)
                setGroupSummary(includeSummary)
                setStyle(buildContentStyle(ntf, notif))
                modifyBuilder(this)
            }

            // add to list of builders
            cache[notif.channelId] = builder

            notify(builder.build(), notif.notificationId)
        }
    }
}