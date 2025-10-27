package com.example.paisacheck360

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ScamReportActivity : AppCompatActivity() {

    private lateinit var db: DatabaseReference
    private lateinit var scamContainer: LinearLayout
    private lateinit var totalScams: TextView
    private lateinit var highRisk: TextView
    private lateinit var blocked: TextView
    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scam_report)

        scamContainer = findViewById(R.id.scamLogsContainer)
        totalScams = findViewById(R.id.totalScamsCount)
        highRisk = findViewById(R.id.highRiskCount)
        blocked = findViewById(R.id.blockedCount)
        backButton = findViewById(R.id.backButton)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        db = FirebaseDatabase.getInstance().getReference("users/$uid/alerts")

        loadReportData()

        backButton.setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.exportPdfBtn).setOnClickListener {
            Toast.makeText(this, "📄 Exporting to PDF coming soon!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.shareReportBtn).setOnClickListener {
            Toast.makeText(this, "📱 Share option coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadReportData() {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scamContainer.removeAllViews()
                var total = 0
                var high = 0
                var blockedCount = 0

                for (child in snapshot.children) {
                    val sender = child.child("sender").getValue(String::class.java) ?: ""
                    val body = child.child("body").getValue(String::class.java) ?: ""
                    val risk = child.child("risk").getValue(String::class.java) ?: "Safe"
                    if (risk != "Safe") total++

                    if (risk == "High" || risk == "Critical") high++

                    // Create a scam card dynamically
                    val card = LayoutInflater.from(this@ScamReportActivity)
                        .inflate(R.layout.item_scam_card, scamContainer, false)

                    val riskBadge = card.findViewById<TextView>(R.id.riskBadge)
                    val senderText = card.findViewById<TextView>(R.id.senderText)
                    val messageText = card.findViewById<TextView>(R.id.messageText)
                    val timeText = card.findViewById<TextView>(R.id.timeText)
                    val reportBtn = card.findViewById<Button>(R.id.reportBtn)
                    val blockBtn = card.findViewById<Button>(R.id.blockBtn)

                    // Bind data
                    senderText.text = "From: $sender"
                    messageText.text = body
                    timeText.text = android.text.format.DateFormat.format("dd MMM, hh:mm a",
                        child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis())

                    riskBadge.text = when (risk) {
                        "High" -> "🔴 HIGH RISK"
                        "Medium" -> "🟠 MEDIUM"
                        "Low" -> "🟡 LOW"
                        else -> "🟢 SAFE"
                    }

                    val bgColor = when (risk) {
                        "High" -> "#FFEBEE"
                        "Medium" -> "#FFF3E0"
                        "Low" -> "#FFF9C4"
                        else -> "#E8F5E9"
                    }
                    card.setBackgroundColor(android.graphics.Color.parseColor(bgColor))

                    reportBtn.setOnClickListener {
                        Toast.makeText(this@ScamReportActivity, "Reported to cyber cell 🚨", Toast.LENGTH_SHORT).show()
                    }

                    blockBtn.setOnClickListener {
                        blockedCount++
                        blocked.text = blockedCount.toString()
                        Toast.makeText(this@ScamReportActivity, "Sender Blocked 🚫", Toast.LENGTH_SHORT).show()
                    }

                    scamContainer.addView(card)
                }

                totalScams.text = total.toString()
                highRisk.text = high.toString()
                blocked.text = blockedCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ScamReportActivity, "Error loading report", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
