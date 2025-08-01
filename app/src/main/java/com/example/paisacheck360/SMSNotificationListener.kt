package com.example.paisacheck360

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.paisacheck360.network.RetrofitClient
import com.example.paisacheck360.network.SmsRequest
import com.example.paisacheck360.network.SmsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SMSNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        val fullMessage = "$title: $text"
        Log.d("SMSNotifyListener", "Checking message: $fullMessage")

        checkSmsWithNLP(this, fullMessage)
    }

    private fun checkSmsWithNLP(context: Context, message: String) {
        val request = SmsRequest(message)

        RetrofitClient.api.predictSms(request).enqueue(object : Callback<SmsResponse> {
            override fun onResponse(call: Call<SmsResponse>, response: Response<SmsResponse>) {
                val result = response.body()
                if (result != null && result.label.equals("scam", ignoreCase = true)) {
                    showScamNotification(context, message)

                    val alertIntent = Intent(context, ScamPopupService::class.java).apply {
                        putExtra("sms_body", message)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(alertIntent)
                    } else {
                        context.startService(alertIntent)
                    }

                    Toast.makeText(context, "🚨 Scam Notification Detected by AI!", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<SmsResponse>, t: Throwable) {
                Toast.makeText(context, "API error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                Log.e("SMSNotifyListener", "API Failure: ${t.localizedMessage}")
            }
        })
    }

    private fun showScamNotification(context: Context, message: String) {
        val channelId = "scam_alerts"
        val notificationId = 202

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Scam Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows scam alerts"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("⚠️ Scam Notification Detected")
            .setContentText(message.take(60) + "...")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }
}
