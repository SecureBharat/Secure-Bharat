package com.example.paisacheck360

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var viewAllVideosBtn: Button
    private lateinit var scamSummaryText: TextView
    private lateinit var scanUpiBtn: LinearLayout
    private lateinit var checkLinkBtn: LinearLayout
    private lateinit var fraudNumberLookupBtn: LinearLayout
    private lateinit var ocrScannerBtn: LinearLayout
    private lateinit var detailedReportBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind views
        viewAllVideosBtn = findViewById(R.id.viewAllVideos)
        scamSummaryText = findViewById(R.id.appTitle) // Showing scam count here under title
        scanUpiBtn = findViewById(R.id.scan_upi)
        checkLinkBtn = findViewById(R.id.check_link)
        fraudNumberLookupBtn = findViewById(R.id.fraud_number_lookup)
        ocrScannerBtn = findViewById(R.id.ocr_scanner)
        detailedReportBtn = findViewById(R.id.detailed_report)

        // Check permissions first time
        checkRequiredPermissions()

        // Button actions
        viewAllVideosBtn.setOnClickListener {
            Toast.makeText(this, "Opening Scam Awareness Videos", Toast.LENGTH_SHORT).show()
        }

        scanUpiBtn.setOnClickListener {
            Toast.makeText(this, "Scan UPI ID Tool Coming Soon", Toast.LENGTH_SHORT).show()
        }

        checkLinkBtn.setOnClickListener {
            Toast.makeText(this, "Check Link Tool Coming Soon", Toast.LENGTH_SHORT).show()
        }

        fraudNumberLookupBtn.setOnClickListener {
            Toast.makeText(this, "Fraud Number Lookup Coming Soon", Toast.LENGTH_SHORT).show()
        }

        ocrScannerBtn.setOnClickListener {
            Toast.makeText(this, "OCR Scanner Coming Soon", Toast.LENGTH_SHORT).show()
        }

        detailedReportBtn.setOnClickListener {
            Toast.makeText(this, "Opening Detailed Scam Report", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateScamCount()
    }

    private fun updateScamCount() {
        val prefs = getSharedPreferences("SecureBharatPrefs", Context.MODE_PRIVATE)
        val scamCount = prefs.getInt("scam_count", 0)
        scamSummaryText.text = "Secure Bharat – $scamCount scams blocked"
    }

    private fun checkRequiredPermissions() {
        // Notification Listener
        if (!isNotificationServiceEnabled()) {
            Toast.makeText(this, "Please enable Notification Access", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        // Overlay Permission
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
