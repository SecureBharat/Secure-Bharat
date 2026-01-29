package com.example.paisacheck360

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SMSNotificationListener : NotificationListenerService() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val apiUrl = "https://backend-k0ri.onrender.com/predict"

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("SMSNotification", "✅ Notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkg = sbn.packageName ?: "unknown"
        val title = sbn.notification.extras.getString("android.title") ?: "Unknown"
        val text = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: ""

        // 1. Filter out system background messages immediately
        if (text.isBlank() || text.contains("doing work in the background")) return

        // 2. Identify if the app is a Messaging/SMS app
        val isSmsApp = pkg.contains("messaging", true) ||
                pkg.contains("sms", true) ||
                pkg.contains("whatsapp", true)

        if (isSmsApp) {
            val risk = analyzeLocalRisk(text)

            // 3. 🔥 RISK FILTER: Only act if it's NOT a Low risk (normal) message
            if (risk != "Low") {
                Log.d("SMSNotification", "🚨 Potential Scam Detected: $text")

                // Save alert to Firebase immediately to update Dashboard counter
                saveAlertToFirebase(title, text, risk)

                // Trigger the visual alert popup
                triggerPopup(title, text, risk)

                // Run background AI check for extra verification
                runApiCheck(title, text)
            } else {
                Log.d("SMSNotification", "✅ Normal message ignored: $text")
            }
        }
    }

    /**
     * Local keyword analysis to distinguish between "Hi" and a Scam.
     */
    private fun analyzeLocalRisk(msg: String): String {
        val m = msg.lowercase()

        // High Risk: Direct threats or financial fraud triggers
        val highRiskKeywords = listOf("loan", "kyc", "blocked", "suspend", "otp", "verify", "pancard", "electricity")

        // Medium Risk: Lure tactics
        val mediumRiskKeywords = listOf("win", "congrats", "offer", "prize", "gift", "click here")

        return when {
            highRiskKeywords.any { m.contains(it) } -> "High"
            mediumRiskKeywords.any { m.contains(it) } -> "Medium"
            else -> "Low"
        }
    }

    private fun saveAlertToFirebase(sender: String, message: String, risk: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseDatabase.getInstance().reference.child("users").child(user.uid).child("alerts")
        val alertId = db.push().key ?: System.currentTimeMillis().toString()

        val payload = mapOf(
            "sender" to sender,
            "body" to message,
            "riskLevel" to risk, // Matches MainActivity key
            "timestamp" to System.currentTimeMillis()
        )

        db.child(alertId).setValue(payload)
            .addOnSuccessListener { Log.d("SMSNotification", "✅ Alert logged to Firebase") }
    }

    private fun runApiCheck(sender: String, message: String) {
        val json = JSONObject().apply { put("message", message) }
        val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
        val req = Request.Builder().url(apiUrl).post(body).build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SMSNotification", "❌ API Timeout/Failure")
            }

            override fun onResponse(call: Call, response: Response) {
                val resStr = response.body?.string()
                try {
                    val jsonRes = JSONObject(resStr ?: "{}")
                    // If AI finds a scam that local keywords missed, trigger popup
                    if (jsonRes.optBoolean("is_scam", false)) {
                        triggerPopup(sender, message, "High")
                    }
                } catch (e: Exception) {
                    Log.e("SMSNotification", "⚠️ API Parse Error")
                }
            }
        })
    }

    private fun triggerPopup(sender: String, message: String, risk: String) {
        val i = Intent(applicationContext, ScamPopupService::class.java).apply {
            putExtra("sender", sender)
            putExtra("body", message)
            putExtra("risk", risk)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startService(i)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}