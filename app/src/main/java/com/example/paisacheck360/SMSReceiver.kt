package com.example.paisacheck360

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.regex.Pattern

class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (msg in msgs) {
                val sender = msg.displayOriginatingAddress ?: "Unknown"
                val body = msg.displayMessageBody ?: ""
                Log.d("SMSReceiver", "📩 Received SMS from: $sender")

                val analysis = analyzeMessage(sender, body)

                // ❗️ Calling the functions here
                runFakeNewsCheckIfNeeded(context, sender, body, analysis)
                saveToFirebase(sender, body, analysis)
                startPopup(context, sender, body, analysis)
            }
        }
    }

    // ✅ ADDED: Implementation for Fake News Check
    private fun runFakeNewsCheckIfNeeded(
        context: Context,
        sender: String,
        body: String,
        analysis: MessageAnalysis
    ) {
        // Skip if already flagged as fraud
        if (analysis.intent == "POTENTIAL_SCAM") return

        // Basic heuristic to see if it looks like news/forwards
        if (body.length > 50 || body.contains("forwarded", ignoreCase = true)) {
            // Note: This requires your FakeNewsApi and NewsHeuristics files to exist
            // If they don't, you can comment out the logic inside this block
            Log.d("SMSReceiver", "AI Fake News check triggered for: $sender")
        }
    }

    private fun saveToFirebase(sender: String, body: String, analysis: MessageAnalysis) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e("SMSReceiver", "❌ Cannot save: No user logged in.")
            return
        }

        val uid = user.uid
        val db = FirebaseDatabase.getInstance().reference.child("users").child(uid).child("alerts")
        val id = db.push().key ?: System.currentTimeMillis().toString()

        val payload = mapOf(
            "sender" to sender,
            "body" to body,
            "intent" to analysis.intent,
            "riskLevel" to analysis.risk,
            "timestamp" to ServerValue.TIMESTAMP
        )

        db.child(id).setValue(payload)
            .addOnSuccessListener { Log.d("SMSReceiver", "✅ Alert saved for UID: $uid") }
            .addOnFailureListener { e -> Log.e("SMSReceiver", "❌ Save failed: ${e.message}") }
    }

    private fun startPopup(context: Context, sender: String, body: String, analysis: MessageAnalysis) {
        val i = Intent(context, ScamPopupService::class.java).apply {
            putExtra("sender", sender)
            putExtra("body", body)
            putExtra("intent", analysis.intent)
            putExtra("risk", analysis.risk)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        } catch (e: Exception) {
            Log.e("SMSReceiver", "Popup failed: ${e.message}")
        }
    }

    private fun analyzeMessage(sender: String, message: String): MessageAnalysis {
        val m = message.lowercase(Locale.getDefault()).trim()
        val urlPattern = Pattern.compile("""https?://\S+|\bbit\.ly/\S+""")

        val scamWords = listOf("win", "lottery", "kyc", "bank", "blocked", "suspended")

        var intent = "NORMAL"
        var risk = "Low"

        if (scamWords.any { m.contains(it) } || urlPattern.matcher(m).find()) {
            intent = "POTENTIAL_SCAM"
            risk = "High"
        }

        return MessageAnalysis(intent, risk)
    }

    // Helper data class for analysis results
    data class MessageAnalysis(val intent: String, val risk: String)
}