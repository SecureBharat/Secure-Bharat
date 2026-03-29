package com.example.paisacheck360

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class CallPopupService : Service() {

    private lateinit var windowManager: WindowManager
    private var popupView: View? = null
    private var isPopupVisible = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val number = intent?.getStringExtra("callerNumber") ?: "Unknown"
        if (!isPopupVisible) {
            fetchAndShowRealTimeAlert(number)
        }
        return START_NOT_STICKY
    }

    private fun fetchAndShowRealTimeAlert(number: String) {
        val safeKey = number.replace(Regex("[.#$\\[\\]]"), "_")
        val globalRef = FirebaseDatabase.getInstance().reference.child("global_scam_reports").child(safeKey)

        globalRef.get().addOnSuccessListener { snapshot ->
            val scamCount = snapshot.child("scam_count").getValue(Long::class.java) ?: 0L
            showOverlay(number, scamCount)
        }.addOnFailureListener {
            showOverlay(number, 0L)
        }
    }

    private fun showOverlay(number: String, scamCount: Long) {
        if (!Settings.canDrawOverlays(this)) return

        try {
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            popupView = inflater.inflate(R.layout.activity_call_feedback, null)

            // Layout parameters to sit in the middle of the screen
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.CENTER }

            popupView?.let { view ->
                val txtNumber = view.findViewById<TextView>(R.id.txtNumber)
                val btnScam = view.findViewById<View>(R.id.btnScam)
                val btnSafe = view.findViewById<View>(R.id.btnSafe)

                if (scamCount > 0) {
                    // REAL-TIME SCAM WARNING
                    txtNumber.text = "🚨 HIGH RISK SCAM 🚨\n$number\nReported by $scamCount users!"
                    txtNumber.setTextColor(Color.RED)
                    view.setBackgroundColor(Color.parseColor("#FFE0E0")) // Light Red background
                } else {
                    txtNumber.text = "Incoming Call: $number\nIs this a scam?"
                }

                btnScam.setOnClickListener {
                    saveFeedback(number, "Scam")
                    stopSelf()
                }

                btnSafe.setOnClickListener {
                    saveFeedback(number, "Safe")
                    stopSelf()
                }

                windowManager.addView(view, params)
                isPopupVisible = true
            }
        } catch (e: Exception) {
            Log.e("CallPopupService", "Overlay Error: ${e.message}")
        }
    }

    private fun saveFeedback(number: String, status: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val safeKey = number.replace(Regex("[.#$\\[\\]]"), "_")
        val rootRef = FirebaseDatabase.getInstance().reference

        // Update User History
        rootRef.child("users").child(userId).child("call_feedback").child(safeKey)
            .setValue(mapOf("status" to status, "timestamp" to System.currentTimeMillis()))

        // Update Global Counter
        if (status == "Scam") {
            rootRef.child("global_scam_reports").child(safeKey).child("scam_count")
                .runTransaction(object : com.google.firebase.database.Transaction.Handler {
                    override fun doTransaction(mutableData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                        val current = mutableData.getValue(Long::class.java) ?: 0L
                        mutableData.value = current + 1
                        return com.google.firebase.database.Transaction.success(mutableData)
                    }
                    override fun onComplete(error: com.google.firebase.database.DatabaseError?, committed: Boolean, snapshot: com.google.firebase.database.DataSnapshot?) {}
                })
        }
    }

    private fun startInForeground() {
        val channelId = "CallPopupChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Security", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Secure Bharat").setContentText("Real-time call protection active").setSmallIcon(R.mipmap.ic_launcher).build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(2, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isPopupVisible) {
            popupView?.let {
                try { windowManager.removeView(it) } catch (e: Exception) {}
            }
        }
    }
}