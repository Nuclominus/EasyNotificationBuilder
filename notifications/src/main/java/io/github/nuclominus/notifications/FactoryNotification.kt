package io.github.nuclominus.notifications

import android.Manifest
import android.app.Notification
import androidx.core.app.NotificationCompat.Style
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.text.TextUtils
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import io.github.nuclominus.notifications.config.GlobalNotificationConfiguration
import io.github.nuclominus.notifications.entry.NotificationEntry
import io.github.nuclominus.notifications.util.PendingIntentType
import java.util.*

/**
 * FactoryNotification
 *
 * @property config The configuration for the notifications
 * @constructor Create FactoryNotification
 */
abstract class FactoryNotification<T>(private val config: GlobalNotificationConfiguration) {

    /**
     * Get the channel id for the notification
     *
     * @return The channel id
     */
    abstract fun getChannelId(): String

    /**
     * Get the description for the notification
     *
     * @return The description
     */
    abstract fun getDescription(): String

    protected val notifications = hashMapOf<String, MutableList<NotificationEntry>>()
    private val silentChannelId = config.getContext().getString(R.string.enb_silent_channel_id)
    private val replyRemoteInputId =
        config.getContext().getString(R.string.enb_reply_remote_input_id)

    /**
     * Create a notification channel
     *
     * @param name The channel name
     * @see NotificationChannel
     */
    @Suppress("unused")
    fun createChannel(name: String? = getChannelId()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel(
                id = getChannelId(),
                name = name,
                priority = NotificationManager.IMPORTANCE_MAX
            )
    }

    /**
     * Create a notification channel
     *
     * @param channelId The channel id
     * @param name The channel name
     * @see NotificationChannel
     */
    @Suppress("unused")
    fun createChannel(channelId: String, name: String? = getChannelId()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel(
                id = channelId,
                name = name,
                priority = NotificationManager.IMPORTANCE_MAX
            )
    }

