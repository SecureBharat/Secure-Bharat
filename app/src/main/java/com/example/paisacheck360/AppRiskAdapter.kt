package com.example.paisacheck360

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppRiskAdapter(private val apps: List<AppRiskInfo>) :
    RecyclerView.Adapter<AppRiskAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val appName: TextView = view.findViewById(R.id.appName)
        val packageName: TextView = view.findViewById(R.id.packageName)
        val riskBadge: LinearLayout = view.findViewById(R.id.riskBadge)
        val riskEmoji: TextView = view.findViewById(R.id.riskEmoji)
        val riskScore: TextView = view.findViewById(R.id.riskScore)
        val riskLevel: TextView = view.findViewById(R.id.riskLevel)
        val dangerousPermissions: TextView = view.findViewById(R.id.dangerousPermissions)
        val totalPermissions: TextView = view.findViewById(R.id.totalPermissions)
        val systemApp: TextView = view.findViewById(R.id.systemApp)
        val detailsContainer: LinearLayout = view.findViewById(R.id.detailsContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_risk, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]

        // Set app info
        holder.appIcon.setImageDrawable(app.icon)
        holder.appName.text = app.appName
        holder.packageName.text = app.packageName

        // Set risk badge
        holder.riskBadge.setBackgroundColor(app.riskLevel.color)
        holder.riskEmoji.text = app.riskLevel.emoji
        holder.riskScore.text = app.riskScore.toString()

        // Set risk level
        holder.riskLevel.text = "${app.riskLevel.emoji} ${app.riskLevel.displayName}"
        holder.riskLevel.setTextColor(app.riskLevel.color)

        // Set dangerous permissions
        if (app.dangerousPermissions.isEmpty()) {
            holder.dangerousPermissions.text = "✅ None detected"
            holder.dangerousPermissions.setTextColor(0xFF4CAF50.toInt())
        } else {
            holder.dangerousPermissions.text = app.dangerousPermissions.joinToString("\n")
            holder.dangerousPermissions.setTextColor(0xFF666666.toInt())
        }

        // Set other details
        holder.totalPermissions.text = app.totalPermissions.toString()
        holder.systemApp.text = if (app.isSystemApp) "Yes" else "No"

        // Toggle details on click
        holder.itemView.setOnClickListener {
            val isVisible = holder.detailsContainer.visibility == View.VISIBLE
            holder.detailsContainer.visibility = if (isVisible) View.GONE else View.VISIBLE
        }
    }

    override fun getItemCount() = apps.size
}