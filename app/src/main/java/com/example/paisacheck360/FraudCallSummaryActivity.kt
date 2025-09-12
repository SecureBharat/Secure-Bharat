package com.example.paisacheck360

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FraudCallSummaryActivity : AppCompatActivity() {

    private lateinit var callListContainer: LinearLayout
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fraud_call_summary)

        callListContainer = findViewById(R.id.callListContainer)

        // ✅ Dummy values for now
        val dummyJson = """
            [
                {"caller":"Bank Alert Dept.","message":"This is the fraud department from your bank.","riskLevel":"High","date":"2025-08-19"},
                {"caller":"Local Police Dept.","message":"You are under investigation for suspicious activity.","riskLevel":"Medium","date":"2025-08-19"},
                {"caller":"Windows Support Center","message":"We've detected a virus on your computer.","riskLevel":"Low","date":"2025-08-19"}
            ]
        """

        val type = object : TypeToken<List<FraudCall>>() {}.type
        val callList: List<FraudCall> = gson.fromJson(dummyJson, type)

        showCalls(callList)
    }

    private fun showCalls(callList: List<FraudCall>) {
        callListContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        callList.forEach { call ->
            val itemView = inflater.inflate(android.R.layout.simple_list_item_2, callListContainer, false)
            val text1 = itemView.findViewById<TextView>(android.R.id.text1)
            val text2 = itemView.findViewById<TextView>(android.R.id.text2)

            text1.text = "Caller ID: ${call.caller} • Risk: ${call.riskLevel}"
            text2.text = call.message

            callListContainer.addView(itemView)
        }
    }
}

// ✅ Data class
data class FraudCall(
    val caller: String,
    val message: String,
    val riskLevel: String,
    val date: String
)
