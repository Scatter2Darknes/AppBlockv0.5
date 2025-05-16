package com.example.appblock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(var apps: MutableList<AppInfo>, private val storage: StorageHelper, private val showRemove: Boolean = false ) :

    RecyclerView.Adapter<AppAdapter.ViewHolder>() {
    var onRemoveClick: ((AppInfo) -> Unit)? = null

    fun updateList(newList: MutableList<AppInfo>) {
        apps = newList ?: mutableListOf() // Handle null input
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.app_icon)
        val appName: TextView = view.findViewById(R.id.app_name)
        //val appPackage: TextView = view.findViewById(R.id.app_package)
        val checkBox: CheckBox = view.findViewById(R.id.check_box)
        val removeButton: Button = view.findViewById(R.id.btn_remove)
        val txtDelay: TextView = view.findViewById(R.id.txt_delay)
        val txtTimeWindow: TextView = view.findViewById(R.id.txt_time_window)
    }

    // Add item click listener
    var onItemClick: ((AppInfo) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps.getOrNull(position) ?: return // Prevent IndexOutOfBounds

        // Basic bindings
        holder.appName.text = app.name
        holder.appIcon.setImageDrawable(app.icon)
        holder.checkBox.isChecked = app.isBlocked
        holder.checkBox.isEnabled = false

        // ====== Subtext Visibility Rules ======
        // 1. Only show subtexts if app is blocked
        val showSubtexts = app.isBlocked

        // 2. Delay subtext: Only show if delay is configured (>0)
        val delayVisible = showSubtexts && app.blockDelay > 0
        holder.txtDelay.visibility = if (delayVisible) View.VISIBLE else View.GONE
        holder.txtDelay.text = "Delay: ${app.blockDelay}s" // Now uses AppInfo's property

        // 3. Time subtext: Only show if time switch is ON AND ranges exist
        val timeEnabled = storage.isTimeRestrictionEnabled(app.packageName)
        val timeVisible = showSubtexts && timeEnabled && app.timeRanges.isNotEmpty()
        holder.txtTimeWindow.visibility = if (timeVisible) View.VISIBLE else View.GONE

        if (timeVisible) {
            holder.txtTimeWindow.text = buildString {
                app.timeRanges.forEach { range -> // Use AppInfo's timeRanges
                    append("Locked: %02d:%02d - %02d:%02d".format(
                        range.startHour,
                        range.startMinute,
                        range.endHour,
                        range.endMinute
                    ))
                }
            }
        }

        // Remove button logic
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

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(app)
        }
    }

    override fun getItemCount() = apps.size
}