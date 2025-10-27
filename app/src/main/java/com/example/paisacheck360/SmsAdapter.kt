package com.example.paisacheck360

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class SmsData(
    val id: String = "",
    val sender: String = "",
    val body: String = "",
    val risk: String = "",
    val timestamp: Long = 0L
)

class SmsAdapter(private val dataList: MutableList<SmsData>) : RecyclerView.Adapter<SmsAdapter.VH>() {

    private var filteredList: List<SmsData> = dataList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val d = filteredList[position]
        holder.t1.text = "${d.sender} • ${formatTime(d.timestamp)}"
        val short = if (d.body.length > 160) d.body.substring(0, 160) + "..." else d.body
        holder.t2.text = "$short\nRisk: ${d.risk}"

        holder.t2.setTextColor(
            when (d.risk) {
                "High" -> Color.parseColor("#B00020")
                "Medium" -> Color.parseColor("#FF8C00")
                "Low" -> Color.parseColor("#0077CC")
                "Suspicious" -> Color.parseColor("#E76F51")
                else -> Color.parseColor("#198754")
            }
        )
    }

    override fun getItemCount(): Int = filteredList.size

    fun updateData(newList: List<SmsData>) {
        dataList.clear()
        dataList.addAll(newList)
        filteredList = dataList
        notifyDataSetChanged()
    }

    fun filter(query: String, risk: String) {
        val q = query.lowercase()
        filteredList = dataList.filter {
            (it.sender.lowercase().contains(q) || it.body.lowercase().contains(q)) &&
                    (risk == "All" || it.risk == risk)
        }
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val t1: TextView = view.findViewById(android.R.id.text1)
        val t2: TextView = view.findViewById(android.R.id.text2)
    }

    private fun formatTime(ts: Long): String {
        if (ts <= 0) return ""
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy • hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(ts))
    }
}
