package com.example.appblock.summary

import com.example.appblock.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppUsageAdapter(private val appUsageList: List<AppUsageInfo>) :
    RecyclerView.Adapter<AppUsageAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.imageViewAppIcon)
        val appName: TextView = view.findViewById(R.id.textViewAppName)
        val usageTime: TextView = view.findViewById(R.id.textViewUsageTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_usage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = appUsageList[position]
        holder.appIcon.setImageDrawable(item.appIcon)
        holder.appName.text = item.appName
        holder.usageTime.text = item.usageTime
    }

    override fun getItemCount() = appUsageList.size
}