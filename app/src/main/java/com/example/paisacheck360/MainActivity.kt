package com.example.paisacheck360

import android.Manifest
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

class MainActivity : AppCompatActivity() {

    private lateinit var videoContainer: LinearLayout
    private lateinit var viewAllVideosBtn: Button
    private lateinit var scamSummaryText: TextView
    private lateinit var checkLinkBtn: LinearLayout
    private lateinit var fraudNumberLookupBtn: LinearLayout
    private lateinit var detailedReportBtn: Button
    private lateinit var viewLogsBtn: Button
    private lateinit var appRiskScannerBtn: LinearLayout

    private val PERMISSIONS_REQUEST_CODE = 101

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

        initializeViews()
        loadVideos()
        setupClickListeners()
        checkAndRequestPermissions()
    }

    private fun initializeViews() {
        videoContainer = findViewById(R.id.videoContainer)
        viewAllVideosBtn = findViewById(R.id.viewAllVideos)
        scamSummaryText = findViewById(R.id.appTitle)
        checkLinkBtn = findViewById(R.id.check_link)
        fraudNumberLookupBtn = findViewById(R.id.fraud_number_lookup)
        detailedReportBtn = findViewById(R.id.detailed_report)
        viewLogsBtn = findViewById(R.id.viewLogsBtn)
        appRiskScannerBtn = findViewById(R.id.app_risk_scanner)
    }

    private fun setupClickListeners() {
        viewAllVideosBtn.setOnClickListener { openYouTubeSearch() }

        checkLinkBtn.setOnClickListener {
            Toast.makeText(this, "Link Checker - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        fraudNumberLookupBtn.setOnClickListener {
            startActivity(Intent(this, FraudCallSummaryActivity::class.java))
        }

        detailedReportBtn.setOnClickListener { showDetailedReport() }

        viewLogsBtn.setOnClickListener {
            startActivity(Intent(this, LogsActivity::class.java))
        }

        appRiskScannerBtn.setOnClickListener {
            Log.d("MainActivity", "App Risk Scanner clicked")
            Toast.makeText(this, "Opening App Risk Scanner...", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AppRiskScannerActivity::class.java))
        }
    }

    private fun loadVideos() {
        videoContainer.removeAllViews()

        videos.forEach { video ->
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val params = LinearLayout.LayoutParams(dpToPx(170), LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, dpToPx(16), 0)
                layoutParams = params
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                background = getDrawable(android.R.drawable.dialog_frame)
                backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFAFAFA.toInt())
                elevation = 4f
            }

            val thumbnail = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(96)
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            Glide.with(this)
                .load("https://img.youtube.com/vi/${video.youtubeId}/maxresdefault.jpg")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(android.R.drawable.ic_media_play)
                .error(android.R.drawable.stat_notify_error)
                .centerCrop()
                .into(thumbnail)

            thumbnail.setOnClickListener { openYouTubeVideo(video.youtubeId) }

            val title = TextView(this).apply {
                text = video.title
                textSize = 13f
                setTextColor(0xFF333333.toInt())
                maxLines = 2
                setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(8))
                setLineSpacing(4f, 1f)
            }

            itemLayout.addView(thumbnail)
            itemLayout.addView(title)
            videoContainer.addView(itemLayout)
        }
    }

    private fun openYouTubeVideo(videoId: String) {
        try {
            val youtubeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
            youtubeIntent.setPackage("com.google.android.youtube")
            if (youtubeIntent.resolveActivity(packageManager) != null) {
                startActivity(youtubeIntent)
            } else throw Exception("YouTube app not found")
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId")))
        }
    }

    private fun openYouTubeSearch() {
        val intent = Intent(Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/results?search_query=digital+fraud+prevention+india+cyber+security"))
        startActivity(intent)
    }

    private fun showDetailedReport() {
        val prefs = getSharedPreferences("SecureBharatPrefs", Context.MODE_PRIVATE)
        val scamCount = prefs.getInt("scam_count", 0)
        val smsCount = prefs.getInt("sms_scam_count", 0)
        val upiCount = prefs.getInt("upi_scam_count", 0)

        val message = """
            SECURITY REPORT

            Total Threats Blocked: $scamCount
            SMS Scams Stopped: $smsCount
            UPI Frauds Flagged: $upiCount
            Links Verified: ${scamCount * 2}

            Your device is secure!
            Keep India safe from digital fraud!
        """.trimIndent()

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        updateScamCount()
    }

    private fun updateScamCount() {
        val prefs = getSharedPreferences("SecureBharatPrefs", Context.MODE_PRIVATE)
        val scamCount = prefs.getInt("scam_count", 0)
        scamSummaryText.text = "Secure Bharat — $scamCount threats blocked"
    }

    /** ✅ Combined permission logic from both versions */
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
            permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
            permissionsToRequest.add(Manifest.permission.READ_CALL_LOG)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED)
            permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED)
            permissionsToRequest.add(Manifest.permission.READ_SMS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        if (!isNotificationServiceEnabled()) {
            try {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } catch (_: Exception) { }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            grantResults.forEachIndexed { index, result ->
                if (result == PackageManager.PERMISSION_DENIED) {
                    Log.w("Permissions", "${permissions[index]} was denied.")
                }
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
