package com.example.paisacheck360

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent


class SMSNotificationListener : NotificationListenerService() {

    private val client = OkHttpClient()
    private val gson = Gson()
    private val TAG = "SMSNotification"

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        // Filter for real SMS apps only (extend as needed)
        val smsApps = listOf(
            "com.google.android.apps.messaging", // Google Messages
            "com.android.mms",                   // AOSP
            "com.samsung.android.messaging",     // Samsung
            "com.miui.sms",                      // Xiaomi
            "com.oneplus.mms"                    // OnePlus
        )

        if (!smsApps.contains(pkg)) return

        Log.d(TAG, "📩 SMS notif from [$title]: $text")

        CoroutineScope(Dispatchers.IO).launch {
            checkWithAPI(title, text)
        }
    }

    private suspend fun checkWithAPI(sender: String, message: String) {
        try {
            val encoded = URLEncoder.encode(message, "UTF-8")
            val url = "https://backend-k0ri.onrender.com/predict?text=$encoded"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            // API returns { "label": "spam" | "ham", ... } (assumed)
            val isScam = body.contains("\"spam\"", ignoreCase = true)

            val hitByKeywords = containsScamKeywords(message)
            if (isScam || hitByKeywords) {
                val risk = if (containsHighRiskKeywords(message)) "High" else "Medium"
                Log.d(TAG, "🚨 Scam Detected. Risk=$risk | $message")

                // Save locally (single key used everywhere: "scam_logs")
                saveScamToLocal(sender, message, risk)

                withContext(Dispatchers.Main) {
                    triggerScamAlert(message, sender)
                }
            } else {
                Log.d(TAG, "✅ Looks safe (API+keywords).")
            }
        } catch (e: Exception) {
            Log.e(TAG, "API check failed: ${e.message}. Falling back to keywords.")
            val risk = when {
                containsHighRiskKeywords(message) -> "High"
                containsScamKeywords(message) -> "Medium"
                else -> null
            }
            if (risk != null) {
                saveScamToLocal(sender, message, risk)
                withContext(Dispatchers.Main) { triggerScamAlert(message, sender) }
            }
        }
    }

    private fun containsScamKeywords(text: String): Boolean {
        val keywords = listOf(
            // Money & Prize scams
            "lottery", "winner", "congratulations", "you won", "reward", "bonus", "prize",

            // Loan & KYC scams
            "loan", "instant loan", "personal loan", "low interest", "kyc", "account update", "update account",

            // OTP / Verification scams
            "otp", "one time password", "verify", "verification", "secure code", "blocked", "account blocked",

            // Urgency & Threats
            "urgent", "immediately", "final warning", "suspend", "deactivate", "limited time", "act now",

            // Links & Clickbait
            "click link", "tap link", "shorturl", "bit.ly", "tinyurl", "http://", "https://",

            // Bank & UPI fraud
            "bank", "ifsc", "upi", "gpay", "phonepe", "paytm", "yono", "sbi", "icici", "hdfc", "axis",
            "upi id", "money transfer", "refund", "claim now",

            // Government / Fake job scams
            "income tax", "pf refund", "govt scheme", "job offer", "work from home", "earn money fast",

            // Crypto / Investment scams
            "crypto", "bitcoin", "forex", "trading", "investment", "double your money",

            // Generic fraud words
            "scam", "fraud", "suspicious", "free gift", "scratch card", "lucky draw"
        )

        return keywords.any { text.contains(it, ignoreCase = true) }
    }


    private fun containsHighRiskKeywords(text: String): Boolean {
        val risky = listOf("password", "account blocked", "verify kyc", "urgent action", "otp now")
        val lower = text.lowercase()
        return risky.any { lower.contains(it) }
    }

    /**
     * APPENDS to SharedPreferences("SecureBharatPrefs") key "scam_logs" using Gson.
     * Structure matches ScamMessage.kt exactly.
     */
    private fun saveScamToLocal(sender: String, message: String, risk: String) {
        val prefs = getSharedPreferences("SecureBharatPrefs", MODE_PRIVATE)
        val existingJson = prefs.getString("scam_logs", "[]")

        val type = object : TypeToken<MutableList<ScamMessage>>() {}.type
        val list: MutableList<ScamMessage> = try {
            gson.fromJson(existingJson, type) ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }

        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        list.add(ScamMessage(sender = sender, text = message, date = now, riskLevel = risk))

        prefs.edit().putString("scam_logs", gson.toJson(list)).apply()
        Log.d(TAG, "💾 Saved scam. Total=${list.size}")
    }

    private fun triggerScamAlert(message: String, sender: String) {
        Log.d(TAG, "⚠️ Scam Alert popup -> from $sender")

        val intent = Intent(this, ScamPopupService::class.java).apply {
            putExtra("sender", sender)
            putExtra("message", message)
        }
        // Needed for service call from NotificationListener
        startService(intent)
    }

}