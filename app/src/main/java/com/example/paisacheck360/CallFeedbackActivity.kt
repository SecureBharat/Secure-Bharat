package com.example.paisacheck360

import android.os.Bundle
import android.provider.Settings // ❗️ ADDED
import android.util.Log // ❗️ ADDED
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
// import com.google.firebase.auth.FirebaseAuth // ❗️ REMOVED
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class CallFeedbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_feedback)

        val callerNumber = intent.getStringExtra("callerNumber") ?: "Unknown"
        val txtNumber = findViewById<TextView>(R.id.txtNumber)
        val btnScam = findViewById<Button>(R.id.btnScam)
        val btnSafe = findViewById<Button>(R.id.btnSafe)

        txtNumber.text = "Incoming Call: $callerNumber"

        btnScam.setOnClickListener {
            saveFeedback(callerNumber, "Scam") // Changed to "Scam" to match summary
            finish()
        }

        btnSafe.setOnClickListener {
            saveFeedback(callerNumber, "Safe") // Changed to "Safe" to match summary
            finish()
        }
    }

    private fun saveFeedback(number: String, feedback: String) {
        // ❗️ FIX: Use the Android ID, just like in your LoginActivity
        val androidID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        if (androidID.isNullOrEmpty()) {
            Log.e("CallFeedback", "Could not get Android ID")
            return // Can't save without an ID
        }

        // Make sure to use your specific database URL
        val db = FirebaseDatabase.getInstance("https://sbtest-9acea-default-rtdb.firebaseio.com").reference
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // Data structure from your summary
        val callData = mapOf(
            "status" to feedback,
            "timestamp" to timestamp
        )

        // Sanitize the phone number to use as a Firebase key (replace invalid characters)
        val safeNumberKey = number.replace(Regex("[.#$\\[\\]]"), "_")

        // Database path from your summary: users/{androidID}/call_feedback/{phone_number}
        db.child("users").child(androidID).child("call_feedback").child(safeNumberKey).setValue(callData)
            .addOnSuccessListener {
                Log.d("CallFeedback", "Feedback saved successfully")
            }
            .addOnFailureListener {
                Log.e("CallFeedback", "Failed to save feedback", it)
            }
    }
}