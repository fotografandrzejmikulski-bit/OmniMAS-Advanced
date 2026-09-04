package pl.omnimas.advanced

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification


data class LocalNotification(
    val pkg: String,
    val title: String,
    val text: String,
    val time: Long
)

object NotificationMemory {
    private val data = ArrayDeque<LocalNotification>()

    @Synchronized
    fun add(notification: LocalNotification) {
        data.addLast(notification)
        while (data.size > 200) data.removeFirst()
    }

    @Synchronized
    fun snapshot(): List<LocalNotification> = data.toList()
}

class OmniNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        NotificationMemory.add(
            LocalNotification(
                pkg = sbn.packageName,
                title = extras.getCharSequence("android.title")?.toString().orEmpty(),
                text = extras.getCharSequence("android.text")?.toString().orEmpty(),
                time = sbn.postTime
            )
        )
    }
}
