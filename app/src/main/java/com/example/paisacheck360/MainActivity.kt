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

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var database: DatabaseReference

    // Views
    private lateinit var viewAllVideosBtn: Button
    private lateinit var scamSummaryText: TextView
    private lateinit var wifiGuardBtn: LinearLayout
    private lateinit var checkLinkBtn: LinearLayout
    private lateinit var fraudNumberLookupBtn: LinearLayout
    private lateinit var appRiskScannerBtn: LinearLayout
    private lateinit var detailedReportBtn: Button
    private lateinit var profileBtn: ImageView
    private lateinit var deviceStatusBar: LinearLayout
    private lateinit var deviceStatusText: TextView
    private lateinit var threatCountText: TextView
    private lateinit var lastSyncText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // Bind all views from the layout
        bindViews()

        // Setup click listeners for all buttons
        setupClickListeners()

        // This listener is the core of the app's logic. It waits for the user
        // to be fully authenticated before trying to access the database.
        setupAuthStateListener()

        // Check for necessary app permissions after setup
        checkRequiredPermissions()
    }

    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // USER IS CONFIRMED - It is now 100% safe to access the database.
                val userId = user.uid
                // THE FIX: Point to the correct "profile" path, not "summary"
                database = FirebaseDatabase.getInstance().reference
                    .child("users")
                    .child(userId)
                    .child("profile")

                listenToFirebaseData() // Read from the correct location
                updateDeviceStatus(true)
            } else {
                // User is logged out.
                updateDeviceStatus(false)
            }
        }
    }

    // Renamed for clarity, as we are now reading profile data.
    private fun listenToFirebaseData() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    threatCountText.text = "Could not find user profile."
                    return
                }

                // Read the data that actually exists in the "profile" node
                val name = snapshot.child("name").getValue(String::class.java) ?: "Guest"
                val createdAtMillis = snapshot.child("created_at").getValue(Long::class.java) ?: 0
                val accountCreationDate = if(createdAtMillis > 0) {
                     SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(createdAtMillis))
                } else {
                    "--"
                }

                // Update the UI with the correct data
                threatCountText.text = "Welcome back, $name!"
                lastSyncText.text = "Account created: $accountCreationDate"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Firebase Read Error.", Toast.LENGTH_SHORT).show()
                updateDeviceStatus(false)
            }
        })
    }

    // Helper function to keep onCreate clean
    private fun bindViews() {
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

    // Helper function to keep onCreate clean
    private fun setupClickListeners() {
        profileBtn.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }

        // Placeholder toasts for features not yet implemented
        val comingSoon = { Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show() }
        viewAllVideosBtn.setOnClickListener { comingSoon() }
        wifiGuardBtn.setOnClickListener { comingSoon() }
        checkLinkBtn.setOnClickListener { comingSoon() }
        fraudNumberLookupBtn.setOnClickListener { comingSoon() }
        appRiskScannerBtn.setOnClickListener { comingSoon() }
        detailedReportBtn.setOnClickListener { comingSoon() }
    }

    private fun updateScamCount() {
        val prefs = getSharedPreferences("SecureBharatPrefs", Context.MODE_PRIVATE)
        val scamCount = prefs.getInt("scam_count", 0)
        scamSummaryText.text = "📅 Last 7 Days: $scamCount scams blocked"
    }

    private fun updateDeviceStatus(isConnected: Boolean) {
        if (isConnected) {
            deviceStatusBar.setBackgroundColor(0xFFE8F5E9.toInt())
            deviceStatusText.text = "🟢 Device Connected"
        } else {
            deviceStatusBar.setBackgroundColor(0xFFFFEBEE.toInt())
            deviceStatusText.text = "🔴 Device Not Connected. Restart app."
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    override fun onResume() {
        super.onResume()
        updateScamCount()
    }

    private fun checkRequiredPermissions() {
        if (!isNotificationServiceEnabled()) {
            Toast.makeText(this, "Please enable Notification Access for Secure Bharat", Toast.LENGTH_LONG).show()
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
}
