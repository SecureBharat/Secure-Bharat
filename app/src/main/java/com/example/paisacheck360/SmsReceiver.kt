package com.example.paisacheck360

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.widget.Toast

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val bundle: Bundle? = intent?.extras

        val pdus = bundle?.get("pdus") as? Array<*>
        pdus?.forEach { pdu ->
            val format = bundle?.getString("format")
            val message = SmsMessage.createFromPdu(pdu as ByteArray, format)
            val sender = message.originatingAddress
            val body = message.messageBody

            if (context != null && body != null) {
                val result = ScamDetector.checkForScam(body)
                Toast.makeText(context, "📩 From: $sender\nStatus: $result", Toast.LENGTH_LONG).show()
            }
        }
    }
}
