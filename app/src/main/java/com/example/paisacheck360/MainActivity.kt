package com.example.paisacheck360

import android.Manifest
import android.annotation.SuppressLint
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
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private val permissionsRequestCode = 101

    // UI Components
    private lateinit var videoContainer: LinearLayout
    private lateinit var viewAllVideosBtn: Button
    private lateinit var scamSummaryTitle: TextView
    private lateinit var scamCountText: TextView

    private lateinit var checkLinkBtn: LinearLayout
    private lateinit var fraudNumberLookupBtn: LinearLayout
    private lateinit var detailedReportBtn: Button
    private lateinit var viewLogsBtn: Button
    private lateinit var appRiskScannerBtn: LinearLayout
    private lateinit var wifiGuardBtn: LinearLayout
    private lateinit var vpnModeSwitch: Switch

    // Firebase
    private lateinit var db: DatabaseReference

    private val videos = listOf(
        VideoData("UPI Fraud Prevention", "XKfgdkcIUxw"),
        VideoData("Digital Payment Safety", "IUG2fB4gKKU"),
        VideoData("Phone Scam Alerts", "dQw4w9WgXcQ"),
        VideoData("WhatsApp Scam Prevention", "2Vv-BfVoq4g"),
        VideoData("Online Banking Tips", "fC7oUOUEEi4")
    )

    data class VideoData(val title: String, val youtubeId: String)

    // ----------------------------------------------------------
    // 🔵 OnCreate
    // ----------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadVideos()
        setupClickListeners()
        restoreVpnState()
        checkPermissions()
        updateScamSummary()
    }

    // ----------------------------------------------------------
    // 🔵 Initialize Views
    // ----------------------------------------------------------
    private fun initViews() {
        videoContainer = findViewById(R.id.videoContainer)
        viewAllVideosBtn = findViewById(R.id.viewAllVideos)

        scamSummaryTitle = findViewById(R.id.appTitle)
        scamCountText = findViewById(R.id.scam_count_text)

        checkLinkBtn = findViewById(R.id.check_link)
        fraudNumberLookupBtn = findViewById(R.id.fraud_number_lookup)
        detailedReportBtn = findViewById(R.id.detailed_report)
        viewLogsBtn = findViewById(R.id.viewLogsBtn)
        appRiskScannerBtn = findViewById(R.id.app_risk_scanner)
        wifiGuardBtn = findViewById(R.id.wifi_guard)

        vpnModeSwitch = findViewById(R.id.vpnModeSwitch)
    }

    // ----------------------------------------------------------
    // 🔵 Video Loader
    // ----------------------------------------------------------
    private fun loadVideos() {
        videoContainer.removeAllViews()

        videos.forEach { video ->
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val params = LinearLayout.LayoutParams(dp(170), LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, dp(16), 0)
                layoutParams = params
                setPadding(dp(8), dp(8), dp(8), dp(8))
                background = getDrawable(android.R.drawable.dialog_frame)
                elevation = 4f
            }

            val thumbnail = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(96)
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            Glide.with(this)
                .load("https://img.youtube.com/vi/${video.youtubeId}/maxresdefault.jpg")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(android.R.drawable.ic_media_play)
                .centerCrop()
                .into(thumbnail)

            thumbnail.setOnClickListener { openYouTubeVideo(video.youtubeId) }

            val title = TextView(this).apply {
                text = video.title
                textSize = 13f
                setTextColor(0xFF333333.toInt())
                maxLines = 2
                setPadding(dp(8), dp(12), dp(8), dp(8))
            }

            itemLayout.addView(thumbnail)
            itemLayout.addView(title)
            videoContainer.addView(itemLayout)
        }
    }

    // ----------------------------------------------------------
    // 🔵 Click Listeners
    // ----------------------------------------------------------
    private fun setupClickListeners() {

        viewAllVideosBtn.setOnClickListener { openYouTubeSearch() }

        checkLinkBtn.setOnClickListener {
            startActivity(Intent(this, LinkScannerActivity::class.java))
        }

        fraudNumberLookupBtn.setOnClickListener {
            startActivity(Intent(this, FraudCallSummaryActivity::class.java))
        }

        detailedReportBtn.setOnClickListener {
            startActivity(Intent(this, ScamReportActivity::class.java))
        }

        viewLogsBtn.setOnClickListener {
            startActivity(Intent(this, SmsSummaryActivity::class.java))
        }

        appRiskScannerBtn.setOnClickListener {
            startActivity(Intent(this, AppRiskScannerActivity::class.java))
        }

        wifiGuardBtn.setOnClickListener {
            startActivity(Intent(this, WiFiGuardActivity::class.java))
        }

        vpnModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            handleVpnSwitch(isChecked)
        }
    }

    // ----------------------------------------------------------
    // 🔵 VPN Mode Saving & Background Service Control
    // ----------------------------------------------------------
    private fun handleVpnSwitch(isEnabled: Boolean) {
        val prefs = getSharedPreferences("SecureBharatPrefs", MODE_PRIVATE)

        prefs.edit().putString("vpn_mode", if (isEnabled) "block" else "monitor").apply()

        val intent = Intent(this, LocalVpnService::class.java)
        intent.action = if (isEnabled) "START" else "STOP"

        startService(intent)

        Toast.makeText(this, "VPN mode: ${if (isEnabled) "Block Suspicious" else "Monitor Only"}", Toast.LENGTH_SHORT).show()
    }

    private fun restoreVpnState() {
        val prefs = getSharedPreferences("SecureBharatPrefs", MODE_PRIVATE)
        vpnModeSwitch.isChecked = prefs.getString("vpn_mode", "block") == "block"
    }

    // ----------------------------------------------------------
    // 🔵 YouTube Helpers
    // ----------------------------------------------------------
    private fun openYouTubeVideo(videoId: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
            intent.setPackage("com.google.android.youtube")
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId")))
        }
    }

    private fun openYouTubeSearch() {
        startActivity(Intent(Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/results?search_query=digital+fraud+prevention+india")))
    }

    // ----------------------------------------------------------
    // 🔵 Scam Summary from Firebase
    // ----------------------------------------------------------
    private fun updateScamSummary() {
        val androidID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "guest"

        db = FirebaseDatabase.getInstance("https://sbtest-9acea-default-rtdb.firebaseio.com")
            .getReference("users/$androidID/alerts")

        db.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {

                var high = 0
                var medium = 0
                var low = 0
                var safe = 0

                for (child in snapshot.children) {
                    when (child.child("risk").getValue(String::class.java)) {
                        "High" -> high++
                        "Medium" -> medium++
                        "Low" -> low++
                        else -> safe++
                    }
                }

                scamSummaryTitle.text = "Secure Bharat – Active Protection"

                scamCountText.text =
                    "📅 Last 7 Days: ${high + medium + low} scams flagged\n" +
                            "🔴 High: $high  |  🟡 Medium: $medium  |  🔵 Low: $low  |  🟢 Safe: $safe"
            }

            override fun onCancelled(error: DatabaseError) {
                scamCountText.text = "Failed to load report."
            }
        })
    }

    // ----------------------------------------------------------
    // 🔵 Permission Request
    // ----------------------------------------------------------
    private fun checkPermissions() {
        val required = mutableListOf<String>()

        fun addPermission(perm: String) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
                required.add(perm)
        }

        addPermission(Manifest.permission.RECEIVE_SMS)
        addPermission(Manifest.permission.READ_SMS)
        addPermission(Manifest.permission.READ_PHONE_STATE)
        addPermission(Manifest.permission.READ_CALL_LOG)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addPermission(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (required.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, required.toTypedArray(), permissionsRequestCode)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
    }

    // ----------------------------------------------------------
    // 🔵 Utility
    // ----------------------------------------------------------
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5).toInt()
    }
}
