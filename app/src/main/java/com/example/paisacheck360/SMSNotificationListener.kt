package com.example.paisacheck360

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class SMSNotificationListener : NotificationListenerService() {

    private val scamKeywords = listOf("fraud", "otp", "bank", "refund", "kbc", "lottery", "winner")

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        val fullMessage = "$title\n$text"
        Log.d("SMSNotification", "From: $packageName\nMessage: $fullMessage")

        val lowerMsg = fullMessage.lowercase()
        if (scamKeywords.any { lowerMsg.contains(it) }) {
            // ✅ Launch ScamPopupService to show warning — same as your SMSReceiver logic
            val intent = Intent(this, ScamPopupService::class.java).apply {
                putExtra("scam_message", fullMessage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startService(intent)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Optional: handle if needed
    }
}
