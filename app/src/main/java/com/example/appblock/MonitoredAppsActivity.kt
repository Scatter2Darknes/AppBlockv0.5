package com.example.appblock

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MonitoredAppsActivity : AppCompatActivity() {
    private lateinit var storage: StorageHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitored_apps)

        try {
            storage = StorageHelper(applicationContext)
            val recyclerView = findViewById<RecyclerView>(R.id.monitored_apps_list)
            recyclerView.layoutManager = LinearLayoutManager(this)

            // Initialize with empty list first
            recyclerView.adapter = AppAdapter(mutableListOf(), storage, showRemove = true)

            // Toolbar setup
            setSupportActionBar(findViewById(R.id.toolbar))
            supportActionBar?.title = "Blocked Apps"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)


            loadBlockedApps(recyclerView)
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_dark) // Create this drawable

        } catch (e: Exception) {
            Log.e("CRASH", "MonitoredApps failed: ${e.stackTraceToString()}")
            finish()
        }
    }

    private fun loadBlockedApps(recyclerView: RecyclerView) {
        if (isFinishing) return // Don't update if activity is closing

        val adapter = AppAdapter(mutableListOf(), storage, showRemove = true).apply {
            onRemoveClick = { app ->
                // Launch MainActivity to configure the app
                startActivity(Intent(this@MonitoredAppsActivity, MainActivity::class.java).apply {
                    putExtra("EDIT_APP", app.packageName)
                })
            }
        }

        recyclerView.adapter = adapter

        val pm = packageManager
        val blockedApps = storage.getBlockedApps()
        val apps = mutableListOf<AppInfo>()

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        pm.queryIntentActivities(intent, 0).forEach { info ->
            val packageName = info.activityInfo.packageName
            if (blockedApps.contains(packageName)) {
                apps.add(AppInfo(
                    name = info.loadLabel(pm).toString(),
                    packageName = packageName,
                    icon = info.loadIcon(pm),
                    isBlocked = true,
                    blockDelay = storage.getBlockDelay(packageName),
                    timeRanges = storage.getTimeRanges(packageName)
                ))
            }
        }

        (recyclerView.adapter as? AppAdapter)?.apply {
            updateList(apps.sortedBy { it.name }.toMutableList())
        }
    }

    private fun launchEditScreenForApp(app: AppInfo) {
        // REVERSE ONLY THIS TRANSITION
        val options = ActivityOptionsCompat.makeCustomAnimation(
            this,
            R.anim.slide_in_left,  // Enter from left
            R.anim.slide_out_right  // Exit to right
        )

        Intent(this, MainActivity::class.java).apply {
            putExtra("EDIT_APP", app.packageName)
            startActivity(this, options.toBundle())
        }

        // Keep original finish behavior
        finish()
    }

    override fun onResume() {
        super.onResume()
        findViewById<RecyclerView>(R.id.monitored_apps_list)?.let {
            loadBlockedApps(it) // Refresh when returning from config
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}