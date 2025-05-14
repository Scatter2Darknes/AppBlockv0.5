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
        val app = apps[position]

        // Existing bindings
        holder.appName.text = app.name
        holder.appPackage.text = app.packageName
        holder.appIcon.setImageDrawable(app.icon)
        holder.checkBox.isChecked = app.isBlocked
        holder.checkBox.isEnabled = false

        // ====== NEW: Configuration-Specific Visibility ======
        // Show delay only if explicitly configured (>0)
        val delayVisible = app.blockDelay > 0
        holder.txtDelay.visibility = if (delayVisible) View.VISIBLE else View.GONE
        holder.txtDelay.text = "Delay: ${app.blockDelay}s"

        // Show time window only if time restrictions are enabled AND ranges exist
        val timeVisible = app.timeRanges.isNotEmpty() &&
                storage.isTimeRestrictionEnabled(app.packageName) // Add storage to adapter
        holder.txtTimeWindow.visibility = if (timeVisible) View.VISIBLE else View.GONE
        holder.txtTimeWindow.text = buildString {
            app.timeRanges.forEach { range ->
                append("Locked: %02d:%02d - %02d:%02d".format(
                    range.startHour,
                    range.startMinute,
                    range.endHour,
                    range.endMinute
                ))
            }
        }

        // Remove button/checkbox visibility logic (keep existing)
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