package com.example.paisacheck360

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class AppRiskScannerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingText: TextView
    private lateinit var appCount: TextView
    private lateinit var criticalCount: TextView
    private lateinit var highCount: TextView
    private lateinit var mediumCount: TextView
    private lateinit var lowCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AppRiskScanner", "📱 Activity onCreate")

        setContentView(R.layout.activity_app_risk_scanner)

        Toast.makeText(this, "🔍 Scanning apps...", Toast.LENGTH_SHORT).show()

        initializeViews()
        startScanning()
    }

    private fun initializeViews() {
        Log.d("AppRiskScanner", "🎨 Initializing views")

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        loadingText = findViewById(R.id.loadingText)
        appCount = findViewById(R.id.appCount)
        criticalCount = findViewById(R.id.criticalCount)
        highCount = findViewById(R.id.highCount)
        mediumCount = findViewById(R.id.mediumCount)
        lowCount = findViewById(R.id.lowCount)

        recyclerView.layoutManager = LinearLayoutManager(this)

        Log.d("AppRiskScanner", "✅ Views initialized")
    }

    private fun startScanning() {
        Log.d("AppRiskScanner", "🔍 Starting full scan...")

        lifecycleScope.launch {
            try {
                // Show loading
                Log.d("AppRiskScanner", "📊 Showing loading UI")
                progressBar.visibility = View.VISIBLE
                loadingText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE

                // Scan apps with full analysis
                Log.d("AppRiskScanner", "🔎 Creating scanner...")
                val scanner = AppScanner(this@AppRiskScannerActivity)

                Log.d("AppRiskScanner", "📱 Scanning apps with risk analysis...")
                val apps = scanner.scanAllApps()
                Log.d("AppRiskScanner", "✅ Found ${apps.size} apps")

                Log.d("AppRiskScanner", "📊 Getting statistics...")
                val stats = scanner.getStatistics()
                Log.d("AppRiskScanner", "✅ Stats: Critical=${stats.criticalApps}, High=${stats.highRiskApps}, Medium=${stats.mediumRiskApps}, Low=${stats.lowRiskApps}")

                // Update UI
                Log.d("AppRiskScanner", "🎨 Updating UI...")
                updateStatistics(stats)
                displayApps(apps)

                // Hide loading
                progressBar.visibility = View.GONE
                loadingText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                Toast.makeText(
                    this@AppRiskScannerActivity,
                    "✅ Scanned ${apps.size} apps successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                Log.d("AppRiskScanner", "✅ Full scan complete!")

            } catch (e: Exception) {
                Log.e("AppRiskScanner", "❌ ERROR: ${e.message}", e)
                loadingText.text = "❌ Error: ${e.message}"
                progressBar.visibility = View.GONE

                Toast.makeText(
                    this@AppRiskScannerActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateStatistics(stats: AppScanStatistics) {
        appCount.text = "${stats.totalApps} apps"
        criticalCount.text = stats.criticalApps.toString()
        highCount.text = stats.highRiskApps.toString()
        mediumCount.text = stats.mediumRiskApps.toString()
        lowCount.text = stats.lowRiskApps.toString()
    }

    private fun displayApps(apps: List<AppRiskInfo>) {
        Log.d("AppRiskScanner", "📋 Displaying ${apps.size} apps in RecyclerView")
        val adapter = AppRiskAdapter(apps)
        recyclerView.adapter = adapter
        Log.d("AppRiskScanner", "✅ Adapter set successfully")
    }
}