    /**
     * Create a silent notification channel
     *
     * @see NotificationChannel
     */
    @Suppress("unused")
    private fun createSilentChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(
                id = silentChannelId,
                name = silentChannelId,
                priority = NotificationManager.IMPORTANCE_DEFAULT
            )
        }
    }

    /**
     * Create a notification channel
     *
     * @param id The channel id
     * @param name The channel name
     * @param priority The channel priority
     * @see NotificationChannel
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(id: String, name: String?, priority: Int) {
        val channel = NotificationChannel(id, name ?: id, priority).apply {
            description = this@FactoryNotification.getDescription()
            enableLights(true)
            val att = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), att)
            config.getNotificationColor()?.let(::setLightColor)
            enableVibration(true)
        }

        config.withNotificationManager {
            createNotificationChannel(channel)
        }
    }

    /**
     * Create a notification builder
     *
     * @param notif The notification
     * @return The notification builder
     * @see NotificationCompat.Builder
     */
    fun createBuilder(notif: NotificationEntry): NotificationCompat.Builder {
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(
            config.getContext(),
            if (showNotification()) notif.channelId else silentChannelId
        )
        builder.apply {
            config.getSmallIcon()?.let(::setSmallIcon)
            setWhen(System.currentTimeMillis())

            // set priority by app background state
            priority = if (showNotification())
                NotificationCompat.PRIORITY_MAX
            else
                NotificationCompat.PRIORITY_DEFAULT

            setSound(alarmSound)
            setContentIntent(notif.pendingIntent)
            config.getNotificationColor()?.let(::setColor)
            setAutoCancel(true)
            setVibrate(config.getVibratePattern())
            notif.actions.takeIf { it.isNotEmpty() }?.forEach {
                addAction(it)
            }
        }
        return builder
    }

    /**
     * Build the style for the notification
     *
     * @param ntf The list of notifications
     * @param notif The notification
     * @return The style for the notification
     * @see NotificationCompat.Style
     */
    abstract fun buildContentStyle(
        ntf: MutableList<NotificationEntry>,
        notif: NotificationEntry
    ): Style

    /**
     * Get the intent for the notification
     *
     * @param context The context
     * @param model The model to be used to create the intent
     * @return The intent
     */
    abstract fun getIntent(context: Context, model: T): Intent

    /**
     * Get the default icon for the notification
     */
    fun getDefaultIcon(): Bitmap? = config.getNotificationDefaultIcon()

    /**
     * Get a PendingIntent for a notification action
     *
     * @param intent The intent to be used for the action
     * @return The PendingIntent
     */
    private fun getPendingIntent(intent: Intent): PendingIntent? {
        return PendingIntent.getBroadcast(
            config.getContext(),
            kotlin.random.Random.nextInt(),
            intent,
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_MUTABLE
            }
        )
    }

    /**
     * Get an action for a notification
     *
     * @param intent The intent to be used for the action
     * @param icon The icon for the action
     * @param actionTitle The title for the action
     * @return The action
     * @see NotificationCompat.Action
     */
    fun getAction(intent: Intent, icon: Int, actionTitle: Int): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            icon,
            config.getContext().getString(actionTitle),
            getPendingIntent(intent)
        ).build()
    }

    /**
     * Get a reply action for a notification
     *
     * @param intent The intent to be used for the reply action
     * @param replyLabel The label for the reply action
     * @param iconAction The icon for the reply action
     * @return The reply action
     * @see NotificationCompat.Action
     */
    fun getActionReply(
        intent: Intent,
        replyLabel: String,
        iconAction: Int
    ): NotificationCompat.Action {
        val remoteInput: RemoteInput = RemoteInput.Builder(replyRemoteInputId).run {
            setLabel(replyLabel)
            build()
        }

        // Create the reply action and add the remote input.
        return NotificationCompat.Action.Builder(
            iconAction,
            replyLabel,
            getPendingIntent(intent)
        ).addRemoteInput(remoteInput)
            .build()
    }

    /**
     * Get a PendingIntent for a notification action
     *
     * @param model The model to be used to create the PendingIntent
     * @param type The type of PendingIntent to be created
     * @param requestCode The request code for the PendingIntent
     * @param flags The flags for the PendingIntent
     * @return The PendingIntent
     */
    @Suppress("unused")
    fun getPendingIntent(
        model: T,
        type: PendingIntentType = PendingIntentType.Activity,
        requestCode: Int = Random().nextInt(),
        flags: Int = PendingIntent.FLAG_UPDATE_CURRENT
    ): PendingIntent {
        val context = config.getContext()
        val intent = getIntent(config.getContext(), model)
        val flag = flags or PendingIntent.FLAG_IMMUTABLE

        return when (type) {
            PendingIntentType.Activity -> PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                flag
            )

            PendingIntentType.Service -> PendingIntent.getService(
                context,
                requestCode,
                intent,
                flag
            )

            PendingIntentType.Broadcast -> PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                flag
            )
        }
    }

    /**
     * Special check that allows to show or not the notification
     * For example, if the user is in a call, the notification should not be shown
     * or show notifications only if app in background etc.
     *
     * Override this method to implement your own logic
     *
     * @return true if the notification should be shown, false otherwise
     */
    fun showNotification(): Boolean {
        return true
    }

    /**
     * Get the notification id for a channel
     *
     * @param channelId The channel id
     * @return The notification id
     */
    fun getNotifId(channelId: String): Int {
        val sequence = channelId.toCharArray().asSequence()
        val iterator = sequence.iterator()
        var id = 0
        while (iterator.hasNext()) {
            id += iterator.next().code
        }
        return id
    }

    /**
     * Notify the user with a notification.
     * Also checks if the app has the permission to show notifications
     *
     * @param notification The notification to be shown
     * @param notId The notification id
     */
    fun notify(notification: Notification?, notId: Int) {
        notification?.let {
            if (ContextCompat.checkSelfPermission(
                    config.getContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                config.withNotificationManager {
                    notify(notId, notification)
                }
            }
        }
    }

    /**
     * Cancel all app notifications
     *
     * For example, if user logs out, you can cancel all notifications
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun cancelAllNotifications() {
        config.withNotificationManager(NotificationManagerCompat::cancelAll)
    }

    /**
     * Remove all notifications from this type of channel
     */
    fun clearNotificationsBy() {
        cancelNotifications(getChannelId())
    }

    /**
     * Remove all notifications by [channelId]
     *
     * Used to remove all notifications from a specific channel.
     * In way when you configure multiple channels of single type.
     * For example, you have a chat app and you have a channel for each chat.
     *
     * If you want to remove all notifications from all channels, use [cancelAllNotifications]
     *
     * @param channelId [String] channel id
     */
    fun clearNotificationsBy(channelId: String) {
        cancelNotifications(channelId)
    }

    /**
     * Remove all notifications from specific channel
     */
    private fun cancelNotifications(channelId: String) {
        config.withNotificationManager {
            notifications[channelId]?.forEach {
                cancel(it.notificationId)
            }
            notifications.remove(channelId)
        }
    }

    /**
     * Additional check for the notification Avatar/LargeIcon
     *
     * @param imageUrl The url of the image
     * @param loadImageAction The action to load the image
     * @return The image
     */
    fun loadImage(imageUrl: String?, loadImageAction: (String) -> Bitmap?): Bitmap? {
        if (imageUrl != null && !TextUtils.isEmpty(imageUrl)) {
            return loadImageAction(imageUrl) ?: getDefaultIcon()
        }
        return getDefaultIcon()
    }
}