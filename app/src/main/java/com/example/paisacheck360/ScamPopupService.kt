package com.example.paisacheck360

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.TextView
import androidx.core.app.NotificationCompat

class ScamPopupService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var view: View

    override fun onCreate() {
        super.onCreate()
        // ✅ Create notification channel for foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "scam_alert"
            val channelName = "Scam Alert Notifications"
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows alerts for suspected scam messages"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val smsBody = intent?.getStringExtra("sms_body") ?: "⚠️ Suspicious SMS Detected"

        // ✅ Foreground notification setup
        val notification = NotificationCompat.Builder(this, "scam_alert")
            .setContentTitle("Scam Message Detected")
            .setContentText(smsBody)
            .setSmallIcon(R.drawable.ic_warning) // make sure this icon exists
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        startForeground(1, notification)

        // ✅ Show overlay popup on screen
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        view = inflater.inflate(R.layout.popup_alert, null)

        val smsTextView = view.findViewById<TextView>(R.id.smsTextView)
        smsTextView.text = smsBody

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(view, params)

        // ✅ Auto close after 3 seconds
        view.postDelayed({ stopSelf() }, 3000)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::view.isInitialized) {
            windowManager.removeView(view)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
