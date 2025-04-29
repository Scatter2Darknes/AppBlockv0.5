package com.example.appblock

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

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Toolbar setup
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Select Apps"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val recyclerView = findViewById<RecyclerView>(R.id.apps_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = AppAdapter(getInstalledApps())
        recyclerView.adapter = adapter

        // Handle item clicks
        adapter.onItemClick = { app ->
            showAppDetailsDialog(app)
        }
    }

    private fun showAppDetailsDialog(app: AppInfo) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_app_details, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Populate dialog views
        dialogView.findViewById<ImageView>(R.id.dialog_app_icon).setImageDrawable(app.icon)
        dialogView.findViewById<TextView>(R.id.dialog_app_name).text = app.name
        dialogView.findViewById<TextView>(R.id.dialog_package_name).text = app.packageName

        val checkBox = dialogView.findViewById<CheckBox>(R.id.dialog_checkbox)
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Save blocked state (we'll implement later)
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