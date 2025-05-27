// File: AppLaunchDetector.kt
package com.example.appblock

import android.app.ActivityManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Calendar
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.widget.Toast
import androidx.annotation.RequiresPermission

class AppLaunchDetector(private val context: Context, private val storage: StorageHelper) {
    var onAppLaunched: ((String) -> Unit)? = null
    private lateinit var usageStatsManager: UsageStatsManager
    private val handler = Handler(Looper.getMainLooper())
    private val pollInterval = 500L // 0.5 seconds
    private var isMonitoringManually = false
    private var isMonitoring = false
    private val lastLaunchTimestamps = mutableMapOf<String, Long>()
    private val lastNotificationTimes = mutableMapOf<String, Long>()
    private val notificationCooldown = 1000L // 3 seconds, adjust as desired

    fun startMonitoring(context: Context) {
        usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        isMonitoring = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            handler.postDelayed(modernPollRunnable, pollInterval)
        } else {
            handler.postDelayed(legacyPollRunnable, pollInterval)
        }
    }


    fun stopMonitoring() {
        isMonitoring = false
        handler.removeCallbacks(modernPollRunnable)
        handler.removeCallbacks(legacyPollRunnable)
    }


    private fun startModernMonitoring() {
        // Use queryEvents directly for wider compatibility
        handler.postDelayed(modernPollRunnable, pollInterval)
    }

    private val modernPollRunnable = object : Runnable {
        @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        override fun run() {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - pollInterval
            val query = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            while (query.hasNextEvent()) {
                query.getNextEvent(event)
                handleEvent(event)
            }
            // ADD THIS LINE
            pollForegroundApp()  // <-- Polls current foreground app for blocked status

            handler.postDelayed(this, pollInterval)
        }

    }

    private fun startLegacyMonitoring() {
        handler.postDelayed(legacyPollRunnable, pollInterval)
    }

    private val legacyPollRunnable = object : Runnable {
        @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        override fun run() {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - pollInterval
            val query = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            while (query.hasNextEvent()) {
                query.getNextEvent(event)
                handleEvent(event)
            }
            pollForegroundApp() // <-- add this line here!
            handler.postDelayed(this, pollInterval)
        }
    }


    // Add this function to check foreground status
    private fun isAppInForeground(packageName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val foregroundTask = activityManager.runningAppProcesses?.firstOrNull {
            it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
        return foregroundTask?.pkgList?.contains(packageName) ?: false
    }

    // Update handleEvent() with blocking logic
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun handleEvent(event: UsageEvents.Event) {
        if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
            val packageName = event.packageName
            if (packageName == context.packageName) return

            // 1. Hard-lock check
            if (storage.isTimeRestrictionEnabled(packageName)) {
                val nowCal = Calendar.getInstance()
                val inWindow = storage.getTimeRanges(packageName).any { it.isWithinRange(nowCal) }
                if (inWindow && canShowOverlay()) {
                    Log.d("BLOCK", "Hard-lock: $packageName during restricted window")
                    showBlockNotification(packageName, isTimeRestricted = true)
                    return
                }
            }

            // 2. Soft-lock check
            if (storage.getBlockedApps().contains(packageName) &&
                !ExcludedApps.isExcluded(packageName)
            ) {
                val delay = storage.getBlockDelay(packageName)
                if (delay > 0 && canShowOverlay()) {
                    Log.d("BLOCK", "Launch detected: $packageName, delaying $delay seconds")
                    showBlockNotification(packageName, isTimeRestricted = false, delay = delay.toLong())
                }
            }
        }
    }




    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showBlockNotification(packageName: String, isTimeRestricted: Boolean, delay: Long = 0) {
        val notificationId = 1234 // You can make this unique per app if needed

        val contentText = if (isTimeRestricted) {
            "This app is blocked due to your time restriction."
        } else if (delay > 0) {
            "You set a delay of $delay seconds for this app."
        } else {
            "Remember your goal to avoid using $packageName right now!"
        }

        // Build the persistent notification
        val builder = NotificationCompat.Builder(context, "BLOCKING_CHANNEL")
            .setSmallIcon(R.drawable.ic_block)
            .setContentTitle("App Block Active: $packageName")
            .setContentText(contentText)
            .setOngoing(true) // <-- Makes notification persistent
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false) // Don't auto-dismiss

        // Optional: Add action to "Open anyway" if you want

        // Show the notification
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }





    private fun canShowOverlay(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasPermission = Settings.canDrawOverlays(context)
            Log.d("OVERLAY_PERM", "Overlay permission granted: $hasPermission")
            hasPermission
        } else {
            Log.d("OVERLAY_PERM", "Pre-Marshmallow, no permission needed")
            true
        }
    }

    private fun isDuringRestrictedTime(packageName: String): Boolean {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)

        return storage.getTimeRanges(packageName).any { range ->
            val startTotal = range.startHour * 60 + range.startMinute
            val endTotal = range.endHour * 60 + range.endMinute
            val currentTotal = currentHour * 60 + currentMinute

            currentTotal in startTotal..endTotal
        }
    }


    fun isMonitoring(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            handler.hasCallbacks(legacyPollRunnable) ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && handler.hasCallbacks(modernPollRunnable))
        } else {
            // For API < 29, maintain manual tracking
            isMonitoringManually
        }
    }

    private fun ensureBlockingNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "BLOCKING_CHANNEL",
                "Blocking Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }


    private fun pollForegroundApp() {
        Log.d("DEBUG", "pollForegroundApp: Checking foreground app")
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcess = activityManager.runningAppProcesses?.firstOrNull {
            it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
        val foregroundPkg = runningAppProcess?.processName
        Log.d("DEBUG", "Foreground app: $foregroundPkg")
        if (foregroundPkg == context.packageName) return
        if (foregroundPkg != null
            && storage.getBlockedApps().contains(foregroundPkg)
            && !ExcludedApps.isExcluded(foregroundPkg)
        ) {
            val now = System.currentTimeMillis()
            val lastTime = lastNotificationTimes[foregroundPkg] ?: 0L
            if (now - lastTime > notificationCooldown) {
                showBlockNotification(
                    foregroundPkg,
                    isTimeRestricted = false,
                    delay = storage.getBlockDelay(foregroundPkg).toLong()
                )
                lastNotificationTimes[foregroundPkg] = now
            }
        }
    }


}