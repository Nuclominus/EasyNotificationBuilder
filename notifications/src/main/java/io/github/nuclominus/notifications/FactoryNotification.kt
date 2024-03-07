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
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import io.github.nuclominus.notifications.config.GlobalNotificationConfiguration
import io.github.nuclominus.notifications.entry.NotificationEntry
import java.util.*

abstract class FactoryNotification<T>(private val config: GlobalNotificationConfiguration) {

    abstract fun getChannelId(): String
    abstract fun getDescription(): String

    private val silentChannelId = config.getContext().getString(R.string.enb_silent_channel_id)
    private val replyRemoteInputId =
        config.getContext().getString(R.string.enb_reply_remote_input_id)

    @Suppress("unused")
    fun createChannel(channelId: String, name: String? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel(
                id = channelId,
                name = name,
                priority = NotificationManager.IMPORTANCE_MAX
            )
    }

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

    fun createBuilder(notif: NotificationEntry): NotificationCompat.Builder {
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(
            config.getContext(),
            if (onAppInBackground()) notif.channelId else silentChannelId
        )
        builder.apply {
            config.getSmallIcon()?.let(::setSmallIcon)
            setWhen(System.currentTimeMillis())

            // set priority by app background state
            priority = if (onAppInBackground())
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

    abstract fun buildContentStyle(
        ntf: MutableList<NotificationEntry>,
        notif: NotificationEntry
    ): Style

    abstract fun removeNotifications(channelId: String)

    abstract fun getIntent(context: Context, model: T): Intent

    fun getDefaultIcon(): Bitmap? = config.getNotificationDefaultIcon()

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

    fun getAction(intent: Intent, icon: Int, actionTitle: Int): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            icon,
            config.getContext().getString(actionTitle),
            getPendingIntent(intent)
        ).build()
    }

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

    fun getPendingIntent(model: T): PendingIntent {
        val intent = getIntent(config.getContext(), model)

        return PendingIntent.getActivity(
            config.getContext(),
            Random().nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun onAppInBackground(): Boolean {
        return true
    }

    fun getNotifId(channelId: String): Int {
        val sequence = channelId.toCharArray().asSequence()
        val iterator = sequence.iterator()
        var id = 0
        while (iterator.hasNext()) {
            id += iterator.next().code
        }
        return id
    }

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

    fun cancelAllNotifications() {
        config.withNotificationManager {
            cancelAll()
        }
    }

    fun loadImage(imageUrl: String?, loadImageAction: (String) -> Bitmap?): Bitmap? {
        if (imageUrl != null && !TextUtils.isEmpty(imageUrl)) {
            return loadImageAction(imageUrl) ?: getDefaultIcon()
        }
        return getDefaultIcon()
    }
}