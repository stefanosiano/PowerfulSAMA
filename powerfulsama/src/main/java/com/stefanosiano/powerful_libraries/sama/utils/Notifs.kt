package com.stefanosiano.powerful_libraries.sama.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.SparseArray
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.stefanosiano.powerful_libraries.sama.logError
import com.stefanosiano.powerful_libraries.sama.utils.PowerfulSama.applicationContext

/** Utility class to manage the notifications. */
object Notifs {
    private val channels: HashMap<String, SamaNotifChannel> = HashMap()
    private val notificationBuilders: SparseArray<NotificationCompat.Builder> = SparseArray()

    /** Initialize the [Notifs] utility with the notification channels that will be used in the app.
     * Channels will be instantiated when needed. Call it from Application's [onCreate] method. */
    fun initChannels(channels: Array<out SamaNotifChannel>) = initChannels(channels.toList())

    /** Initialize the [Notifs] utility with the notification channels that will be used in the app.
     * Channels will be instantiated when needed. Call it from Application's [onCreate] method. */
    fun initChannels(channels: Collection<SamaNotifChannel>) {
        this.channels.clear()
        this.channels.putAll(channels.map { Pair(it.getChannelId(), it) })
    }

    /** Create a notification to be pushed in [channel] and show it with a [notifId].
     * You can customize the notification builder before notification is shown through [f]. */
    fun create(channel: SamaNotifChannel, notifId: Int, f: (NotificationCompat.Builder) -> NotificationCompat.Builder) {
        this.channels.put(channel.getChannelId(), channel)
        create(channel.getChannelId(), notifId, f)
    }

    /** Create a notification to be pushed in the channel [channelId] and show it with a [notifId].
     * You can customize the notification builder before notification is shown through [f]. */
    fun create(channelId: String, notifId: Int, f: (NotificationCompat.Builder) -> NotificationCompat.Builder) {
        var notification = NotificationCompat.Builder(applicationContext, channelId)
        notification = f(notification)
        notificationBuilders.put(notifId, notification)

        channels.get(channelId)?.let { createChannel(it) } ?: logError("Notification Channel $channelId not found!")
        // notificationId is a unique int for each notification that you must define
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(notifId, notification.build())
        }
    }

    /** Create a notification builder to be pushed in [channel] without showing it and returns it. */
    fun create(channel: SamaNotifChannel) = create(channel.getChannelId())

    /** Create a notification builder to be pushed in the channel [channelId] without showing it and returns it. */
    fun create(channelId: String): NotificationCompat.Builder {
        val notification = NotificationCompat.Builder(applicationContext, channelId)
        channels.get(channelId)?.let { createChannel(it) } ?: logError("Notification Channel $channelId not found!")
        return notification
    }

    /** Update a notification pushed in [channel] with id [notifId]. You can customize the notification builder
     *  before notification is shown through [f]. If it's not found, a new notification builder is passed to [f]. */
    fun update(channel: SamaNotifChannel, notifId: Int, f: (NotificationCompat.Builder) -> NotificationCompat.Builder) =
        update(channel.getChannelId(), notifId, f)

    /** Update a notification pushed in the channel [channelId] with id [notifId]. You can customize the notification builder
     *  before notification is shown through [f]. If it's not found, a new notification builder is passed to [f]. */
    fun update(channelId: String, notifId: Int, f: (NotificationCompat.Builder) -> NotificationCompat.Builder) {
        var notif = notificationBuilders.get(notifId) ?: NotificationCompat.Builder(applicationContext, channelId)
        notificationBuilders.put(notifId, notif)
        notif = f(notif)
        channels.get(channelId)?.let { createChannel(it) } ?: logError("Notification Channel $channelId not found!")
        with(NotificationManagerCompat.from(applicationContext)) { notify(notifId, notif.build()) }
    }

    /** Cancel and dismiss the notification [notifId]. */
    fun cancel(notifId: Int) {
        notificationBuilders.remove(notifId)
        with(NotificationManagerCompat.from(applicationContext)) { cancel(notifId) }
    }

    /** Create the channel. */
    private fun createChannel(c: SamaNotifChannel) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                c.getChannelId(),
                Res.string(c.getChannelNameId()),
                c.getChannelImportance()
            ).apply {
                description = Res.string(c.getChannelDescriptionId())
            }
            // Register the channel with the system
            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(c.customize(channel))
        }
    }
}

/** Interface to be implemented by an enum or a class to provide a notification channel used in the app. */
interface SamaNotifChannel {
    /** The channel id. Must be unique for each channel. */
    fun getChannelId(): String
    /** The channel name string id. */
    fun getChannelNameId(): Int
    /** The channel importance. Defaults to [NotificationManagerCompat.IMPORTANCE_DEFAULT]. */
    fun getChannelImportance(): Int = NotificationManagerCompat.IMPORTANCE_DEFAULT
    /** The channel description string id. Defaults to [getChannelNameId]. */
    fun getChannelDescriptionId(): Int = getChannelNameId()
    /** Function used to customize the channel before instantiate it. Do not rely on it being called multiple times, as it should be called only once. */
    fun customize(channel: NotificationChannel): NotificationChannel { return channel }
}
