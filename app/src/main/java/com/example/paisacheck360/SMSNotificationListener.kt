import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.content.Intent
import android.os.Build
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.paisacheck360.network.RetrofitClient
import com.example.paisacheck360.network.SmsRequest
import com.example.paisacheck360.ScamPopupService
import com.example.paisacheck360.network.SmsResponse

class SMSNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        Log.d("SMSNotifyListener", "Notif from: $packageName | Title: $title | Text: $text")

        // Send text to NLP backend
        val request = SmsRequest(text)
        val call = RetrofitClient.api.predictSms(request)

        call.enqueue(object : Callback<SmsResponse> {
            override fun onResponse(call: Call<SmsResponse>, response: Response<SmsResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d("SMSNotifyListener", "🧠 NLP Result: $result")

                    if (result != null && result.label.lowercase() == "scam") {
                        // 🔔 Show scam warning popup
                        val alertIntent = Intent(this@SMSNotificationListener, ScamPopupService::class.java).apply {
                            putExtra("sms_body", "$title: $text")
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(alertIntent)
                        } else {
                            startService(alertIntent)
                        }
                        Log.d("SMSNotifyListener", "🚨 Scam Detected via NLP!")
                    }
                }
            }

            override fun onFailure(call: Call<SmsResponse>, t: Throwable) {
                Log.e("SMSNotifyListener", "NLP API call failed", t)
            }
        })
    }
}
