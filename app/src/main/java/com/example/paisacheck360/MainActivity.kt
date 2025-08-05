package com.example.paisacheck360

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.example.paisacheck360.ScamPopupService


class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var enableNotificationBtn: Button
    private lateinit var enableOverlayBtn: Button
    private lateinit var testBtn: Button

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 1001
        private const val OVERLAY_PERMISSION_REQUEST = 1002
        private const val SMS_PERMISSION_REQUEST = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()
        updateStatus()
    }

    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        enableNotificationBtn = findViewById(R.id.enableNotificationBtn)
        enableOverlayBtn = findViewById(R.id.enableOverlayBtn)
        testBtn = findViewById(R.id.testBtn)
    }

    private fun setupClickListeners() {
        enableNotificationBtn.setOnClickListener {
            requestNotificationAccess()
        }

        enableOverlayBtn.setOnClickListener {
            requestOverlayPermission()
        }

        testBtn.setOnClickListener {
            testScamDetection()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val notificationEnabled = isNotificationAccessEnabled()
        val overlayEnabled = isOverlayPermissionGranted()
        val smsEnabled = isSMSPermissionGranted()

        val status = buildString {
            appendLine("App Status:")
            appendLine("✓ Notification Access: ${if (notificationEnabled) "Enabled" else "Disabled"}")
            appendLine("✓ Overlay Permission: ${if (overlayEnabled) "Enabled" else "Disabled"}")
            appendLine("✓ SMS Permission: ${if (smsEnabled) "Enabled" else "Disabled"}")
            appendLine()
            if (notificationEnabled && overlayEnabled && smsEnabled) {
                appendLine("🟢 All permissions granted! Scam detection is active.")
            } else {
                appendLine("🔴 Please enable all permissions for the app to work.")
            }
        }

        statusText.text = status

        // Update button states
        enableNotificationBtn.isEnabled = !notificationEnabled
        enableOverlayBtn.isEnabled = !overlayEnabled

        // Request SMS permission if not granted
        if (!smsEnabled) {
            requestSMSPermission()
        }
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(this)
        return enabledPackages.contains(packageName)
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun isSMSPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestNotificationAccess() {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        startActivityForResult(intent, NOTIFICATION_PERMISSION_REQUEST)
        Toast.makeText(
            this,
            "Please enable 'Secure Bharat' in the notification access settings",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST)
        }
    }

    private fun requestSMSPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            ),
            SMS_PERMISSION_REQUEST
        )
    }

    private fun testScamDetection() {
        if (!isNotificationAccessEnabled() || !isOverlayPermissionGranted()) {
            Toast.makeText(this, "Please enable all permissions first", Toast.LENGTH_SHORT).show()
            return
        }

        // Test with a sample scam message
        val testMessage = "Congratulations! You've won ₹50,000. Click here to claim: http://fake-link.com"

        Toast.makeText(this, "Testing scam detection with sample message...", Toast.LENGTH_SHORT).show()

        // Simulate scam detection (you can remove this in production)
        val intent = Intent(this, com.example.paisacheck360.ScamPopupService::class.java)
        intent.putExtra("message", testMessage)
        intent.putExtra("isTest", true)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error starting service: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            SMS_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this, "SMS permissions granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "SMS permissions are required for the app to work", Toast.LENGTH_LONG).show()
                }
                updateStatus()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        updateStatus()
    }
}