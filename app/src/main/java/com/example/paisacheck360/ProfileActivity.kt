package com.example.paisacheck360

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // 🔐 CHECK CUSTOM LOGIN (NOT FIREBASE)
        val prefs = getSharedPreferences("SecureBharatPrefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)

        if (!isLoggedIn) {
            // User not logged in → go to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // ===== UI =====
        val androidIdText = findViewById<TextView>(R.id.nameTextView)
        val logoutBtn = findViewById<Button>(R.id.logoutButton)

        // Show Android ID (Secure ID)
        val androidID = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        )
        androidIdText.text = "Secure ID: $androidID"

        // 🚪 MANUAL LOGOUT ONLY
        logoutBtn.setOnClickListener {
            prefs.edit()
                .putBoolean("is_logged_in", false)
                .apply()

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
