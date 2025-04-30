package com.example.appblock

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MonitoredAppsActivity : AppCompatActivity() {
    private lateinit var storage: StorageHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitored_apps)
        storage = StorageHelper(applicationContext)

        // Toolbar setup
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Blocked Apps"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val recyclerView = findViewById<RecyclerView>(R.id.monitored_apps_list)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadBlockedApps(recyclerView)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_dark) // Create this drawable
    }

    private fun loadBlockedApps(recyclerView: RecyclerView) {
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
                    isBlocked = true
                ))
            }
        }

        recyclerView.adapter = AppAdapter(apps.sortedBy { it.name }.toMutableList())
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}