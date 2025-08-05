package com.example.paisacheck360

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SMSNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "SMSNotificationListener"
        private const val API_URL = "https://backend-k0ri.onrender.com/predict"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🟢 Service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        Log.d(TAG, "🔴 Service destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName
        val message = extractMessageText(sbn) ?: return
        val sender = extractSenderInfo(sbn)

        // Filter: only SMS-like apps + meaningful text
        if (!isSMSLikeApp(packageName)) return
        if (message.isBlank() || message.length < 10) return

        Log.d(TAG, "📩 SMS Notification from $packageName: $message")

        scope.launch {
            checkWithAPI(message, sender)
        }
    }

    private fun extractMessageText(sbn: StatusBarNotification): String? {
        val extras = sbn.notification.extras
        return extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
    }

    private fun extractSenderInfo(sbn: StatusBarNotification): String {
        val extras = sbn.notification.extras
        return extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: "Unknown Sender"
    }

    private fun isSMSLikeApp(pkg: String): Boolean {
        val smsApps = listOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.android.messaging",
            "com.sonyericsson.conversations",
        )
        return smsApps.any { pkg.contains(it, ignoreCase = true) }
    }

    private suspend fun checkWithAPI(message: String, sender: String) {
        try {
            val json = JSONObject().apply {
                put("message", message)
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            val isScam = if (response.isSuccessful && responseBody != null) {
                val obj = JSONObject(responseBody)
                obj.optBoolean("is_scam", false).also {
                    Log.d(TAG, "🤖 API Prediction Result: $it")
                }
            } else {
                false
            }

            if (isScam || containsScamKeywords(message)) {
                Log.d(TAG, "🚨 Scam Detected: $message")
                withContext(Dispatchers.Main) {
                    triggerScamAlert(message, sender)
                }
            } else {
                Log.d(TAG, "✅ Not a scam")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ API Error", e)

            // fallback keyword check
            if (containsScamKeywords(message)) {
                Log.w(TAG, "⚠️ Fallback keyword scam match")
                withContext(Dispatchers.Main) {
                    triggerScamAlert(message, sender)
                }
            }
        }
    }

    private fun containsScamKeywords(text: String): Boolean {
        val keywords = listOf(
            "won", "free", "prize", "lottery", "urgent", "verify", "click", "claim", "refund",
            "blocked", "password", "credit", "debit", "account", "gift", "reward"
        )
        val lower = text.lowercase()
        return keywords.count { lower.contains(it) } >= 2
    }

    private fun triggerScamAlert(message: String, sender: String) {
        try {
            val intent = Intent(this, ScamPopupService::class.java).apply {
                putExtra("message", message)
                putExtra("sender", sender)
                putExtra("timestamp", System.currentTimeMillis())
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            Log.d(TAG, "🔔 Scam popup triggered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ScamPopupService", e)
        }
    }
}
