package io.github.nuclominus.notifications.entry

import android.app.PendingIntent
import androidx.core.app.NotificationCompat

data class NotificationEntry(
    val channelId: String,
    val notificationId: Int,
    val title: String? = null,
    val group: String? = null,
    val author: String,
    val content: String,
    val pendingIntent: PendingIntent,
    val actions: List<NotificationCompat.Action?> = emptyList()
)