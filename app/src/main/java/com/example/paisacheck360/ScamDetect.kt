package com.example.paisacheck360

object ScamDetector {
    fun checkForScam(message: String): String {
        // Example logic (replace with your real one)
        val scamKeywords = listOf("lottery", "win", "free", "urgent", "click")
        return if (scamKeywords.any { message.contains(it, ignoreCase = true) }) {
            "🚨 Scam Detected!"
        } else {
            "✅ Safe"
        }
    }
}
