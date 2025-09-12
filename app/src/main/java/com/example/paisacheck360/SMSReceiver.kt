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
import com.example.paisacheck360.network.RetrofitClient
import com.example.paisacheck360.network.SmsRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val messageBody = messages.joinToString(" ") { it.messageBody }

            checkSmsWithNLP(context, messageBody)
        }
    }

    private fun checkSmsWithNLP(context: Context, message: String) {
        val request = SmsRequest(message)

        RetrofitClient.api.predictSms(request).enqueue(object : Callback<com.example.paisacheck360.network.SmsResponse> {
            override fun onResponse(
                call: Call<com.example.paisacheck360.network.SmsResponse>,
                response: Response<com.example.paisacheck360.network.SmsResponse>
            ) {
                val result = response.body()
                if (result != null && result.label.equals("scam", ignoreCase = true)) {

                    // Save scam to SharedPreferences
                    saveScamMessage(context, "SMS", message, System.currentTimeMillis())

                    // Notify UI
                    context.sendBroadcast(Intent("com.example.paisacheck360.SCAM_DATA_UPDATED"))

                    // Show notification
                    showScamNotification(context, message)

                    // Show alert popup
                    val alertIntent = Intent(context, ScamPopupService::class.java)
                    alertIntent.putExtra("sms_body", message)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(alertIntent)
                    } else {
                        context.startService(alertIntent)
                    }

                    Toast.makeText(context, "🚨 Scam SMS Detected by AI!", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<com.example.paisacheck360.network.SmsResponse>, t: Throwable) {
                Toast.makeText(context, "API call failed: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveScamMessage(context: Context, type: String, text: String, timestamp: Long) {
        val prefs = context.getSharedPreferences("SecureBharatPrefs", Context.MODE_PRIVATE)
        val existing = prefs.getStringSet("fraud_messages", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        // Format: type|text|timestamp
        existing.add("$type|$text|$timestamp")

        prefs.edit().putStringSet("fraud_messages", existing).apply()
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
            .setSmallIcon(R.drawable.ic_warning) // Replace with your icon
            .setContentTitle("⚠️ Scam SMS Detected")
            .setContentText(message.take(60) + "...")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }
}
