package com.example.appblock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(var apps: MutableList<AppInfo>, private val showRemove: Boolean = false ) :

    RecyclerView.Adapter<AppAdapter.ViewHolder>() {
    var onRemoveClick: ((AppInfo) -> Unit)? = null

    fun updateList(newList: List<AppInfo>) {
        apps = newList.toMutableList()
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.app_icon)
        val appName: TextView = view.findViewById(R.id.app_name)
        val appPackage: TextView = view.findViewById(R.id.app_package)
        val checkBox: CheckBox = view.findViewById(R.id.check_box)
        val removeButton: Button = view.findViewById(R.id.btn_remove)
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

        // Make checkbox non-interactable
        holder.checkBox.isEnabled = false  // Disables interaction
        holder.checkBox.isChecked = app.isBlocked

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(app)
        }

        if (showRemove) {
            holder.checkBox.visibility = View.GONE
            holder.removeButton.visibility = View.VISIBLE
            holder.removeButton.setOnClickListener {
                onRemoveClick?.invoke(app)
            }
        } else {
            holder.checkBox.visibility = View.VISIBLE
            holder.removeButton.visibility = View.GONE
        }
    }

    override fun getItemCount() = apps.size
}