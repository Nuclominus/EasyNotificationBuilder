package io.github.nuclominus.notifications

import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import io.github.nuclominus.notifications.config.GlobalNotificationConfiguration
import io.github.nuclominus.notifications.entry.NotificationEntry

/**
 * Factory for single notifications
 *
 * @param T type of notification
 * @property config [GlobalNotificationConfiguration] notification configuration
 *
 */
@Suppress("unused")
abstract class FactorySingleNotification<T>(private val config: GlobalNotificationConfiguration) :
    FactoryNotification<T>(config) {

    /**
     * Show or update a single notification
     *
     * @param entry [NotificationEntry] notification entry
     * @param avatar [Bitmap] notification avatar (optional). If not provided, default icon will be used
     */
    fun show(entry: NotificationEntry, avatar: Bitmap? = null) {
        val ntfByChannel = notifications[entry.channelId]

        // add entry to array by channel
        notifications[entry.channelId] = ntfByChannel?.apply {
            add(entry)
        } ?: mutableListOf(entry)

        notifications[entry.channelId]?.let {
            notifySingle(avatar, it)
        }
    }

    /**
     * Notify a single notification
     *
     * @param avatar [Bitmap] notification avatar (optional). If not provided, default icon will be used
     * @param ntf [MutableList<NotificationEntry>] list of notifications
     */
    private fun notifySingle(avatar: Bitmap?, ntf: MutableList<NotificationEntry>) {
        if (showNotification()) {
            val notif = ntf.last()
            val builder = createBuilder(notif)

            // update notification summary
            builder.setContentTitle(notif.author)
                .setContentText(notif.content)
                .setLargeIcon(avatar ?: getDefaultIcon())

            notify(builder.build(), notif.notificationId)
        }
    }

    /**
     * Build content style for notification
     *
     * @param ntf [MutableList<NotificationEntry>] list of notifications
     * @param notif [NotificationEntry] notification entry
     */
    override fun buildContentStyle(
        ntf: MutableList<NotificationEntry>,
        notif: NotificationEntry
    ) = NotificationCompat.InboxStyle()

}