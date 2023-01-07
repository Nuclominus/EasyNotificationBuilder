package io.github.nuclominus.notifications

import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import io.github.nuclominus.notifications.config.GlobalNotificationConfiguration
import io.github.nuclominus.notifications.entry.NotificationEntry

abstract class FactorySingleNotification<T>(private val config: GlobalNotificationConfiguration) :
    FactoryNotification<T>(config) {
    private val notifications = hashMapOf<String, MutableList<NotificationEntry>>()

    fun show(entry: NotificationEntry, avatar: Bitmap? = null) {
        val ntfByChannel = notifications[entry.channelId]

        // add entry to array by channel
        notifications[entry.channelId] = ntfByChannel?.apply {
            add(entry)
        } ?: mutableListOf(entry)

        notifySingle(avatar, notifications[entry.channelId]!!)
    }

    private fun notifySingle(avatar: Bitmap?, ntf: MutableList<NotificationEntry>) {
        if (onAppInBackground()) {
            val notif = ntf.last()
            val builder = createBuilder(notif)

            // update notification summary
            builder.setContentTitle(notif.author)
                .setContentText(notif.content)
                .setLargeIcon(avatar ?: getDefaultIcon())

            notify(builder.build(), notif.notificationId)
        }
    }

    override fun buildContentStyle(
        ntf: MutableList<NotificationEntry>,
        notif: NotificationEntry
    ) = NotificationCompat.InboxStyle()

    override fun removeNotifications(channelId: String) {
        config.withNotificationManager {
            notifications[channelId]?.forEach {
                cancel(it.notificationId)
            }
            notifications.remove(channelId)
        }
    }
}