package com.example.paisacheck360

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var database: DatabaseReference

    // Views
    private lateinit var viewAllVideosBtn: ImageView // Changed to ImageView to match XML
    private lateinit var scamSummaryText: TextView
    private lateinit var wifiGuardBtn: LinearLayout
    private lateinit var checkLinkBtn: LinearLayout
    private lateinit var fraudNumberLookupBtn: LinearLayout
    private lateinit var appRiskScannerBtn: LinearLayout
    private lateinit var detailedReportBtn: ImageView // Changed to ImageView to match XML
    private lateinit var profileBtn: ImageView
    private lateinit var deviceStatusBar: RelativeLayout // Changed to RelativeLayout to match XML
    private lateinit var deviceStatusText: TextView
    private lateinit var threatCountText: TextView
    private lateinit var lastSyncText: TextView

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
        // Matches the IDs added to activity_main.xml
        viewAllVideosBtn = findViewById(R.id.viewAllVideos)
        scamSummaryText = findViewById(R.id.scam_count_text)
        wifiGuardBtn = findViewById(R.id.wifi_guard)
        checkLinkBtn = findViewById(R.id.check_link)
        fraudNumberLookupBtn = findViewById(R.id.fraud_number_lookup)
        appRiskScannerBtn = findViewById(R.id.app_risk_scanner)
        detailedReportBtn = findViewById(R.id.detailed_report)
        profileBtn = findViewById(R.id.profileBtn)
        deviceStatusBar = findViewById(R.id.deviceStatusBar)
        deviceStatusText = findViewById(R.id.deviceStatusText)
        threatCountText = findViewById(R.id.threatCountText)
        lastSyncText = findViewById(R.id.lastSyncText)
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
                updateDeviceStatus(true)
            } else {
                updateDeviceStatus(false)
            }
        }
    }

    private fun listenToFirebaseData() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    threatCountText.text = "Welcome back, Guest!"
                    return
                }
                val name = snapshot.child("name").getValue(String::class.java) ?: "Guest"
                threatCountText.text = "Welcome back, $name!"
                // You can add more profile logic here if needed
            }
            override fun onCancelled(error: DatabaseError) {
                updateDeviceStatus(false)
            }
        })
    }

    private fun updateScamCount() {
        val prefs = getSharedPreferences("SecureBharatPrefs", Context.MODE_PRIVATE)
        val scamCount = prefs.getInt("scam_count", 0)
        scamSummaryText.text = scamCount.toString()
    }

    private fun updateDeviceStatus(isConnected: Boolean) {
        if (isConnected) {
            deviceStatusText.text = "Connected to Secure Anti-Virus"
        } else {
            deviceStatusText.text = "🔴 Not Protected. Sign in."
        }
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