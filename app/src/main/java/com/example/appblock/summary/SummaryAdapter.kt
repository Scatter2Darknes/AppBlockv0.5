package com.example.appblock.summary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appblock.AppInfo
import com.example.appblock.R

class SummaryAdapter(private val apps: List<AppInfo>) :
    RecyclerView.Adapter<SummaryAdapter.SummaryViewHolder>() {  // Changed to SummaryViewHolder

    inner class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.summary_app_icon)
        val appName: TextView = itemView.findViewById(R.id.summary_app_name)
        val packageName: TextView = itemView.findViewById(R.id.summary_package_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.summary_list_item, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        val app = apps[position]
        holder.appIcon.setImageDrawable(app.icon)
        holder.appName.text = app.name
        holder.packageName.text = app.packageName
    }

    override fun getItemCount() = apps.size
}