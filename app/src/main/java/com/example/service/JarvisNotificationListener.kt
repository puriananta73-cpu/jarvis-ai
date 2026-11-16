package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.AuditLogEntity
import com.example.data.JarvisDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JarvisNotificationListener : NotificationListenerService() {
    private val TAG = "JarvisNotifListener"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val WHATSAPP_PKG = "com.whatsapp"
        const val INSTAGRAM_PKG = "com.instagram.android"
        
        // Callback or flow for active notifications
        var onMessageIntercepted: ((sender: String, message: String, appName: String) -> Unit)? = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        if (pkg == WHATSAPP_PKG || pkg == INSTAGRAM_PKG) {
            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: "Unknown Sender"
            val text = extras.getCharSequence("android.text")?.toString() ?: ""

            if (text.isNotBlank()) {
                val appName = if (pkg == WHATSAPP_PKG) "WhatsApp" else "Instagram"
                Log.d(TAG, "Intercepted notification from $appName ($title): $text")

                serviceScope.launch {
                    try {
                        val db = JarvisDatabase.getDatabase(applicationContext)
                        db.auditLogDao().insertLog(
                            AuditLogEntity(
                                actionType = "NOTIF_INTERCEPT",
                                recipient = "$appName: $title",
                                content = text,
                                status = "PENDING_DECISION"
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to log intercepted notification", e)
                    }
                }

                onMessageIntercepted?.invoke(title, text, appName)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
