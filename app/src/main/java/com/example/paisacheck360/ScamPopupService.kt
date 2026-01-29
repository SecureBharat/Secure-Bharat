package com.example.paisacheck360

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class ScamPopupService : Service() {

    private lateinit var windowManager: WindowManager
    private var popupView: View? = null

    override fun onCreate() {
        super.onCreate()

        // 🔥 MUST START FOREGROUND IMMEDIATELY
        startForegroundServiceProperly()
        showPopup()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ✅ FOREGROUND NOTIFICATION (MANDATORY)
    @RequiresApi(Build.VERSION_CODES.ECLAIR)
    private fun startForegroundServiceProperly() {

        val channelId = "scam_popup_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Scam Alerts",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Secure Bharat Active")
            .setContentText("Monitoring for scam messages")
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .build()

        startForeground(101, notification) // 🔥 THIS PREVENTS CRASH
    }

    // ✅ POPUP OVERLAY
    private fun showPopup() {

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        popupView = inflater.inflate(R.layout.popup_scam_alert, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP

        windowManager.addView(popupView, params)

        popupView?.setOnClickListener {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (popupView != null) {
            windowManager.removeView(popupView)
        }
    }
}
