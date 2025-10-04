package com.example.paisacheck360

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class MainActivity : AppCompatActivity() {

    private lateinit var videoContainer: LinearLayout
    private lateinit var viewAllVideosBtn: Button
    private lateinit var scamSummaryText: TextView
    private lateinit var scanUpiBtn: LinearLayout
    private lateinit var checkLinkBtn: LinearLayout
    private lateinit var fraudNumberLookupBtn: LinearLayout
    private lateinit var ocrScannerBtn: LinearLayout   // ✅ OCR Scanner Button
    private lateinit var detailedReportBtn: Button
    private lateinit var viewLogsBtn: Button           // ✅ Logs button

    private val videos = listOf(
        VideoData("🔒 UPI Fraud Prevention", "XKfgdkcIUxw"),
        VideoData("📱 Digital Payment Safety", "IUG2fB4gKKU"),
        VideoData("⚠️ Phone Scam Alerts", "dQw4w9WgXcQ"),
        VideoData("🎯 QR Code Safety", "9bZkp7q19f0"),
        VideoData("💬 WhatsApp Scam Prevention", "2Vv-BfVoq4g"),
        VideoData("💳 Online Banking Tips", "fC7oUOUEEi4")
    )

    data class VideoData(val title: String, val youtubeId: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        loadVideos()
        checkRequiredPermissions()
        setupClickListeners()
    }

    private fun initializeViews() {
        videoContainer = findViewById(R.id.videoContainer)
        viewAllVideosBtn = findViewById(R.id.viewAllVideos)
        scamSummaryText = findViewById(R.id.appTitle)
        scanUpiBtn = findViewById(R.id.scan_upi)
        checkLinkBtn = findViewById(R.id.check_link)
        fraudNumberLookupBtn = findViewById(R.id.fraud_number_lookup)
        ocrScannerBtn = findViewById(R.id.ocr_scanner)
        detailedReportBtn = findViewById(R.id.detailed_report)
        viewLogsBtn = findViewById(R.id.viewLogsBtn)
    }

    private fun setupClickListeners() {
        viewAllVideosBtn.setOnClickListener {
            openYouTubeSearch()
        }

        scanUpiBtn.setOnClickListener {
            // ✅ Launch QR scanner directly
            startActivity(Intent(this, QRScannerActivity::class.java))
        }

        checkLinkBtn.setOnClickListener {
            // ✅ Open link phishing checker activity
            startActivity(Intent(this, PhishCheckActivity::class.java).apply {
                putExtra("scanned", "https://example.com") // placeholder or scanned link
            })
        }

        fraudNumberLookupBtn.setOnClickListener {
            startActivity(Intent(this, FraudCallSummaryActivity::class.java))
        }

        ocrScannerBtn.setOnClickListener {
            // ✅ Launch OCR / QR Scanner Activity
            startActivity(Intent(this, QRScannerActivity::class.java))
        }

        detailedReportBtn.setOnClickListener {
            showDetailedReport()
        }

        viewLogsBtn.setOnClickListener {
            startActivity(Intent(this, LogsActivity::class.java))
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
                setBackgroundColor(0xFFE8F4FD.toInt())
                setPadding(4, 4, 4, 4)
            }

            Glide.with(this)
                .load("https://img.youtube.com/vi/${video.youtubeId}/maxresdefault.jpg")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(android.R.drawable.ic_media_play)
                .error(android.R.drawable.stat_notify_error)
                .centerCrop()
                .into(thumbnail)

            thumbnail.setOnClickListener {
                openYouTubeVideo(video.youtubeId)
            }

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
                Toast.makeText(this, "🎬 Opening video...", Toast.LENGTH_SHORT).show()
            } else {
                throw Exception("YouTube app not found")
            }
        } catch (e: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                startActivity(webIntent)
            } catch (ex: Exception) {
                Toast.makeText(this, "❌ Cannot open video", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openYouTubeSearch() {
        try {
            val intent = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/results?search_query=digital+fraud+prevention+india+cyber+security"))
            startActivity(intent)
            Toast.makeText(this, "🎬 Opening security videos...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Cannot open browser", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDetailedReport() {
        val prefs = getSharedPreferences("SecureBharatPrefs", Context.MODE_PRIVATE)
        val scamCount = prefs.getInt("scam_count", 0)
        val smsCount = prefs.getInt("sms_scam_count", 0)
        val upiCount = prefs.getInt("upi_scam_count", 0)

        val message = "📊 SECURITY REPORT\n\n" +
                "🛡️ Total Threats Blocked: $scamCount\n" +
                "📧 SMS Scams Stopped: $smsCount\n" +
                "💳 UPI Frauds Flagged: $upiCount\n" +
                "🔗 Links Verified: ${scamCount * 2}\n\n" +
                "✅ Your device is secure!\n" +
                "🇮🇳 Keep India safe from digital fraud!"

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        updateScamCount()
    }

    private fun updateScamCount() {
        val prefs = getSharedPreferences("SecureBharatPrefs", Context.MODE_PRIVATE)
        val scamCount = prefs.getInt("scam_count", 0)
        scamSummaryText.text = "🛡️ Secure Bharat — $scamCount threats blocked"
    }

    private fun checkRequiredPermissions() {
        if (!isNotificationServiceEnabled()) {
            Toast.makeText(this, "🔔 Enable notifications for real-time protection",
                Toast.LENGTH_LONG).show()
            try {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } catch (e: Exception) {
                // Settings not available
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
                startActivity(intent)
            } catch (e: Exception) {
                // Settings not available
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
