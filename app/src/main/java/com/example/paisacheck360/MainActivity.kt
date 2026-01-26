package com.example.paisacheck360

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewAllVideosBtn: Button
    private lateinit var scamSummaryText: TextView

    private lateinit var wifiGuardBtn: LinearLayout
    private lateinit var checkLinkBtn: LinearLayout
    private lateinit var fraudNumberLookupBtn: LinearLayout
    private lateinit var appRiskScannerBtn: LinearLayout
    private lateinit var detailedReportBtn: Button

    private lateinit var profileBtn: ImageView

    // 🔥 Device Status
    private lateinit var deviceStatusBar: LinearLayout
    private lateinit var deviceStatusText: TextView

    // 🔥 Live Stats
    private lateinit var threatCountText: TextView
    private lateinit var lastSyncText: TextView

    // 🔥 Firebase
    private lateinit var database: DatabaseReference

    // 👉 YOUR REAL SID (from screenshot)
    private val deviceSID = "nDvU7cLlHc8cRKDEvt1HJCt0Zk2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ===== Bind Views =====
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

        // ===== Firebase Path =====
        database = FirebaseDatabase.getInstance()
            .reference
            .child(deviceSID)
            .child("summary")

        checkRequiredPermissions()
        listenToFirebaseSummary()

        // ===== Click Listeners =====

        viewAllVideosBtn.setOnClickListener {
            Toast.makeText(this, "Security videos coming soon", Toast.LENGTH_SHORT).show()
        }

        wifiGuardBtn.setOnClickListener {
            Toast.makeText(this, "Wifi Guard coming soon", Toast.LENGTH_SHORT).show()
        }

        checkLinkBtn.setOnClickListener {
            Toast.makeText(this, "Link check coming soon", Toast.LENGTH_SHORT).show()
        }

        fraudNumberLookupBtn.setOnClickListener {
            Toast.makeText(this, "Fraud number lookup coming soon", Toast.LENGTH_SHORT).show()
        }

        appRiskScannerBtn.setOnClickListener {
            Toast.makeText(this, "App risk scanner coming soon", Toast.LENGTH_SHORT).show()
        }

        detailedReportBtn.setOnClickListener {
            Toast.makeText(this, "Detailed report coming soon", Toast.LENGTH_SHORT).show()
        }

        profileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    // 🔥 LISTEN TO YOUR REAL SUMMARY NODE
    private fun listenToFirebaseSummary() {

        database.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                if (!snapshot.exists()) {
                    updateDeviceStatus(false)
                    return
                }

                val files = snapshot.child("files").getValue(Int::class.java) ?: 0
                val network = snapshot.child("network").getValue(Int::class.java) ?: 0
                val processes = snapshot.child("processes").getValue(Int::class.java) ?: 0
                val threats = snapshot.child("threats").getValue(Int::class.java) ?: 0
                val updatedAt = snapshot.child("updatedAt").getValue(String::class.java) ?: "--"

                updateDeviceStatus(true)

                threatCountText.text =
                    "🛡 Threats: $threats   📁 Files: $files\n🌐 Network: $network   ⚙ Processes: $processes"

                lastSyncText.text = "🕒 Last Sync: $updatedAt"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Firebase error", Toast.LENGTH_SHORT).show()
                updateDeviceStatus(false)
            }
        })
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
            deviceStatusText.text = "🔴 Device Not Connected"
        }
    }

    override fun onResume() {
        super.onResume()
        updateScamCount()
    }

    // ===== Permissions =====

    private fun checkRequiredPermissions() {

        if (!isNotificationServiceEnabled()) {
            Toast.makeText(this, "Enable Notification Access", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }
}
