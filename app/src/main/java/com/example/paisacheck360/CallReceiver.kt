package com.example.paisacheck360

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            if (state == TelephonyManager.EXTRA_STATE_RINGING && number != null) {
                Log.d("CallReceiver", "Incoming call from: $number")

                // This starts the ACTUAL service
                val serviceIntent = Intent(context, CallPopupService::class.java)
                serviceIntent.putExtra("callerNumber", number)
                context.startService(serviceIntent)
            }
        }
    }
}