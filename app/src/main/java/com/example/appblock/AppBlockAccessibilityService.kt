package com.example.appblock

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AppBlockAccessibilityService : AccessibilityService() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Only interested in window (app) switches
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Ignore own package to avoid loop
            if (packageName == this.packageName) return

            Log.d("AppBlockAccessibility", "App switched: $packageName")

            // Here: Insert your blocking/shaming logic!
            // For demo: Show a persistent notification if app is in blocked list
            val sharedPrefs = getSharedPreferences("appblock_prefs", MODE_PRIVATE)
            val blockedApps = sharedPrefs.getStringSet("blocked_apps", setOf()) ?: setOf()
            val delay = sharedPrefs.getLong("block_delay_$packageName", 0L)

            if (blockedApps.contains(packageName)) {
                showBlockNotification(packageName, delay)
            }
        }
    }

    override fun onInterrupt() {
        // Required method. No-op.
    }

    // Example function to show a notification (reuse your real logic!)
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showBlockNotification(packageName: String, delay: Long = 0) {
        val channelId = "BLOCKING_CHANNEL"
        val notificationId = 1234

        val contentText = if (delay > 0) {
            "You set a delay of $delay seconds for this app."
        } else {
            "Remember your goal to avoid using $packageName right now!"
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_block) // Provide your icon!
            .setContentTitle("App Block Active: $packageName")
            .setContentText(contentText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)

        NotificationManagerCompat.from(this).notify(notificationId, builder.build())
    }
}
