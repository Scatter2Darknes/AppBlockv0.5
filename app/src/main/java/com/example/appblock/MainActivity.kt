package com.example.appblock

import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.apps_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load apps and populate RecyclerView
        val apps = getInstalledApps()

        recyclerView.adapter = AppAdapter(apps)
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val apps = mutableListOf<AppInfo>()

        // Query for all apps with a launcher intent
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
        Log.d("APP_LIST", "Found ${resolveInfos.size} apps")

        for (info in resolveInfos) {
            val appName = info.loadLabel(pm).toString()
            val icon: Drawable = info.loadIcon(pm)
            val packageName = info.activityInfo.packageName
            apps.add(AppInfo(appName, packageName, icon))
        }

        return apps.sortedBy { it.name }
    }
}