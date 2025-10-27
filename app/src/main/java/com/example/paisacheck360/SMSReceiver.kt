package com.example.paisacheck360

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale
import java.util.ArrayList
import java.util.regex.Pattern

/**
 * SMSReceiver — receives sms intents, runs lightweight context analysis,
 * saves structured record to Firebase and starts ScamPopupService with suggested replies.
 */
class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (msg in msgs) {
                val sender = msg.displayOriginatingAddress ?: "Unknown"
                val body = msg.displayMessageBody ?: ""
                Log.d("SMSReceiver", "📩 From: $sender | $body")

                // analyze
                val analysis = analyzeMessage(sender, body)
                Log.d("SMSReceiver", "🧠 analysis -> intent=${analysis.intent} risk=${analysis.risk} tone=${analysis.tone} replies=${analysis.suggestedReplies}")

                // save structured alert to Firebase
                saveToFirebase(sender, body, analysis)

                // start popup with suggested replies
                startPopup(context, sender, body, analysis)
            }
        }
    }

    private fun startPopup(context: Context, sender: String, body: String, analysis: MessageAnalysis) {
        val i = Intent(context, ScamPopupService::class.java).apply {
            putExtra("sender", sender)
            putExtra("body", body)
            putExtra("intent", analysis.intent)
            putExtra("tone", analysis.tone)
            putExtra("risk", analysis.risk)
            putStringArrayListExtra("suggestedReplies", ArrayList(analysis.suggestedReplies))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startService(i)
        } catch (ise: IllegalStateException) {
            // Android O+ might require startForegroundService
            try { context.startForegroundService(i) } catch (t: Throwable) { Log.w("SMSReceiver","start service failed: ${t.message}") }
        }
    }

    private fun saveToFirebase(sender: String, body: String, analysis: MessageAnalysis) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        val db = FirebaseDatabase.getInstance().getReference("users/$uid/alerts")
        val id = db.push().key ?: System.currentTimeMillis().toString()
        val payload = mapOf(
            "sender" to sender,
            "body" to body,
            "intent" to analysis.intent,
            "tone" to analysis.tone,
            "risk" to analysis.risk,
            "suggestedReplies" to analysis.suggestedReplies,
            "timestamp" to System.currentTimeMillis()
        )
        db.child(id).setValue(payload)
            .addOnSuccessListener { Log.d("SMSReceiver","Saved alert to Firebase ($id)") }
            .addOnFailureListener { e -> Log.e("SMSReceiver","Firebase save failed: ${e.message}") }
    }

    // Lightweight NLP-like analyzer: keywords + regex + heuristics
    private fun analyzeMessage(sender: String, message: String): MessageAnalysis {
        val m = message.lowercase(Locale.getDefault()).trim()

        // regex patterns
        val otpPattern = Pattern.compile("""\b(\d{4,8})\b""")
        val urlPattern = Pattern.compile("""https?://\S+|\bbit\.ly/\S+|\bgoo\.gl/\S+""", Pattern.CASE_INSENSITIVE)
        val upiPattern = Pattern.compile("""[a-zA-Z0-9.\-_]{3,}@[a-zA-Z]{2,}""")
        val moneyPattern = Pattern.compile("""\b(rs\.?|inr|₹)\s?[\d,]+""", Pattern.CASE_INSENSITIVE)

        // word-lists (Indian-context tuned)
        val transactionWords = listOf("debited","credited","txn","transaction","payment","transfer","paid","withdrawn")
        val urgentWords = listOf("urgent","verify now","verify","account blocked","blocked","suspend","immediately","confirm now","click link","update now","update account","kyc")
        val rewardWords = listOf("lottery","won","winner","prize","reward","claim","free","congratulations")
        val greetingWords = listOf("hello","hi","hey","wassup","are you","let's","wanna","come")

        // Sender heuristics
        val isShortSender = sender.length <= 8
        val isNumericSender = sender.matches(Regex("^[0-9+\\- ]{3,}$"))

        // Determine intent
        val intent = when {
            otpPattern.matcher(m).find() && (m.contains("otp") || m.contains("one time") || m.contains("verification") || m.contains("code")) -> "OTP"
            transactionWords.any { m.contains(it) } || moneyPattern.matcher(m).find() || m.contains("upi") -> "TRANSACTION"
            urlPattern.matcher(m).find() || rewardWords.any { m.contains(it) } || (m.contains("click") && m.contains("link")) -> "POTENTIAL_SCAM"
            greetingWords.any { m.contains(it) } || m.contains("?") && m.length < 120 -> "CHAT"
            upiPattern.matcher(m).find() -> "UPI_ID"
            else -> "UNKNOWN"
        }

        // Tone detection
        val tone = when {
            urgentWords.any { m.contains(it) } -> "urgent"
            rewardWords.any { m.contains(it) } -> "tempting"
            m.contains("thank") || m.contains("ok") -> "calm"
            else -> "neutral"
        }

        // Risk scoring (simple heuristic)
        val risk = when {
            intent == "POTENTIAL_SCAM" && (urlPattern.matcher(m).find() || rewardWords.any { m.contains(it) }) -> "High"
            intent == "TRANSACTION" && (isShortSender || isNumericSender) && m.contains("debited") -> "Medium"
            intent == "OTP" -> "Low"
            isNumericSender && intent == "UNKNOWN" -> "Suspicious"
            intent == "CHAT" -> "Safe"
            else -> "Safe"
        }

        // Suggested replies (context-aware)
        val suggested = mutableListOf<String>()
        when (intent) {
            "CHAT" -> {
                // try to be conversational: inspect message for tokens
                if (m.contains("come") || m.contains("meet") || m.contains("lunch") || m.contains("dinner") || m.contains("now")) {
                    suggested.addAll(listOf("Yes", "No", "Maybe later"))
                } else if (m.contains("?") || m.contains("you free") || m.contains("plan")) {
                    suggested.addAll(listOf("Yes", "Not now", "Call you"))
                } else {
                    suggested.addAll(listOf("Okay", "On my way", "Can't"))
                }
            }
            "OTP" -> {
                val found = otpPattern.matcher(m)
                val otp = if (found.find()) found.group(1) else null
                suggested.add(if (!otp.isNullOrEmpty()) "Copy OTP: $otp" else "Copy OTP")
                suggested.add("Ignore")
            }
            "TRANSACTION" -> {
                suggested.addAll(listOf("I didn't do this","Check bank","Report if wrong"))
            }
            "UPI_ID" -> {
                suggested.addAll(listOf("Block UPI ID","Report","Ignore"))
            }
            "POTENTIAL_SCAM" -> {
                suggested.addAll(listOf("Report","Block","Ignore"))
            }
            else -> {
                suggested.addAll(listOf("OK","Thanks"))
            }
        }

        // dedupe and limit to 4 suggestions
        val finalReplies = suggested.distinct().take(4)

        return MessageAnalysis(category = intent, intent = intent, tone = tone, risk = risk, suggestedReplies = finalReplies)
    }
}

/** Simple data holder returned by analyzer */
data class MessageAnalysis(
    val category: String,
    val intent: String,
    val tone: String,
    val risk: String,
    val suggestedReplies: List<String>
)
