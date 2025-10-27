package com.example.paisacheck360

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.PHONE_STATE") {
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        Log.d("CallReceiver", "Phone State: $state")

        // Get SharedPreferences to store the number during the call
        val prefs = context.getSharedPreferences("CallTracker", Context.MODE_PRIVATE)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // 1. CALL IS RINGING: Save the number
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                // We save the number (even if it's null) to know a call was in progress
                prefs.edit().putString("incomingCallNumber", incomingNumber).apply()
                Log.d("CallReceiver", "RINGING: Saved number $incomingNumber")
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                // 2. CALL IS IDLE (ENDED): Check if we were tracking a call

                // Check if the "incomingCallNumber" key exists
                if (prefs.contains("incomingCallNumber")) {
                    // Get the number we saved
                    val trackedNumber = prefs.getString("incomingCallNumber", null)

                    Log.d("CallReceiver", "IDLE: Call ended. Showing popup for $trackedNumber")

                    // 3. SHOW THE POPUP with the saved number
                    val popupIntent = Intent(context, CallFeedbackActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra("callerNumber", trackedNumber ?: "Unknown")
                    }
                    context.startActivity(popupIntent)

                    // 4. CLEAN UP: Remove the number so we don't show the popup again
                    prefs.edit().remove("incomingCallNumber").apply()
                }
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call was answered. We don't need to do anything here.
                // We will just wait for the call to become IDLE.
                Log.d("CallReceiver", "OFFHOOK: Call answered.")
            }
        }
    }
}