package io.github.nuclominus.notifications

import android.graphics.Bitmap
import android.text.SpannableStringBuilder
import androidx.core.app.NotificationCompat
import io.github.nuclominus.notifications.config.GlobalNotificationConfiguration
import io.github.nuclominus.notifications.entry.NotificationEntry
import io.github.nuclominus.notifications.util.withBoldSpan

abstract class FactoryGroupNotification<T>(private val config: GlobalNotificationConfiguration) :
    FactoryNotification<T>(config) {
    private val notifications = hashMapOf<String, MutableList<NotificationEntry>>()
    private val builders = hashMapOf<String, NotificationCompat.Builder>()

    // show grouped notification
    fun showOrUpdate(entry: NotificationEntry, avatar: Bitmap?) {
        val ntfByChannel = notifications[entry.channelId]

        // add entry to array by channel
        notifications[entry.channelId] = ntfByChannel?.apply {
            add(entry)
        } ?: mutableListOf(entry)

        // create/recreate group
        notifyGroup(avatar, notifications[entry.channelId]!!)
    }

    private fun notifyGroup(avatar: Bitmap?, ntf: MutableList<NotificationEntry>) {
        if (onAppInBackground()) {
            val notif = ntf.last()
            val builder = builders[notif.channelId]
                ?: createBuilder(notif)

            // update notification summary
            val content =
                SpannableStringBuilder("${notif.author} ${notif.content}").withBoldSpan(notif.author)

            builder.apply {
                setContentTitle(notif.title ?: notif.author)
                setContentText(content)
                setLargeIcon(avatar ?: getDefaultIcon())
                notif.group?.let(::setGroup)
                setGroupSummary(false)
                setStyle(buildContentStyle(ntf, notif))
            }

            // add to list of builders
            builders[notif.channelId] = builder

            notify(builder.build(), notif.notificationId)
        }
    }

    override fun removeNotifications(channelId: String) {
        config.withNotificationManager {
            notifications[channelId]?.forEach {
                cancel(it.notificationId)
            }
            notifications.remove(channelId)
        }
    }
}