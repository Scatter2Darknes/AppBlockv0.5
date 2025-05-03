package com.example.appblock

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

class MainActivity : AppCompatActivity() {

    private lateinit var storage: StorageHelper
    private lateinit var adapter: AppAdapter // Make adapter a class property

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
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // ====== 1. POPULATE APP DETAILS FIRST ======
        // App Icon
        dialogView.findViewById<ImageView>(R.id.dialog_app_icon).apply {
            setImageDrawable(app.icon)
            contentDescription = "${app.name} icon"
        }

        // App Name
        dialogView.findViewById<TextView>(R.id.dialog_app_name).apply {
            text = app.name
            setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.black))
        }

        // Package Name
        dialogView.findViewById<TextView>(R.id.dialog_package_name).apply {
            text = app.packageName
            setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.darker_gray))
        }

        // ====== 2. CONFIGURE BLOCKING SETTINGS AFTER ======
        // Initialize views
        val checkBox = dialogView.findViewById<CheckBox>(R.id.dialog_checkbox)
        val delayContainer = dialogView.findViewById<LinearLayout>(R.id.delay_container)
        val delayInput = dialogView.findViewById<EditText>(R.id.delay_input)

        // 1. Load saved delay (not default 10)
        val savedDelay = storage.getBlockDelay(app.packageName)
        delayInput.setText(savedDelay.toString())

        // 2. Initialize checkbox state and delay visibility
        checkBox.isChecked = app.isBlocked
        delayContainer.visibility = if (app.isBlocked) View.VISIBLE else View.GONE

        // 3. Update delay visibility IMMEDIATELY on checkbox change
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            delayContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // 4. Save changes ONLY when OK is clicked
        dialogView.findViewById<Button>(R.id.btn_ok).setOnClickListener {
            val isBlocked = checkBox.isChecked

            // Update blocking state
            val blockedApps = storage.getBlockedApps().toMutableSet().apply {
                if (isBlocked) add(app.packageName) else remove(app.packageName)
            }
            storage.saveBlockedApps(blockedApps)

            // Update delay
            try {
                val delay = delayInput.text.toString().toInt().coerceAtLeast(1)
                storage.saveBlockDelay(app.packageName, delay)
                app.blockDelay = delay
            } catch (e: NumberFormatException) {
                delayInput.error = getString(R.string.invalid_delay)
                return@setOnClickListener // Prevent dialog close on error
            }

            // Update UI
            app.isBlocked = isBlocked
            val position = adapter.apps.indexOfFirst { it.packageName == app.packageName }
            if (position != -1) adapter.notifyItemChanged(position)

            dialog.dismiss()
        }

        // Show dialog after setup
        dialog.show()
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
            // Skip excluded apps
            if (ExcludedApps.isExcluded(packageName)) continue

            val appName = info.loadLabel(pm).toString() ?: "Unknown App"
            val icon = info.loadIcon(pm)
            apps.add(AppInfo(appName, packageName, icon, blockedApps.contains(packageName)))
        }

        return apps.sortedBy { it.name }.toMutableList()
    }
}