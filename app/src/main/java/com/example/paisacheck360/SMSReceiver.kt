package com.example.paisacheck360

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*
import java.util.regex.Pattern

class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {

            val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (msg in msgs) {
                val sender = msg.displayOriginatingAddress ?: "Unknown"
                val body = msg.displayMessageBody ?: ""

                Log.d("SMSReceiver", "📩 SMS From: $sender\n$body")

                // 1️⃣ Scam text analysis
                val analysis = analyzeMessage(sender, body)

                // 2️⃣ URL extraction & rule-based scan
                val urls = UrlAnalyzer.extractUrls(body)

                for (url in urls) {
                    val urlResult = UrlAnalyzer.analyzeUrl(url)

                    Log.d(
                        "SMSReceiver",
                        "🌐 URL detected: ${urlResult.url} | Level=${urlResult.level} | Reasons=${urlResult.reasons}"
                    )

                    // ⭐ IMPORTANT — Register the URL for automatic blocking
                    UrlBlocker.registerIncomingUrl(urlResult)

                    // 3️⃣ Show notification if needed
                    if (urlResult.level != UrlAnalyzer.RiskLevel.SAFE) {
                        NotificationUtils.showUrlAlertNotification(
                            context = context,
                            notificationId = System.currentTimeMillis().toInt(),
                            title = if (urlResult.level == UrlAnalyzer.RiskLevel.DANGEROUS)
                                "🚨 Dangerous link detected"
                            else
                                "⚠️ Suspicious link detected",
                            text = "Message from $sender contains a risky link:\n${urlResult.url}",
                            url = urlResult.url,
                            reasons = urlResult.reasons
                        )
                    }
                }

                // 4️⃣ Save analysis to Firebase
                saveToFirebase(sender, body, analysis)

                // 5️⃣ Show scam popup
                startPopup(context, sender, body, analysis)
            }
        }
    }

    /** Popup service starter */
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(i)
            else
                context.startService(i)
        } catch (e: Exception) {
            Log.e("SMSReceiver", "Popup service failed: ${e.message}")
        }
    }

    /** Save scam info to Firebase */
    private fun saveToFirebase(sender: String, body: String, analysis: MessageAnalysis) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        val ref = FirebaseDatabase.getInstance().getReference("users/$uid/alerts")
        val id = ref.push().key ?: System.currentTimeMillis().toString()

        val data = mapOf(
            "sender" to sender,
            "body" to body,
            "intent" to analysis.intent,
            "tone" to analysis.tone,
            "risk" to analysis.risk,
            "suggestedReplies" to analysis.suggestedReplies,
            "timestamp" to System.currentTimeMillis()
        )

        ref.child(id).setValue(data)
    }

    /** Scam analyzer (unchanged) */
    private fun analyzeMessage(sender: String, message: String): MessageAnalysis {
        val m = message.lowercase(Locale.getDefault()).trim()

        val otpPattern = Pattern.compile("""\b(\d{4,8})\b""")
        val urlPattern = Pattern.compile("""https?://\S+|\bbit\.ly/\S+|\bgoo\.gl/\S+""", Pattern.CASE_INSENSITIVE)
        val upiPattern = Pattern.compile("""[a-zA-Z0-9.\-_]{3,}@[a-zA-Z]{2,}""")
        val moneyPattern = Pattern.compile("""\b(rs\.?|inr|₹)\s?[\d,]+""", Pattern.CASE_INSENSITIVE)

        val transactionWords = listOf("debited","credited","txn","transaction","payment","transfer","paid","withdrawn")
        val urgentWords = listOf("urgent","verify now","account blocked","click link","update account","kyc")
        val rewardWords = listOf("lottery","prize","claim","free","congratulations")
        val greetingWords = listOf("hello","hi","hey","wassup","are you","wanna","come")

        val isShortSender = sender.length <= 8
        val isNumericSender = sender.matches(Regex("^[0-9+\\- ]{3,}$"))

        val intent = when {
            otpPattern.matcher(m).find() && m.contains("otp") -> "OTP"
            transactionWords.any { m.contains(it) } -> "TRANSACTION"
            urlPattern.matcher(m).find() || rewardWords.any { m.contains(it) } -> "POTENTIAL_SCAM"
            greetingWords.any { m.contains(it) } -> "CHAT"
            upiPattern.matcher(m).find() -> "UPI_ID"
            else -> "UNKNOWN"
        }

        val tone = when {
            urgentWords.any { m.contains(it) } -> "urgent"
            rewardWords.any { m.contains(it) } -> "tempting"
            m.contains("thank") -> "calm"
            else -> "neutral"
        }

        val risk = when {
            intent == "POTENTIAL_SCAM" -> "High"
            intent == "TRANSACTION" && isNumericSender -> "Medium"
            intent == "OTP" -> "Low"
            isNumericSender -> "Suspicious"
            else -> "Safe"
        }

        val suggested = when (intent) {
            "OTP" -> listOf("Copy OTP", "Ignore")
            "TRANSACTION" -> listOf("I didn't do this", "Check bank", "Report")
            "POTENTIAL_SCAM" -> listOf("Report", "Block", "Ignore")
            "CHAT" -> listOf("Yes", "No", "Call you")
            else -> listOf("OK", "Thanks")
        }

        return MessageAnalysis(intent, intent, tone, risk, suggested)
    }
}

data class MessageAnalysis(
    val category: String,
    val intent: String,
    val tone: String,
    val risk: String,
    val suggestedReplies: List<String>
)
