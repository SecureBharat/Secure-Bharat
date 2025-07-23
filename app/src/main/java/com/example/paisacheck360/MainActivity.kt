package com.example.paisacheck360

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val SMS_PERMISSION_CODE = 100
    private val OVERLAY_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        var permissionMessage = ""

        // ✅ 1. Check SMS permissions
        if (!hasSMSPermissions()) {
            requestSMSPermissions()
            permissionMessage += "Requesting SMS permissions...\n"
        } else {
            permissionMessage += "SMS permission already granted ✅\n"
        }

        // ✅ 2. Check Overlay permission (for popup alerts)
        if (!canDrawOverlays()) {
            requestOverlayPermission()
            permissionMessage += "Requesting overlay permission...\n"
        } else {
            permissionMessage += "Overlay permission already granted ✅\n"
        }

        Toast.makeText(this, permissionMessage.trim(), Toast.LENGTH_LONG).show()
    }

    private fun hasSMSPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSMSPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            ),
            SMS_PERMISSION_CODE
        )
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_CODE)
        }
    }

    // ✅ Handle permission results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "SMS Permissions Granted ✅", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "SMS Permissions Denied ❌", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ✅ Handle overlay permission result (optional for future enhancement)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            if (canDrawOverlays()) {
                Toast.makeText(this, "Overlay permission granted ✅", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Overlay permission denied ❌", Toast.LENGTH_LONG).show()
            }
        }
    }
}
