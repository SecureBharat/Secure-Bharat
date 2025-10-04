package com.example.paisacheck360.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class SafeBrowsingClient(private val apiKey: String) {

    private val client = OkHttpClient()

    /**
     * Checks if a URL is flagged by Google Safe Browsing
     * @return Pair<isMalicious, rawResponse>
     */
    suspend fun isUrlMalicious(url: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val apiUrl =
                "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=$apiKey"

            val bodyJson = JSONObject()
                .put("client", JSONObject()
                    .put("clientId", "secure-bharat")
                    .put("clientVersion", "1.0"))
                .put("threatInfo", JSONObject()
                    .put("threatTypes", listOf(
                        "MALWARE",
                        "SOCIAL_ENGINEERING",
                        "UNWANTED_SOFTWARE",
                        "POTENTIALLY_HARMFUL_APPLICATION",
                        "PHISHING"
                    ))
                    .put("platformTypes", listOf("ANY_PLATFORM"))
                    .put("threatEntryTypes", listOf("URL"))
                    .put("threatEntries", listOf(
                        JSONObject().put("url", url)
                    ))
                )

            val requestBody = bodyJson.toString()
                .toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: "{}"

                // 🔎 Print the raw Safe Browsing response in Logcat
                Log.d("SafeBrowsingClient", "URL checked: $url")
                Log.d("SafeBrowsingClient", "Safe Browsing Response: $respStr")

                // If response has "matches", then it's flagged
                val isMalicious = respStr.contains("matches")
                return@withContext Pair(isMalicious, respStr)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SafeBrowsingClient", "Error checking URL: ${e.message}")
            return@withContext Pair(false, "Error: ${e.message}")
        }
    }
}
