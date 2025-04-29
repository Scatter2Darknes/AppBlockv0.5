package com.example.appblock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(val apps: MutableList<AppInfo>) :
    RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.app_icon)
        val appName: TextView = view.findViewById(R.id.app_name)
        val appPackage: TextView = view.findViewById(R.id.app_package)
        val checkBox: CheckBox = view.findViewById(R.id.check_box)
    }

    // Add item click listener
    var onItemClick: ((AppInfo) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.appName.text = app.name
        holder.appPackage.text = app.packageName
        holder.appIcon.setImageDrawable(app.icon)
        holder.checkBox.isChecked = app.isBlocked // Bind checkbox state

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(app)
        }
    }

    override fun getItemCount() = apps.size
}