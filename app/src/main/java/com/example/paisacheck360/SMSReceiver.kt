package com.example.paisacheck360

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.NotificationChannel
import android.app.NotificationManager

class SMSReceiver : BroadcastReceiver() {

    private val scamKeywords = listOf("win", "click here", "lottery", "urgent", "OTP", "claim", "reward", "offer")

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val messageBody = messages.joinToString(" ") { it.messageBody }

            if (scamKeywords.any { messageBody.contains(it, ignoreCase = true) }) {
                showScamNotification(context, messageBody)

                // Start floating popup alert
                val alertIntent = Intent(context, ScamPopupService::class.java)
                alertIntent.putExtra("sms_body", messageBody)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(alertIntent)
                } else {
                    context.startService(alertIntent)
                }

                Toast.makeText(context, "🚨 Scam SMS Detected!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showScamNotification(context: Context, message: String) {
        val channelId = "scam_alerts"
        val notificationId = 101

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Scam Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows notifications for suspected scam SMS"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_warning) // Make sure this icon exists in res/drawable
            .setContentTitle("⚠️ Scam SMS Detected")
            .setContentText(message.take(60) + "...")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }
}
