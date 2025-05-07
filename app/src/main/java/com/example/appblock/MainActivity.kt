package com.example.appblock

import TimeRange
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
// In MainActivity.kt
import android.content.SharedPreferences
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import android.app.TimePickerDialog
import android.widget.Switch

class MainActivity : AppCompatActivity() {

    private lateinit var storage: StorageHelper
    private lateinit var adapter: AppAdapter // Make adapter a class property
    companion object {
        const val TIME_PICKER_START = 0
        const val TIME_PICKER_END = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            // Initialize storage FIRST
            storage = StorageHelper(applicationContext) // MOVED THIS LINE UP

            val apps = getInstalledApps().apply {
                forEach { app ->
                    app.isBlocked = storage.getBlockedApps().contains(app.packageName)
                }
            }

            // Toolbar setup
            setSupportActionBar(findViewById(R.id.toolbar))
            supportActionBar?.title = "Select Apps"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            val recyclerView = findViewById<RecyclerView>(R.id.apps_recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(this)


            adapter = AppAdapter(apps)
            recyclerView.adapter = adapter

            // Handle item clicks
            adapter.onItemClick = { app ->
                showAppDetailsDialog(app)
            }

            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_dark) // Create this drawable
        } catch (e:Exception) {
            Log.e("CRASH", "MainActivity failed: ${e.stackTraceToString()}")
            finish()
        }
    }

    private fun showAppDetailsDialog(app: AppInfo) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_app_details, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        // ====== 1. POPULATE APP DETAILS ======
        dialogView.findViewById<ImageView>(R.id.dialog_app_icon).apply {
            setImageDrawable(app.icon)
            contentDescription = "${app.name} icon"
        }

        dialogView.findViewById<TextView>(R.id.dialog_app_name).apply {
            text = app.name
            setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.black))
        }

        dialogView.findViewById<TextView>(R.id.dialog_package_name).apply {
            text = app.packageName
            setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.darker_gray))
        }

        // ====== 2. BLOCKING SETTINGS ======
        val checkBox = dialogView.findViewById<CheckBox>(R.id.dialog_checkbox)
        val delayContainer = dialogView.findViewById<LinearLayout>(R.id.delay_container)
        val delayInput = dialogView.findViewById<EditText>(R.id.delay_input)

        // Delay configuration
        val savedDelay = storage.getBlockDelay(app.packageName)
        delayInput.setText(savedDelay.toString())
        checkBox.isChecked = app.isBlocked
        delayContainer.visibility = if (app.isBlocked) View.VISIBLE else View.GONE

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            delayContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // ====== 3. TIME RESTRICTIONS ======
        val timeSwitch = dialogView.findViewById<Switch>(R.id.switch_time_block)
        val btnStart = dialogView.findViewById<Button>(R.id.btn_set_start_time)
        val btnEnd = dialogView.findViewById<Button>(R.id.btn_set_end_time)
        val txtTimeRange = dialogView.findViewById<TextView>(R.id.txt_time_range)
        val timeContainer = dialogView.findViewById<LinearLayout>(R.id.time_block_container)

        // Load time restriction state FIRST
        val isTimeEnabled = storage.isTimeRestrictionEnabled(app.packageName)
        timeContainer.visibility = if (isTimeEnabled) View.VISIBLE else View.GONE
        timeSwitch.isChecked = isTimeEnabled

        // Load existing time ranges (even if switch is off)
        val timeRanges = storage.getTimeRanges(app.packageName)
        var currentStartTime: Pair<Int, Int>? = null
        var currentEndTime: Pair<Int, Int>? = null

        if (timeRanges.isNotEmpty()) {
            val firstRange = timeRanges.first()
            currentStartTime = firstRange.startHour to firstRange.startMinute
            currentEndTime = firstRange.endHour to firstRange.endMinute
            updateTimeDisplay(txtTimeRange, currentStartTime, currentEndTime)
        }

        // Set visibility based on saved state
        timeContainer.visibility = if (isTimeEnabled) View.VISIBLE else View.GONE




        timeSwitch.setOnCheckedChangeListener { _, isChecked ->
            timeContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            // Refresh time display when enabling
            if (isChecked) {
                val timeRanges = storage.getTimeRanges(app.packageName)
                if (timeRanges.isNotEmpty()) {
                    val firstRange = timeRanges.first()
                    currentStartTime = firstRange.startHour to firstRange.startMinute
                    currentEndTime = firstRange.endHour to firstRange.endMinute
                    updateTimeDisplay(txtTimeRange, currentStartTime, currentEndTime)
                }
            }
        }

        // ====== 4. TIME PICKERS ======
        fun showTimePicker(type: Int) {
            val initialHour = when (type) {
                TIME_PICKER_START -> currentStartTime?.first ?: 0
                else -> currentEndTime?.first ?: 0
            }
            val initialMinute = when (type) {
                TIME_PICKER_START -> currentStartTime?.second ?: 0
                else -> currentEndTime?.second ?: 0
            }

            TimePickerDialog(
                this,
                { _, hour, minute ->
                    when (type) {
                        TIME_PICKER_START -> currentStartTime = hour to minute
                        TIME_PICKER_END -> currentEndTime = hour to minute
                    }
                    txtTimeRange.error = null
                    updateTimeDisplay(txtTimeRange, currentStartTime, currentEndTime)
                },
                initialHour,
                initialMinute,
                true
            ).show()
        }

        btnStart.setOnClickListener { showTimePicker(TIME_PICKER_START) }
        btnEnd.setOnClickListener { showTimePicker(TIME_PICKER_END) }

        // ====== 5. SAVE HANDLER ======
        dialogView.findViewById<Button>(R.id.btn_ok).setOnClickListener {
            // Save blocking state
            val isBlocked = checkBox.isChecked
            val blockedApps = storage.getBlockedApps().toMutableSet().apply {
                if (isBlocked) add(app.packageName) else remove(app.packageName)
            }
            storage.saveBlockedApps(blockedApps)
            app.isBlocked = isBlocked

            // Save delay
            try {
                val delay = delayInput.text.toString().toInt().coerceAtLeast(1)
                storage.saveBlockDelay(app.packageName, delay)
                app.blockDelay = delay
            } catch (e: NumberFormatException) {
                delayInput.error = getString(R.string.invalid_delay)
                return@setOnClickListener
            }

            // Save time restrictions
            val isTimeEnabled = timeSwitch.isChecked
            storage.saveTimeRestrictionEnabled(app.packageName, isTimeEnabled)

            // Always save time ranges if valid (regardless of switch state)
            if (currentStartTime != null && currentEndTime != null) {
                val timeRange = TimeRange(
                    currentStartTime!!.first,
                    currentStartTime!!.second,
                    currentEndTime!!.first,
                    currentEndTime!!.second
                )
                if (timeRange.isValid()) {
                    storage.saveTimeRanges(app.packageName, listOf(timeRange))
                    app.timeRanges = listOf(timeRange)
                } else if (isTimeEnabled) { // Only validate if enabled
                    txtTimeRange.error = "Invalid time range"
                    return@setOnClickListener
                }
            } else if (isTimeEnabled) { // Only require times if enabled
                txtTimeRange.error = "Set start/end times"
                return@setOnClickListener
            }

            // Always save enabled state
            storage.saveTimeRestrictionEnabled(app.packageName, isTimeEnabled)

            adapter.notifyItemChanged(adapter.apps.indexOfFirst { it.packageName == app.packageName })
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateTimeDisplay(
        textView: TextView,
        start: Pair<Int, Int>?,
        end: Pair<Int, Int>?
    ) {
        if (start == null || end == null) {
            textView.text = "Time range saved but not active" // Changed text
            return
        }

        textView.text = String.format(
            "%02d:%02d - %02d:%02d",
            start.first,
            start.second,
            end.first,
            end.second
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        finish() // Close this activity and return to dashboard
        return true
    }

    override fun onResume() {
        super.onResume()
        // Refresh list when returning from background
        adapter.apps = getInstalledApps()
        adapter.notifyDataSetChanged()
    }

    private fun getInstalledApps(): MutableList<AppInfo> {
        val pm = packageManager
        val apps = mutableListOf<AppInfo>()
        val blockedApps = storage.getBlockedApps() // get all blocked apps

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = try {
            pm.queryIntentActivities(intent, 0)
        } catch (e: Exception) {
            emptyList()
        }

        for (info in resolveInfos) {
            val packageName = info.activityInfo.packageName
            // Skip excluded apps
            if (ExcludedApps.isExcluded(packageName)) continue

            val appName = info.loadLabel(pm).toString() ?: "Unknown App"
            val icon = info.loadIcon(pm)
            apps.add(AppInfo(
                name = appName,
                packageName = packageName,
                icon = icon,
                isBlocked = blockedApps.contains(packageName) // Check against full set
            ))
        }

        return apps.sortedBy { it.name }.toMutableList()
    }
}