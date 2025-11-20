package com.example.paisacheck360

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.regex.Pattern

class ScamPopupService : Service() {

    private var windowManager: WindowManager? = null
    private var popupView: LinearLayout? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sender = intent?.getStringExtra("sender") ?: "Unknown"
        val body = intent?.getStringExtra("body") ?: ""
        val risk = intent?.getStringExtra("risk") ?: "Safe"
        val suggestions = intent?.getStringArrayListExtra("suggestedReplies") ?: arrayListOf("OK", "Got it")

        showPopup(sender, body, risk, suggestions)
        return START_NOT_STICKY
    }

    private fun showPopup(sender: String, message: String, risk: String, suggestions: ArrayList<String>) {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        try { windowManager?.removeView(popupView) } catch (_: Exception) {}

        val headerColor = when (risk) {
            "High" -> "#FF5252"
            "Medium" -> "#FFD740"
            "Low" -> "#81D4FA"
            "Suspicious" -> "#FFAB40"
            else -> "#4CAF50"
        }

        popupView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable()
            bg.setColor(Color.WHITE)
            bg.cornerRadius = 40f
            background = bg
            setPadding(32, 32, 32, 32)
            elevation = 16f
        }

        val header = TextView(this).apply {
            text = "📩 Message Alert"
            setBackgroundColor(Color.parseColor(headerColor))
            setTextColor(Color.WHITE)
            textSize = 17f
            setPadding(20, 15, 20, 15)
        }
        popupView?.addView(header)

        val senderView = TextView(this).apply {
            text = "From: $sender"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 10, 0, 10)
        }
        popupView?.addView(senderView)

        val bodyView = TextView(this).apply {
            text = if (message.length > 250) message.take(250) + "..." else message
            textSize = 15f
            setTextColor(Color.BLACK)
            setPadding(0, 5, 0, 10)
        }
        popupView?.addView(bodyView)

        // ---------------------------------------------------------------------
        // ⭐⭐ NEW CODE ADDED HERE — URL DETECTION + BUTTON (as requested)
        // ---------------------------------------------------------------------

        val urls = UrlAnalyzer.extractUrls(message)
        if (urls.isNotEmpty()) {

            val openBtn = Button(this).apply {
                text = "🔒 Open Link Safely"
                setAllCaps(false)
                textSize = 14f
                setBackgroundColor(Color.parseColor("#FF5722"))
                setTextColor(Color.WHITE)
                setOnClickListener {

                    // ⭐ Requested line (added but disabled ⭐)
                     UrlBlocker.openUrlWithProtection(this@ScamPopupService, urls[0])

                    Toast.makeText(
                        this@ScamPopupService,
                        "Safe-open protection is added but not active.",
                        Toast.LENGTH_SHORT
                    ).show()

                    removePopup()
                }
            }

            popupView?.addView(openBtn)
        }

        // ---------------------------------------------------------------------

        if (suggestions.isNotEmpty()) {
            val label = TextView(this).apply {
                text = "💡 Suggested replies:"
                setTextColor(Color.BLACK)
                textSize = 15f
                setPadding(0, 10, 0, 5)
            }
            popupView?.addView(label)

            val suggestionsLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            for (s in suggestions.take(3)) {
                val replyBtn = Button(this).apply {
                    text = s
                    setAllCaps(false)
                    setBackgroundColor(Color.parseColor("#0D6EFD"))
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.setMargins(8, 8, 8, 8)
                    layoutParams = lp
                    setOnClickListener {
                        onSuggestionClicked(sender, s, message)
                        removePopup()
                    }
                }
                suggestionsLayout.addView(replyBtn)
            }
            popupView?.addView(suggestionsLayout)
        }

        val actionsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        actionsLayout.addView(actionButton("📊 View Report") {
            startActivity(Intent(this, SmsSummaryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            removePopup()
        })
        actionsLayout.addView(actionButton("🚨 Report") {
            saveUserAction("report", sender, message, risk)
            Toast.makeText(this, "Reported", Toast.LENGTH_SHORT).show()
            removePopup()
        })
        actionsLayout.addView(actionButton("✅ Safe") {
            saveUserAction("safe", sender, message, risk)
            removePopup()
        })
        popupView?.addView(actionsLayout)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP
        params.y = 80

        windowManager?.addView(popupView, params)
        popupView?.postDelayed({ removePopup() }, 9000)
    }

    private fun actionButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setAllCaps(false)
            textSize = 13f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2196F3"))
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(8, 12, 8, 12)
            layoutParams = lp
            setOnClickListener { onClick() }
        }
    }

    private fun onSuggestionClicked(number: String, suggestion: String, body: String) {
        if (suggestion.startsWith("Copy OTP", true)) {
            val otp = extractOtp(body)
            if (otp != null) {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("OTP", otp))
                Toast.makeText(this, "Copied OTP: $otp", Toast.LENGTH_SHORT).show()
            }
            return
        }

        try {
            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
            smsIntent.putExtra("sms_body", suggestion)
            smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(smsIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open SMS app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractOtp(body: String): String? {
        val pattern = Pattern.compile("\\b(\\d{4,8})\\b")
        val matcher = pattern.matcher(body)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun saveUserAction(action: String, sender: String, message: String, risk: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        val db = FirebaseDatabase.getInstance().getReference("users/$uid/userActions")
        val id = db.push().key ?: System.currentTimeMillis().toString()
        val data = mapOf(
            "action" to action,
            "sender" to sender,
            "message" to message,
            "risk" to risk,
            "timestamp" to System.currentTimeMillis()
        )
        db.child(id).setValue(data)
    }

    private fun removePopup() {
        try { windowManager?.removeView(popupView) } catch (_: Exception) {}
        popupView = null
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
