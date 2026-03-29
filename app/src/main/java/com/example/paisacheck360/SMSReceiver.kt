package com.example.paisacheck360

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log

class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // High-priority log to see if the system even tries to wake us
        Log.i("SecureBharat-SMS", "WAKEUP: Received action ${intent.action}")

        // Handle both standard SMS and the newer "DATA_SMS" type
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION ||
            intent.action == "android.intent.action.DATA_SMS_RECEIVED") {

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.displayOriginatingAddress ?: "Unknown"
                val body = sms.displayMessageBody ?: ""

                Log.d("SecureBharat-SMS", "Detected Message from: $sender")

                // If it's a scam, we MUST use a foreground service to bypass the freeze
                if (isScam(body)) {
                    val popupIntent = Intent(context, ScamPopupService::class.java).apply {
                        putExtra("sender", sender)
                        putExtra("body", body)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(popupIntent)
                    } else {
                        context.startService(popupIntent)
                    }
                }
            }
        }
    }

    private fun isScam(msg: String): Boolean {
        val lower = msg.lowercase()
        return lower.contains("win") || lower.contains("bank") || lower.contains("otp") || lower.contains("kyc")
    }
}