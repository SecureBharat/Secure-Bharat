package com.example.paisacheck360

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var callerIdCard: CardView
    private lateinit var appScanCard: CardView
    private lateinit var linkGuardCard: CardView
    private lateinit var wifiGuardCard: CardView
    private lateinit var profileBtn: ImageView
    private lateinit var shieldIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        bindViews()
        setupClickListeners()

        Handler(Looper.getMainLooper()).postDelayed({
            animateShield()
        }, 100)
    }

    private fun bindViews() {
        profileBtn = findViewById(R.id.profileBtn)
        shieldIcon = findViewById(R.id.shieldIcon)
        appScanCard = findViewById(R.id.appScanCard)
        linkGuardCard = findViewById(R.id.linkGuardCard)
        callerIdCard = findViewById(R.id.callerIdCard)
        wifiGuardCard = findViewById(R.id.wifiGuardCard)
    }

    private fun setupClickListeners() {
        profileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // REDIRECTED: Open FraudCallSummaryActivity for the custom UI
        callerIdCard.setOnClickListener {
            animateCardClick(callerIdCard)
            startActivity(Intent(this, FraudCallSummaryActivity::class.java))
        }

        val soon = { Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show() }
        appScanCard.setOnClickListener { animateCardClick(appScanCard); soon() }
        linkGuardCard.setOnClickListener { animateCardClick(linkGuardCard); soon() }
        wifiGuardCard.setOnClickListener { animateCardClick(wifiGuardCard); soon() }
    }

    private fun animateShield() {
        val scaleX = ObjectAnimator.ofFloat(shieldIcon, "scaleX", 0.5f, 1f)
        val scaleY = ObjectAnimator.ofFloat(shieldIcon, "scaleY", 0.5f, 1f)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 800
        animatorSet.interpolator = OvershootInterpolator()
        animatorSet.start()
    }

    private fun animateCardClick(card: CardView) {
        card.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
            card.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
    }
}