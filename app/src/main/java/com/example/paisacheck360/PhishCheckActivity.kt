package com.example.paisacheck360

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.paisacheck360.network.LocalPhishModel
import com.example.paisacheck360.network.SafeBrowsingClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhishCheckActivity : AppCompatActivity() {

    private lateinit var resultText: TextView
    private lateinit var detailText: TextView
    private lateinit var openBtn: Button

    // 🔑 Your Google Safe Browsing API key
    private val safeBrowsingKey = "AIzaSyAs2QQMGRH1gWKC3aX0SdXCm87M4NUxFA0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phish_check)

        resultText = findViewById(R.id.resultText)
        detailText = findViewById(R.id.detailText)
        openBtn = findViewById(R.id.openButton)

        val scanned = intent.getStringExtra("scanned") ?: ""
        resultText.text = "🔍 Scanning: $scanned\nAnalyzing..."

        // 🚫 No LocalPhishModel.init() anymore

        lifecycleScope.launch {
            // Step 1: Local heuristic/model score
            val localScore = withContext(Dispatchers.Default) {
                LocalPhishModel.predict(scanned)  // Always available
            }

            // Show local result
            updateUI("Local analysis", scanned, localScore)

            // Step 2: Online Safe Browsing check
            if (safeBrowsingKey.isNotBlank()) {
                val sbClient = SafeBrowsingClient(safeBrowsingKey)
                val (isMalicious, _) = sbClient.isUrlMalicious(scanned)

                if (isMalicious) {
                    // 🚨 Override final: Dangerous
                    showFinalVerdict(scanned, 1.0f, "Google Safe Browsing flagged this link.")
                } else {
                    // 🟢 Merge: give Safe Browsing weight to reduce false positives
                    val finalScore = (0.7f * localScore) + (0.3f * 0.0f)
                    updateUI("Merged with Google Safe Browsing", scanned, finalScore)
                }
            }
        }
    }

    /** Show verdict from any stage */
    private fun updateUI(source: String, url: String, score: Float) {
        runOnUiThread {
            val verdict = when {
                score >= 0.75f -> "🚨 MALICIOUS"
                score >= 0.45f -> "⚠️ SUSPICIOUS"
                else -> "✅ SAFE"
            }

            val color = when {
                score >= 0.75f -> Color.RED
                score >= 0.45f -> Color.parseColor("#FF8800")
                else -> Color.parseColor("#2E7D32")
            }

            resultText.setTextColor(color)
            resultText.text = "$source verdict:\n$verdict\nScore: ${"%.2f".format(score)}\n\n$url"
        }
    }

    /** 🚨 Safe Browsing overrides */
    private fun showFinalVerdict(url: String, score: Float, reason: String) {
        runOnUiThread {
            resultText.setTextColor(Color.RED)
            resultText.text = "FINAL VERDICT: 🚨 DANGEROUS\nReason: $reason\n\n$url"
            detailText.text = "⚠️ This link is listed by Google Safe Browsing. Avoid opening it!"
        }
    }
}
