package com.example.paisacheck360

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // ✅ USE FIREBASE AUTH (NOT SharedPreferences)
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val nameTextView = findViewById<TextView>(R.id.nameTextView)
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        val androidID = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        )

        nameTextView.text = "Secure ID: $androidID"

        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
