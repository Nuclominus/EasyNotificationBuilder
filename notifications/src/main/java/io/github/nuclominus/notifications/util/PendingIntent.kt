package io.github.nuclominus.notifications.util
import android.app.PendingIntent

/**
 * Type of [PendingIntent] to be used.
 */
sealed class PendingIntentType {
    object Activity : PendingIntentType()
    object Service : PendingIntentType()
    object Broadcast : PendingIntentType()
}