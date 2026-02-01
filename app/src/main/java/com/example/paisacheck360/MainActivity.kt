package com.example.paisacheck360

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var database: DatabaseReference

    // Views
    private lateinit var viewAllVideosBtn: ImageView
    private lateinit var scamCountText: TextView
    private lateinit var welcomeText: TextView
    private lateinit var wifiGuardBtn: LinearLayout
    private lateinit var checkLinkBtn: LinearLayout
    private lateinit var fraudNumberLookupBtn: LinearLayout
    private lateinit var appRiskScannerBtn: LinearLayout
    private lateinit var detailedReportBtn: ImageView
    private lateinit var profileBtn: ImageView
    private lateinit var threatsDetectedText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        bindViews()
        setupClickListeners()
        setupAuthStateListener()
        checkRequiredPermissions()
    }

    private fun bindViews() {
        viewAllVideosBtn = findViewById(R.id.viewAllVideos)
        scamCountText = findViewById(R.id.scam_count_text)
        welcomeText = findViewById(R.id.welcomeText)
        wifiGuardBtn = findViewById(R.id.wifi_guard)
        checkLinkBtn = findViewById(R.id.check_link)
        fraudNumberLookupBtn = findViewById(R.id.fraud_number_lookup)
        appRiskScannerBtn = findViewById(R.id.app_risk_scanner)
        detailedReportBtn = findViewById(R.id.detailed_report)
        profileBtn = findViewById(R.id.profileBtn)
        threatsDetectedText = findViewById(R.id.threats_detected_text)
    }

    private fun setupClickListeners() {
        profileBtn.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }

        val comingSoon = { Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show() }

        viewAllVideosBtn.setOnClickListener { comingSoon() }
        wifiGuardBtn.setOnClickListener { comingSoon() }
        checkLinkBtn.setOnClickListener { comingSoon() }
        fraudNumberLookupBtn.setOnClickListener { comingSoon() }
        appRiskScannerBtn.setOnClickListener { comingSoon() }
        detailedReportBtn.setOnClickListener { comingSoon() }
    }

    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                database = FirebaseDatabase.getInstance().reference
                    .child("users").child(user.uid).child("profile")
                listenToFirebaseData()
            } else {
                welcomeText.text = "Welcome back, Guest!"
            }
        }
    }

    private fun listenToFirebaseData() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    welcomeText.text = "Welcome back, Guest!"
                    return
                }
                val name = snapshot.child("name").getValue(String::class.java) ?: "Guest"
                welcomeText.text = "Welcome back, $name 👋"
            }
            override fun onCancelled(error: DatabaseError) {
                welcomeText.text = "Welcome back, Guest!"
            }
        })
    }

    private fun updateScamCount() {
        val prefs = getSharedPreferences("SecureBharatPrefs", Context.MODE_PRIVATE)
        val scamCount = prefs.getInt("scam_count", 0)
        scamCountText.text = scamCount.toString()
        threatsDetectedText.text = scamCount.toString() // Corrected: No Emoji!
    }

    private fun checkRequiredPermissions() {
        if (!isNotificationServiceEnabled()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) ?: false
    }

    override fun onStart() { super.onStart(); auth.addAuthStateListener(authStateListener) }
    override fun onStop() { super.onStop(); auth.removeAuthStateListener(authStateListener) }
    override fun onResume() { super.onResume(); updateScamCount() }
}
