package com.example.appblock

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appblock.summary.SummaryAdapter

class SummaryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val recyclerView = findViewById<RecyclerView>(R.id.summary_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        recyclerView.adapter = SummaryAdapter(getInstalledApps())
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun getInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val apps = mutableListOf<AppInfo>()

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = try {
            // Use the same flags as MainActivity (0 for broadest compatibility)
            pm.queryIntentActivities(intent, 0)
        } catch (e: Exception) {
            emptyList()
        }

        for (info in resolveInfos) {
            val packageName = info.activityInfo.packageName
            if (ExcludedApps.isExcluded(packageName)) continue

            apps.add(AppInfo(
                name = info.loadLabel(pm).toString(),
                packageName = packageName,
                icon = info.loadIcon(pm),
                isBlocked = false,  // Not used in summary
                blockDelay = 0      // Not used in summary
            ))
        }

        return apps.sortedBy { it.name }
    }
}