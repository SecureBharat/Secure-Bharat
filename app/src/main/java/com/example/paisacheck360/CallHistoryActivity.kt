package com.example.paisacheck360

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class CallHistoryActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var searchButton: ImageView
    
    private lateinit var summaryCards: LinearLayout
    private lateinit var totalCallsCount: TextView
    private lateinit var blockedCallsCount: TextView
    private lateinit var safeCallsCount: TextView
    
    private lateinit var filterAll: CardView
    private lateinit var filterScam: CardView
    private lateinit var filterSafe: CardView
    private lateinit var filterMissed: CardView
    
    private lateinit var callHistoryList: LinearLayout
    private lateinit var emptyState: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_history)

        bindViews()
        setupClickListeners()
        loadCallHistory()
        
        // Start animations
        Handler(Looper.getMainLooper()).postDelayed({
            startEntryAnimations()
        }, 100)
    }

    private fun bindViews() {
        backButton = findViewById(R.id.backButton)
        searchButton = findViewById(R.id.searchButton)
        
        summaryCards = findViewById(R.id.summaryCards)
        totalCallsCount = findViewById(R.id.totalCallsCount)
        blockedCallsCount = findViewById(R.id.blockedCallsCount)
        safeCallsCount = findViewById(R.id.safeCallsCount)
        
        filterAll = findViewById(R.id.filterAll)
        filterScam = findViewById(R.id.filterScam)
        filterSafe = findViewById(R.id.filterSafe)
        filterMissed = findViewById(R.id.filterMissed)
        
        callHistoryList = findViewById(R.id.callHistoryList)
        emptyState = findViewById(R.id.emptyState)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }
        
        searchButton.setOnClickListener {
            // TODO: Implement search
            android.widget.Toast.makeText(this, "Search feature coming soon", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        // Filter clicks
        filterAll.setOnClickListener { selectFilter(filterAll) }
        filterScam.setOnClickListener { selectFilter(filterScam) }
        filterSafe.setOnClickListener { selectFilter(filterSafe) }
        filterMissed.setOnClickListener { selectFilter(filterMissed) }
    }

    private fun selectFilter(selectedFilter: CardView) {
        // Reset all filters
        resetFilter(filterAll)
        resetFilter(filterScam)
        resetFilter(filterSafe)
        resetFilter(filterMissed)
        
        // Activate selected filter
        selectedFilter.setCardBackgroundColor(getColor(R.color.primary))
        val textView = selectedFilter.getChildAt(0) as TextView
        textView.setTextColor(getColor(android.R.color.white))
        
        // Animate selection
        animateFilterSelection(selectedFilter)
    }

    private fun resetFilter(filter: CardView) {
        filter.setCardBackgroundColor(getColor(R.color.background))
        val textView = filter.getChildAt(0) as TextView
        textView.setTextColor(getColor(R.color.text_secondary))
    }

    private fun animateFilterSelection(filter: CardView) {
        val scaleX = ObjectAnimator.ofFloat(filter, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(filter, "scaleY", 1f, 1.1f, 1f)
        
        scaleX.duration = 200
        scaleY.duration = 200
        
        scaleX.start()
        scaleY.start()
    }

    private fun startEntryAnimations() {
        // Animate summary cards
        animateSummaryCards()
        
        // Animate filter chips
        animateFilterChips()
        
        // Animate call history list
        animateCallHistoryList()
    }

    private fun animateSummaryCards() {
        for (i in 0 until summaryCards.childCount) {
            val card = summaryCards.getChildAt(i)
            card.alpha = 0f
            card.translationY = 50f
            
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay((i * 100).toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun animateFilterChips() {
        val filterContainer = filterAll.parent as? LinearLayout
        filterContainer?.let { container ->
            container.alpha = 0f
            container.translationX = -100f
            
            Handler(Looper.getMainLooper()).postDelayed({
                container.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(400)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }, 300)
        }
    }

    private fun animateCallHistoryList() {
        for (i in 0 until callHistoryList.childCount) {
            val card = callHistoryList.getChildAt(i)
            card.alpha = 0f
            card.translationY = 30f
            
            Handler(Looper.getMainLooper()).postDelayed({
                card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }, 400 + (i * 100).toLong())
        }
    }

    private fun animateCountUp(textView: TextView, targetValue: Int) {
        val animator = android.animation.ValueAnimator.ofInt(0, targetValue)
        animator.duration = 800
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            textView.text = animation.animatedValue.toString()
        }
        animator.start()
    }

    private fun loadCallHistory() {
        // Animate count up for summary cards
        Handler(Looper.getMainLooper()).postDelayed({
            animateCountUp(totalCallsCount, 156)
            animateCountUp(blockedCallsCount, 23)
            animateCountUp(safeCallsCount, 133)
        }, 400)
        
        // Load actual call history from database
        // TODO: Implement Firebase/Room database fetch
        
        // Show/hide empty state based on data
        if (callHistoryList.childCount == 0) {
            callHistoryList.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            callHistoryList.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }
}
