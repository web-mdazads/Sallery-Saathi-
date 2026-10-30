package com.example.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BankNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val pkg = notification.packageName ?: return

        // Never inspect Salary Saathi's own notifications
        if (pkg == applicationContext.packageName) return
        if (notification.isOngoing) return

        val extras = notification.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val shortText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        val messageContent = bigText.ifBlank { shortText }.trim()
        if (messageContent.length < 12) return

        val postTime = notification.postTime.takeIf { it > 0L } ?: System.currentTimeMillis()
        val appContext = applicationContext

        serviceScope.launch {
            BankAlertCoordinator.processIncomingMessage(
                context = appContext,
                senderAddress = title.ifBlank { pkg },
                messageBody = messageContent,
                timestampMillis = postTime
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        fun isNotificationAccessEnabled(context: Context): Boolean {
            return runCatching {
                val flat = Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners"
                ) ?: return false
                val myComponent = ComponentName(context, BankNotificationListenerService::class.java)
                flat.split(":").any { ComponentName.unflattenFromString(it) == myComponent }
            }.getOrDefault(false)
        }
    }
}
