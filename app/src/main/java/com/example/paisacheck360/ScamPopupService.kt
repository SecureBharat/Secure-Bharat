package com.example.paisacheck360

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.util.Log
import androidx.core.app.NotificationCompat

class ScamPopupService : Service() {

    private var windowManager: WindowManager? = null
    private var popupView: View? = null

    companion object {
        private const val NOTIF_CHANNEL_ID = "scam_popup_channel"
        private const val NOTIF_ID = 777
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("ScamPopupService", "✅ Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val message = intent?.getStringExtra("message") ?: "Unknown scam message"

        startForegroundNotification()
        showScamPopup(message)

        return START_NOT_STICKY
    }

    private fun startForegroundNotification() {
        val channelName = "Scam Alert Popup"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notif = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("Scam Alert")
            .setContentText("Popup running...")
            .setSmallIcon(R.drawable.ic_warning) // Replace with your alert icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIF_ID, notif)
    }

    private fun showScamPopup(message: String) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        popupView = inflater.inflate(R.layout.scam_alert_popup, null)

        // Set message
        popupView?.findViewById<TextView>(R.id.textViewMessage)?.text = message

        // Dismiss button
        popupView?.findViewById<Button>(R.id.buttonDismiss)?.setOnClickListener {
            Log.d("ScamPopupService", "❌ Dismiss clicked")
            if (popupView != null) {
                windowManager?.removeView(popupView)
                popupView = null
            }
            stopForeground(true)
            stopSelf()
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 100

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        try {
            windowManager?.addView(popupView, params)
            Log.d("ScamPopupService", "🔔 Popup displayed")
        } catch (e: Exception) {
            Log.e("ScamPopupService", "❌ Failed to show popup: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (popupView != null) {
            windowManager?.removeView(popupView)
            popupView = null
        }
        stopForeground(true)
        Log.d("ScamPopupService", "🛑 Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
