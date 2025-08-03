package com.example.paisacheck360

import android.Manifest
import android.content.Context
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
import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {

    private val SMS_PERMISSION_CODE = 100
    private val OVERLAY_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()
        checkNotificationAccessPermission(this)

        // 🔐 Firebase Secure Login Logic (Android ID + Hashed Password)
        val androidID = getAndroidID()
        val password = "123456" // Replace later with EditText input
        val hashedPassword = hashPassword(password)

        val db = FirebaseDatabase.getInstance().reference
        db.child("users").child(androidID).child("password").setValue(hashedPassword)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ User saved securely", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Failed to save user", Toast.LENGTH_LONG).show()
            }
    }

    private fun getAndroidID(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val result = digest.digest(password.toByteArray())
        return result.joinToString("") { "%02x".format(it) }
    }

    private fun checkAndRequestPermissions() {
        var permissionMessage = ""

        // SMS Permissions
        if (!hasSMSPermissions()) {
            requestSMSPermissions()
            permissionMessage += "Requesting SMS permissions...\n"
        } else {
            permissionMessage += "✅ SMS permissions already granted\n"
        }

        // Overlay Permission
        if (!canDrawOverlays()) {
            requestOverlayPermission()
            permissionMessage += "Requesting overlay permission...\n"
        } else {
            permissionMessage += "✅ Overlay permission already granted\n"
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "✅ SMS permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "❌ SMS permissions denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == OVERLAY_PERMISSION_CODE) {
            if (canDrawOverlays()) {
                Toast.makeText(this, "✅ Overlay permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "❌ Overlay permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }
}

// 🔔 Notification Access Helper Function
fun checkNotificationAccessPermission(context: Context) {
    val listeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )

    if (listeners == null || !listeners.contains(context.packageName)) {
        Toast.makeText(context, "Please enable Notification Access", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
