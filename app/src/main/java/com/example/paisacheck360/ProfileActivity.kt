package com.example.paisacheck360

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        val nameTextView = findViewById<TextView>(R.id.nameTextView)
        val emailTextView = findViewById<TextView>(R.id.emailTextView)
        val uidTextView = findViewById<TextView>(R.id.uidTextView)
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        if (user != null) {
            nameTextView.text = user.displayName
            emailTextView.text = user.email
            uidTextView.text = "UID: ${user.uid}"
        } else {
            // Should not happen if app is designed correctly
            Toast.makeText(this, "Error: No user logged in", Toast.LENGTH_SHORT).show()
            finish()
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
