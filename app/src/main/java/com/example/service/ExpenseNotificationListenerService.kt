package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.ExpenseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ExpenseNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // Skip notifications from our own app unless it's a test notification
        if (sbn.packageName == packageName) {
            if (!title.contains("[Test]") && !title.contains("[Prueba]")) {
                return
            }
        }

        if (text.isEmpty()) {
            return
        }

        Log.d("NotiExpenseService", "Notification received from ${sbn.packageName}: $title - $text")

        // Retrieve repository from Application
        val app = application as? ExpenseApp ?: return
        val repository = app.repository

        serviceScope.launch {
            repository.processNotification(
                title = title,
                body = text,
                appName = sbn.packageName,
                timestamp = sbn.postTime
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
