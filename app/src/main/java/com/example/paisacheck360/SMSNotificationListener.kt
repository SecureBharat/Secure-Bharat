package com.example.paisacheck360

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.content.Intent
import android.os.Build

class SMSNotificationListener : NotificationListenerService() {

    private val scamKeywords = listOf("win", "click here", "lottery", "urgent", "OTP", "claim", "reward", "offer")

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName
        val extras = sbn.notification.extras

        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        // Log to check real content
        Log.d("SMSNotifyListener", "Package: $packageName, Title: $title, Text: $text")

        // 🔍 Simple scam keyword check
        if (scamKeywords.any { text.contains(it, ignoreCase = true) }) {
            val alertIntent = Intent(this, ScamPopupService::class.java).apply {
                putExtra("sms_body", "$title: $text")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(alertIntent)
            } else {
                startService(alertIntent)
            }

            Log.d("SMSNotifyListener", "⚠️ Scam SMS Detected from $packageName!")
        }
    }
}
