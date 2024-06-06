package io.github.nuclominus.notifications.entry

import android.app.PendingIntent
import androidx.core.app.NotificationCompat

/**
 * Entry of notification
 *
 * @property channelId [String] android channel id/name
 * @property notificationId [Int] notification uuid
 * @property title [String] notification title (optional)
 * @property group [String] notification group id (optional)
 * @property author [String] notification author name
 * @property content [String] notification message
 * @property pendingIntent [PendingIntent] notification PendingIntent
 * @property actions List<[NotificationCompat.Action]> notification custom actions
 * @property data HashMap<[String], [Any]> custom data
 *
 */
data class NotificationEntry(
    val channelId: String,
    val notificationId: Int,
    val title: String? = null,
    val group: String? = null,
    val author: String = "",
    val content: String = "",
    val pendingIntent: PendingIntent,
    val actions: List<NotificationCompat.Action> = emptyList(),
    val data: HashMap<String, Any> = hashMapOf()
)