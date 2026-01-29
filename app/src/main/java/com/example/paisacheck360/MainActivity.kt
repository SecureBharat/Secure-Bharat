package com.example.paisacheck360

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var profileDatabase: DatabaseReference
    private lateinit var alertsDatabase: DatabaseReference

    // Views
    private lateinit var videoContainer: LinearLayout
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

    private val PERMISSIONS_REQUEST_CODE = 101

    // YouTube Data for Dashboard
    private val videos = listOf(
        VideoData("UPI Fraud Prevention", "XKfgdkcIUxw"),
        VideoData("Digital Payment Safety", "IUG2fB4gKKU"),
        VideoData("Phone Scam Alerts", "dQw4w9WgXcQ"),
        VideoData("WhatsApp Scam Prevention", "2Vv-BfVoq4g"),
        VideoData("Online Banking Tips", "fC7oUOUEEi4")
    )

    data class VideoData(val title: String, val youtubeId: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        bindViews()
        loadVideos()
        setupClickListeners()
        setupAuthStateListener()
        checkRequiredPermissions()
    }

    private fun bindViews() {
        videoContainer = findViewById(R.id.videoContainer)
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
        viewAllVideosBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=digital+fraud+prevention+india"))
            startActivity(intent)
        }

        checkLinkBtn.setOnClickListener { startActivity(Intent(this, LinkScannerActivity::class.java)) }
        fraudNumberLookupBtn.setOnClickListener { startActivity(Intent(this, FraudCallSummaryActivity::class.java)) }
        appRiskScannerBtn.setOnClickListener { startActivity(Intent(this, AppRiskScannerActivity::class.java)) }
        wifiGuardBtn.setOnClickListener { startActivity(Intent(this, WiFiGuardActivity::class.java)) }
        detailedReportBtn.setOnClickListener { startActivity(Intent(this, ScamReportActivity::class.java)) }
    }

    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                profileDatabase = FirebaseDatabase.getInstance().reference.child("users").child(user.uid).child("profile")
                alertsDatabase = FirebaseDatabase.getInstance().reference.child("users").child(user.uid).child("alerts")

                listenToProfileData()
                listenToScamAlerts()
                updateDeviceStatus(true)
            } else {
                updateDeviceStatus(false)
            }
        }
    }

    private fun listenToScamAlerts() {
        alertsDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalScams = 0
                for (alert in snapshot.children) {
                    // Match the key used in SMSReceiver ("riskLevel")
                    val risk = alert.child("riskLevel").getValue(String::class.java)
                    if (risk == "High" || risk == "Medium" || risk == "POTENTIAL_SCAM") {
                        totalScams++
                    }
                }
                scamSummaryText.text = "📅 Last 7 Days: $totalScams scams blocked"
            }
            override fun onCancelled(error: DatabaseError) { Log.e("Firebase", "Alerts Failed", error.toException()) }
        })
    }

    private fun listenToProfileData() {
        profileDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java) ?: "Guest"
                val createdAt = snapshot.child("created_at").getValue(Long::class.java) ?: 0
                val dateStr = if (createdAt > 0) SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(createdAt)) else "--"

                threatCountText.text = "Welcome back, $name!"
                lastSyncText.text = "Account created: $dateStr"
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadVideos() {
        videoContainer.removeAllViews()
        videos.forEach { video ->
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(dpToPx(170), LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, dpToPx(16), 0) }
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                background = getDrawable(android.R.drawable.dialog_frame)
            }

            val thumbnail = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(96))
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            // 🔥 CHANGED: Use hqdefault.jpg for better reliability
            Glide.with(this)
                .load("https://img.youtube.com/vi/${video.youtubeId}/hqdefault.jpg")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(thumbnail)

            thumbnail.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.youtubeId}")))
            }

            itemLayout.addView(thumbnail)
            itemLayout.addView(TextView(this).apply { text = video.title; textSize = 13f; setPadding(0, dpToPx(4), 0, 0) })
            videoContainer.addView(itemLayout)
        }
    }

    private fun checkRequiredPermissions() {
        val permissions = mutableListOf<String>()

        // 1. Check Standard Dialog Permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.RECEIVE_SMS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_SMS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_PHONE_STATE)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_CALL_LOG)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        }

        // 2. Check Overlay Permission (Settings)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }

        // 3. Check Notification Access (Settings)
        if (!isNotificationServiceEnabled()) {
            Toast.makeText(this, "Enable Notification Access to detect scams", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) ?: false
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density + 0.5f).toInt()

    private fun updateDeviceStatus(isConnected: Boolean) {
        deviceStatusText.text = if (isConnected) "🟢 Device Connected" else "🔴 Device Disconnected"
    }

    override fun onStart() { super.onStart(); auth.addAuthStateListener(authStateListener) }
    override fun onStop() { super.onStop(); auth.removeAuthStateListener(authStateListener) }
}