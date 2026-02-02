package com.example.paisacheck360

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.animation.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var database: DatabaseReference

    // Views
    private lateinit var menuIcon: ImageView
    private lateinit var profileBtn: ImageView
    private lateinit var shieldIcon: ImageView

    private lateinit var contentCard: LinearLayout
    private lateinit var statsCardsRow: LinearLayout
    private lateinit var threatsCard: CardView
    private lateinit var alertsCard: CardView
    private lateinit var toolsHeader: RelativeLayout
    private lateinit var toolsGrid: LinearLayout

    private lateinit var threatCountText: TextView
    private lateinit var alertCountText: TextView
    private lateinit var viewAllTools: TextView

    private lateinit var appScanCard: CardView
    private lateinit var linkGuardCard: CardView
    private lateinit var callerIdCard: CardView
    private lateinit var wifiGuardCard: CardView

    // Navbar
    private lateinit var navHome: FrameLayout
    private lateinit var navTools: FrameLayout
    private lateinit var navScan: FrameLayout
    private lateinit var navReport: FrameLayout
    private lateinit var navStats: FrameLayout

    private lateinit var navHomeContainer: RelativeLayout
    private lateinit var navToolsContainer: RelativeLayout
    private lateinit var navScanContainer: RelativeLayout
    private lateinit var navReportContainer: RelativeLayout
    private lateinit var navStatsContainer: RelativeLayout

    private lateinit var iconHome: ImageView
    private lateinit var iconTools: ImageView
    private lateinit var iconScan: ImageView
    private lateinit var iconReport: ImageView
    private lateinit var iconStats: ImageView

    private var currentActiveIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        bindViews()
        bindNavbarViews()
        setupClickListeners()
        setupNavbarClickListeners()
        setupAuthStateListener()
        setInitialNavbarState()
        updateStats()

        // Start smooth animations after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            startEntryAnimations()
        }, 100)
    }

    private fun bindViews() {
        menuIcon = findViewById(R.id.menuIcon)
        profileBtn = findViewById(R.id.profileBtn)
        shieldIcon = findViewById(R.id.shieldIcon)

        contentCard = findViewById(R.id.contentCard)
        statsCardsRow = findViewById(R.id.statsCardsRow)
        threatsCard = findViewById(R.id.threatsCard)
        alertsCard = findViewById(R.id.alertsCard)
        toolsHeader = findViewById(R.id.toolsHeader)
        toolsGrid = findViewById(R.id.toolsGrid)

        threatCountText = findViewById(R.id.threatCountText)
        alertCountText = findViewById(R.id.alertCountText)
        viewAllTools = findViewById(R.id.viewAllTools)

        appScanCard = findViewById(R.id.appScanCard)
        linkGuardCard = findViewById(R.id.linkGuardCard)
        callerIdCard = findViewById(R.id.callerIdCard)
        wifiGuardCard = findViewById(R.id.wifiGuardCard)
    }

    private fun bindNavbarViews() {
        navHome = findViewById(R.id.navHome)
        navTools = findViewById(R.id.navTools)
        navScan = findViewById(R.id.navScan)
        navReport = findViewById(R.id.navReport)
        navStats = findViewById(R.id.navStats)

        navHomeContainer = findViewById(R.id.navHomeContainer)
        navToolsContainer = findViewById(R.id.navToolsContainer)
        navScanContainer = findViewById(R.id.navScanContainer)
        navReportContainer = findViewById(R.id.navReportContainer)
        navStatsContainer = findViewById(R.id.navStatsContainer)

        iconHome = findViewById(R.id.iconHome)
        iconTools = findViewById(R.id.iconTools)
        iconScan = findViewById(R.id.iconScan)
        iconReport = findViewById(R.id.iconReport)
        iconStats = findViewById(R.id.iconStats)
    }

    private fun setupClickListeners() {
        menuIcon.setOnClickListener {
            Toast.makeText(this, "Menu", Toast.LENGTH_SHORT).show()
        }

        profileBtn.setOnClickListener {
            try {
                startActivity(Intent(this, ProfileActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
            }
        }

        val comingSoon = {
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show()
        }

        viewAllTools.setOnClickListener { comingSoon() }
        appScanCard.setOnClickListener { animateCardClick(appScanCard); comingSoon() }
        linkGuardCard.setOnClickListener { animateCardClick(linkGuardCard); comingSoon() }
        callerIdCard.setOnClickListener { animateCardClick(callerIdCard); comingSoon() }
        wifiGuardCard.setOnClickListener { animateCardClick(wifiGuardCard); comingSoon() }
    }

    private fun setupNavbarClickListeners() {
        navHome.setOnClickListener { navigateToItem(0) }
        navTools.setOnClickListener { navigateToItem(1) }
        navScan.setOnClickListener { navigateToItem(2) }
        navReport.setOnClickListener { navigateToItem(3) }
        navStats.setOnClickListener { navigateToItem(4) }
    }

    private fun startEntryAnimations() {
        // Shield icon pulse animation
        animateShield()

        // Content card slide up animation
        animateContentCard()

        // Stats cards pop in animation
        animateStatsCards()

        // Tools section fade in
        animateToolsSection()
    }

    private fun animateShield() {
        val scaleX = ObjectAnimator.ofFloat(shieldIcon, "scaleX", 0.5f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(shieldIcon, "scaleY", 0.5f, 1.1f, 1f)
        val rotation = ObjectAnimator.ofFloat(shieldIcon, "rotation", -10f, 10f, 0f)
        val alpha = ObjectAnimator.ofFloat(shieldIcon, "alpha", 0f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, rotation, alpha)
        animatorSet.duration = 800
        animatorSet.interpolator = OvershootInterpolator(1.2f)
        animatorSet.start()
    }

    private fun animateContentCard() {
        contentCard.alpha = 0f
        contentCard.translationY = 300f

        contentCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()
    }

    private fun animateStatsCards() {
        // Initially hide cards
        threatsCard.alpha = 0f
        threatsCard.scaleX = 0.8f
        threatsCard.scaleY = 0.8f

        alertsCard.alpha = 0f
        alertsCard.scaleX = 0.8f
        alertsCard.scaleY = 0.8f

        // Animate threats card
        Handler(Looper.getMainLooper()).postDelayed({
            threatsCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(OvershootInterpolator(1.3f))
                .start()
        }, 300)

        // Animate alerts card with delay
        Handler(Looper.getMainLooper()).postDelayed({
            alertsCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(OvershootInterpolator(1.3f))
                .start()
        }, 450)
    }

    private fun animateToolsSection() {
        toolsHeader.alpha = 0f
        toolsHeader.translationX = -100f

        Handler(Looper.getMainLooper()).postDelayed({
            toolsHeader.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(500)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, 600)

        // Animate tool cards one by one
        animateToolCard(appScanCard, 700)
        animateToolCard(linkGuardCard, 800)
        animateToolCard(callerIdCard, 900)
        animateToolCard(wifiGuardCard, 1000)
    }

    private fun animateToolCard(card: CardView, delay: Long) {
        card.alpha = 0f
        card.translationY = 50f

        Handler(Looper.getMainLooper()).postDelayed({
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, delay)
    }

    private fun animateCardClick(card: CardView) {
        val scaleDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(card, "scaleX", 1f, 0.95f),
                ObjectAnimator.ofFloat(card, "scaleY", 1f, 0.95f)
            )
            duration = 100
        }

        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(card, "scaleX", 0.95f, 1f),
                ObjectAnimator.ofFloat(card, "scaleY", 0.95f, 1f)
            )
            duration = 100
        }

        scaleDown.start()
        Handler(Looper.getMainLooper()).postDelayed({
            scaleUp.start()
        }, 100)
    }

    private fun navigateToItem(index: Int) {
        if (currentActiveIndex == index) return

        when (currentActiveIndex) {
            0 -> animateItemOut(iconHome)
            1 -> animateItemOut(iconTools)
            2 -> {}
            3 -> animateItemOut(iconReport)
            4 -> animateItemOut(iconStats)
        }

        when (index) {
            0 -> animateItemIn(iconHome)
            1 -> animateItemIn(iconTools)
            2 -> animateScanButton()
            3 -> animateItemIn(iconReport)
            4 -> animateItemIn(iconStats)
        }

        currentActiveIndex = index
    }

    private fun animateItemOut(icon: ImageView) {
        val iconColor = ValueAnimator.ofArgb(
            ContextCompat.getColor(this, R.color.icon_active),
            ContextCompat.getColor(this, R.color.icon_inactive)
        )
        iconColor.addUpdateListener { animator ->
            icon.setColorFilter(animator.animatedValue as Int)
        }
        iconColor.duration = 200
        iconColor.start()
    }

    private fun animateItemIn(icon: ImageView) {
        val scaleX = ObjectAnimator.ofFloat(icon, "scaleX", 0.7f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(icon, "scaleY", 0.7f, 1.2f, 1f)

        val iconColor = ValueAnimator.ofArgb(
            ContextCompat.getColor(this, R.color.icon_inactive),
            ContextCompat.getColor(this, R.color.icon_active)
        )
        iconColor.addUpdateListener { animator ->
            icon.setColorFilter(animator.animatedValue as Int)
        }

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, iconColor)
        animatorSet.duration = 300
        animatorSet.interpolator = OvershootInterpolator(2f)
        animatorSet.start()
    }

    private fun animateScanButton() {
        val scaleX = ObjectAnimator.ofFloat(navScanContainer, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(navScanContainer, "scaleY", 1f, 1.2f, 1f)
        val rotation = ObjectAnimator.ofFloat(iconScan, "rotation", 0f, 360f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, rotation)
        animatorSet.duration = 500
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.start()

        Toast.makeText(this, "Scan", Toast.LENGTH_SHORT).show()
    }

    private fun setInitialNavbarState() {
        iconHome.setColorFilter(ContextCompat.getColor(this, R.color.icon_active))
        iconTools.setColorFilter(ContextCompat.getColor(this, R.color.icon_inactive))
        iconReport.setColorFilter(ContextCompat.getColor(this, R.color.icon_inactive))
        iconStats.setColorFilter(ContextCompat.getColor(this, R.color.icon_inactive))
    }

    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                database = FirebaseDatabase.getInstance().reference
                    .child("users").child(user.uid).child("profile")
                listenToFirebaseData()
            }
        }
    }

    private fun listenToFirebaseData() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateStats() {
        val prefs = getSharedPreferences("SecureBharatPrefs", Context.MODE_PRIVATE)
        val threats = prefs.getInt("threat_count", 0)
        val alerts = prefs.getInt("alert_count", 0)

        threatCountText.text = threats.toString()
        alertCountText.text = alerts.toString()
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    override fun onResume() {
        super.onResume()
        updateStats()
    }
}