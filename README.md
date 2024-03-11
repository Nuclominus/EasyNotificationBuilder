EasyNotificationBuilder
=======================
[![Maven Central](https://img.shields.io/maven-central/v/io.github.nuclominus/easynotificationbuilder.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.nuclominus/easynotificationbuilder/overview)

EasyNotificationBuilder is a simple library for building notifications in Android. It provides an
easy-to-use interface for creating and customizing notifications, allowing developers to quickly add
notification functionality to their apps.

Features
--------

* Create and configure notifications
* Support different notification styles
* Group notifications
* Customize notification content, icon, and actions
* Support for Android 4.1 (Jelly Bean) and above

Installation
------------

To use EasyNotificationBuilder in your Android project, add the following dependency to your app's
build.gradle file:

```kotlin
implementation 'io.github.nuclominus:easynotificationbuilder:<version>'

```

or for version catalog:

```groovy
[versions]
...
notificationBuilderVersion = <version>

[libraries]

notification-builder = {group ="io.github.nuclominus", name = "easynotificationbuilder", version.ref = "notificationBuilderVersion" }

```

Usage
-----

For create your own notification you should use one of factory types:
* `FactorySingleNotification` - suitable for options where you need to show notifications without special conditions
* `FactoryGroupNotification` - option with channel push caching

Note: The samples used dagger2 DI, but DI is optional

Single notification
-------------------

Top level:
```kotlin
class PushNotificationsService : FirebaseMessagingService() {
    // 1. Provide our notification builder 
    @Inject
    lateinit var singleNotification: SingleNotification
    ...

    when (pushType) {
        // 2. Call build notification from model
        PushType.Single -> singleNotification.buildNotification(pushModel)
    }
}
```

Implementation:
```kotlin
class SingleNotification @Inject constructor(private val config: GlobalNotificationConfiguration) :
    FactorySingleNotification<SsinglePushModel>(config) {

    // 1. Setup channel data
    override fun getChannelId() = "Single Push"
    override fun getDescription() = "Showing single notifications"

    fun buildNotification(model: SessionPushModel) {
        // 2. Create channel before show notification
        createChannel(name = "Single Notification")

        // 3. Create notification entry
        val entry = NotificationEntry(
            channelId = getChannelId(),
            notificationId = System.currentTimeMillis().toInt(),
            author = model.authorName,
            content = model.text,
            group = getChannelId(),
            pendingIntent = getPendingIntent(model)
        )
        
        // 4. Optional. Load avatar. Without avatar used defaultIcon from global config
        val avatar = loadImage(model.authorAvatar) {
            ImageBinder(config.getContext())
                .getBitmap(it, Size(200, 200))
        }

        // 5. Show notification
        show(entry, avatar)
    }

    override fun getIntent(context: Context, model: SinglePushModel): Intent {
        return createSingleIntentFrom(context, model)
    }

}
```

Group notification
------------------
For example take chat notification

Top level:
```kotlin
class PushNotificationsService : FirebaseMessagingService() {
    // 1. Provide our notification builder 
    @Inject
    lateinit var chatNotification: ChatNotification
    ...

    when (pushType) {
        // 2. Call build notification from model
        PushType.Chat -> chatNotification.buildNotification(pushModel)
    }
}
```

Implementation:
```kotlin
private const val CHANNEL_ID = "Chat"
private const val CHANNEL_DESCRIPTION = "Showing chat notifications"

class ChatNotification @Inject constructor(
    private val config: GlobalNotificationConfiguration
) : FactoryGroupNotification<ChatPushModel>(config) {

    // 1. Setup channel data. Can be empty if you are configuring the channel yourself
    override fun getChannelId() = CHANNEL_ID
    override fun getDescription() = CHANNEL_DESCRIPTION

    fun buildNotification(model: ChatPushModel) {
        val chatId = model.chatId
        // 1. Create chennel for each chat separate and set name by author
        createChannel(
            channelId = chatId,
            name = "Chat: ${model.authorName}"
        )

        // 2. Create uniq notification id
        val notificationId = getNotifId(chatId)

        // 3. Create notification entry
        val entity = NotificationEntry(
            channelId = chatId,
            notificationId = notificationId,
            title = model.authorName,
            content = model.message,
            pendingIntent = getPendingIntent(model),
        )

        // 4. Optional. Load avatar. Without avatar used defaultIcon from global config
        val avatar = loadImage(model.authorAvatar) {
            ImageBinder(config.getContext())
                .getBitmap(it, Size(200, 200))
        }

        // 5. Show notification
        showOrUpdate(entity, avatar)
    }

    // 6. Setup content style
    override fun buildContentStyle(
        ntf: MutableList<NotificationEntry>,
        notif: NotificationEntry
    ): Style {
        val contentStyle = NotificationCompat.BigTextStyle()
        // 7. Show amount of unread notifications in title
        val bigContent = if (ntf.size > 1) "${notif.title} (${ntf.size})" else notif.author
        // 8. Set title and summary
        contentStyle
            .setBigContentTitle(bigContent)
            .setSummaryText(notif.author)

        // 9. Show only last 5 messages
        contentStyle.bigText(
            ntf.takeLast(5)
                .map(NotificationEntry::content)
                .joinToString("\n")
        )

        return contentStyle
    }

    override fun getIntent(context: Context, model: ChatPushModel): Intent {
        return createChatIntentFrom(context, model)
    }

}
```

Actions
-------

Also you could add custom actions to notification. For example let's add `reply` action:

```kotlin
        ...
        val replyActionIntent =
            Intent(config.getContext(), ReplyReceiver::class.java).apply {
                action = "Reply Message ${model.messageId}"
                // Put all nessessary data for reply action in BroadcastReceiver
                putExtra(ReplyReceiver.TYPE, PushConst.REPLY_ACTION_TYPE)
                putExtra(ReplyReceiver.NOTIFICATION_ID, notificationId)
                putExtra(ReplyReceiver.CHAT_ID, model.chatId)
            }

        val replyActions = mutableListOf<NotificationCompat.Action>().apply {
                add(
                    getActionReply(
                        intent = replyActionIntent,
                        replyLabel = config.getContext().getString(R.string.push_reply_action), // set action replay lable 
                        iconAction = R.drawable.ic_arrow_right, // set action replay icon
                    )
                )
        }
        val entry = NotificationEntry(
            channelId = model.chatId,
            notificationId = notificationId,
            author = model.authorName,
            title = model.chatTitle,
            content = model.text,
            group = model.chatId,
            pendingIntent = getPendingIntent(model),
            actions = replyActions, // <- Add actions 
        )
``` 

Catch payload from replay into your BroadcastReceiver and send network request with reply message

License
-------

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.
