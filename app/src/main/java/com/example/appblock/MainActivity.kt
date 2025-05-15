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
import android.view.animation.AnimationUtils
import android.widget.Switch
import androidx.core.app.ActivityOptionsCompat

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

//        val adapter = AppAdapter(getInstalledApps())
//        recyclerView.adapter = adapter

            adapter = AppAdapter(apps,storage)
            recyclerView.adapter = adapter

            // Handle item clicks
            adapter.onItemClick = { app ->
                showAppDetailsDialog(app)
            }

            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_dark) // Create this drawable
            handleEditDeepLink()
        } catch (e:Exception) {
            Log.e("CRASH", "MainActivity failed: ${e.stackTraceToString()}")
            finish()
        }
    }

    private fun handleEditDeepLink() {
        val packageToEdit = intent.getStringExtra("EDIT_APP")
        packageToEdit?.let { pkg ->
            val appToEdit = adapter.apps.firstOrNull { it.packageName == pkg }
            appToEdit?.let {
                // Post to ensure UI is ready
                findViewById<RecyclerView>(R.id.apps_recycler_view).post {
                    showAppDetailsDialog(it)
                }
            }
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

        // ====== 2. BLOCKING SETTINGS ======
        val checkBox = dialogView.findViewById<CheckBox>(R.id.dialog_checkbox)
        val optionsContainer = dialogView.findViewById<LinearLayout>(R.id.options_container)
        val delaySwitch = dialogView.findViewById<Switch>(R.id.switch_delay)
        val delayInputContainer = dialogView.findViewById<LinearLayout>(R.id.delay_input_container)
        val delayInput = dialogView.findViewById<EditText>(R.id.delay_input)
        val timeSwitch = dialogView.findViewById<Switch>(R.id.switch_time_block)
        val timeContainer = dialogView.findViewById<LinearLayout>(R.id.time_block_container)
        val txtTimeRange = dialogView.findViewById<TextView>(R.id.txt_time_range)

        // Initial state
        checkBox.isChecked = app.isBlocked
        optionsContainer.visibility = if (app.isBlocked) View.VISIBLE else View.GONE

        // Configure delay settings
        delaySwitch.isChecked = app.blockDelay > 0
        delayInputContainer.visibility = if (delaySwitch.isChecked) View.VISIBLE else View.GONE
        delayInput.setText(if (app.blockDelay > 0) app.blockDelay.toString() else "10")

        // Configure time settings
        val isTimeEnabled = storage.isTimeRestrictionEnabled(app.packageName)
        timeSwitch.isChecked = isTimeEnabled
        timeContainer.visibility = if (isTimeEnabled) View.VISIBLE else View.GONE

        var currentStartTime: Pair<Int, Int>? = null
        var currentEndTime: Pair<Int, Int>? = null
        storage.getTimeRanges(app.packageName).firstOrNull()?.let {
            currentStartTime = it.startHour to it.startMinute
            currentEndTime = it.endHour to it.endMinute
            updateTimeDisplay(txtTimeRange, currentStartTime, currentEndTime)
        }

        // ====== 3. EVENT LISTENERS ======
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            optionsContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        delaySwitch.setOnCheckedChangeListener { _, isChecked ->
            delayInputContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                delayInput.setText("10") // Reset to default when disabled
            }
        }

        timeSwitch.setOnCheckedChangeListener { _, isChecked ->
            timeContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            txtTimeRange.error = null // Clear error when toggling

            if (isChecked && currentStartTime == null && currentEndTime == null) {
                currentStartTime = 9 to 0  // Default start time
                currentEndTime = 17 to 0   // Default end time
                updateTimeDisplay(txtTimeRange, currentStartTime, currentEndTime)
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

                    dialogView.findViewById<TextView>(R.id.txt_time_error).apply {
                        visibility = View.GONE
                        clearAnimation()
                    }

                    txtTimeRange.error = null
                    updateTimeDisplay(txtTimeRange, currentStartTime, currentEndTime)
                },
                initialHour,
                initialMinute,
                true
            ).show()
        }

        dialogView.findViewById<Button>(R.id.btn_set_start_time).setOnClickListener {
            showTimePicker(TIME_PICKER_START)
        }
        dialogView.findViewById<Button>(R.id.btn_set_end_time).setOnClickListener {
            showTimePicker(TIME_PICKER_END)
        }


        // ====== 5. SAVE HANDLER ======
        dialogView.findViewById<Button>(R.id.btn_ok).setOnClickListener {
            // Save blocking state
            val isBlocked = checkBox.isChecked
            val blockedApps = storage.getBlockedApps().toMutableSet().apply {
                if (isBlocked) add(app.packageName) else remove(app.packageName)
            }
            storage.saveBlockedApps(blockedApps)
            app.isBlocked = isBlocked

            // Save delay configuration
            val delay = if (delaySwitch.isChecked) {
                try {
                    delayInput.text.toString().toInt().coerceAtLeast(1)
                } catch (e: Exception) {
                    delayInput.error = getString(R.string.invalid_delay)
                    return@setOnClickListener
                }
            } else {
                0 // Clear delay when switch is off
            }
            storage.saveBlockDelay(app.packageName, delay)
            app.blockDelay = delay

            // Save time configuration
            val isTimeEnabled = timeSwitch.isChecked
            storage.saveTimeRestrictionEnabled(app.packageName, isTimeEnabled)

            if (isTimeEnabled) {
                if (currentStartTime == null || currentEndTime == null) {
                    txtTimeRange.error = "Set start/end times"
                    return@setOnClickListener
                }

                val timeRange = TimeRange(
                    currentStartTime!!.first,
                    currentStartTime!!.second,
                    currentEndTime!!.first,
                    currentEndTime!!.second
                )

                // validate the time range
                if (!timeRange.isValid()) {
                    val errorText = dialogView.findViewById<TextView>(R.id.txt_time_error)
                    errorText.visibility = View.VISIBLE
                    errorText.text = "ⓘ End time must be after start time"

                    // shake animation
                    val shake = AnimationUtils.loadAnimation(this@MainActivity, R.anim.shake)
                    errorText.startAnimation(shake)

                    return@setOnClickListener // Prevent dialog dismissal
                } else {
                    // Clear error and animation
                    dialogView.findViewById<TextView>(R.id.txt_time_error).apply {
                        visibility = View.GONE
                        clearAnimation()
                    }
                }

                storage.saveTimeRanges(app.packageName, listOf(timeRange))
                app.timeRanges = listOf(timeRange)
            } else {
                storage.saveTimeRanges(app.packageName, emptyList())
                app.timeRanges = emptyList()
            }

            // Update UI
            adapter.notifyItemChanged(
                adapter.apps.indexOfFirst { it.packageName == app.packageName }
            )
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
            textView.text = "No time set"
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



    private fun returnToDashboard() {
        if (isTaskRoot) {
            super.onBackPressed()
        } else {
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this,
                R.anim.slide_in_left,  // Return enter animation
                R.anim.slide_out_right   // Return exit animation
            )
            startActivity(Intent(this, DashboardActivity::class.java), options.toBundle())
            finish()
        }
    }

    // Handle back arrow (action bar)
    override fun onSupportNavigateUp(): Boolean {
        returnToDashboard()
        return true // Indicate we've handled the event
    }

    // Handle system back button
    override fun onBackPressed() {
        super.onBackPressed()
        returnToDashboard()
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
        val blockedApps = storage.getBlockedApps()

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
            if (ExcludedApps.isExcluded(packageName)) continue

            val appName = info.loadLabel(pm)?.toString() ?: "Unknown App"
            val icon = info.loadIcon(pm)
            val isBlocked = blockedApps.contains(packageName)

            // Load saved configurations for this app
            val blockDelay = storage.getBlockDelay(packageName)
            val timeRanges = storage.getTimeRanges(packageName)

            apps.add(AppInfo(
                name = appName,
                packageName = packageName,
                icon = icon,
                isBlocked = isBlocked,
                blockDelay = blockDelay,
                timeRanges = timeRanges
            ))
        }

        return apps.sortedBy { it.name }.toMutableList()
    }
}