package com.example.paisacheck360.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class SmsRequest(val message: String)
data class SmsResponse(val label: String, val confidence: Double)

interface ApiService {
    @POST("/api/predict")
    fun predictSms(@Body body: SmsRequest): Call<SmsResponse>
}
