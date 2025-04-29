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
        } catch (e:Exception) {
            Log.e("CRASH", "MainActivity failed: ${e.stackTraceToString()}")
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showAppDetailsDialog(app: AppInfo) {
        // Double-check exclusion (shouldn't be needed but safe-guard)
        if (ExcludedApps.isExcluded(app.packageName)) {
            AlertDialog.Builder(this)
                .setTitle("Protected App")
                .setMessage("This app cannot be blocked for system security reasons")
                .setPositiveButton("OK", null)
                .show()
            return
        }



        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_app_details, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()



        // Populate dialog views
        dialogView.findViewById<ImageView>(R.id.dialog_app_icon).setImageDrawable(app.icon)
        dialogView.findViewById<TextView>(R.id.dialog_app_name).text = app.name
        dialogView.findViewById<TextView>(R.id.dialog_package_name).text = app.packageName

        // Add this ↓
//        val checkBox = dialogView.findViewById<CheckBox>(R.id.dialog_checkbox)
//        checkBox.isChecked = app.isBlocked

        val checkBox = dialogView.findViewById<CheckBox>(R.id.dialog_checkbox)
        checkBox.text = "Block ${app.name}"

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            // Update storage
            val blockedApps = storage.getBlockedApps().toMutableSet().apply {
                if (isChecked) add(app.packageName) else remove(app.packageName)
            }
            storage.saveBlockedApps(blockedApps)

            // Update the app object in the adapter
            app.isBlocked = isChecked
            val position = adapter.apps.indexOfFirst { it.packageName == app.packageName }
            if (position != -1) {
                adapter.notifyItemChanged(position)
            }
        }

        dialogView.findViewById<Button>(R.id.btn_ok).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish() // Close this activity and return to dashboard
        return true
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