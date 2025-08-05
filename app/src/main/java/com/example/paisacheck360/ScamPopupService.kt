package com.example.paisacheck360

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.TextView
import android.util.Log

class ScamPopupService : Service() {

    private lateinit var windowManager: WindowManager
    private var scamView: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val message = intent?.getStringExtra("message") ?: "Suspicious message detected!"

        Log.d("ScamPopupService", "🔔 Showing scam popup: $message")

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        scamView = inflater.inflate(R.layout.scam_alert_popup, null)

        val messageTextView = scamView?.findViewById<TextView>(R.id.alertMessageText)
        messageTextView?.text = message

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 100

        windowManager.addView(scamView, params)

        // Auto-dismiss after 5 seconds
        scamView?.postDelayed({
            stopSelf()
        }, 5000)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scamView?.let {
            windowManager.removeView(it)
            scamView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
