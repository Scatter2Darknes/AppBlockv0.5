// File: AppLaunchDetector.kt
package com.example.appblock

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AppLaunchDetector(private val storage: StorageHelper) {
    var onAppLaunched: ((String) -> Unit)? = null
    private lateinit var usageStatsManager: UsageStatsManager
    private val handler = Handler(Looper.getMainLooper())
    private val pollInterval = 1000L
    private var isMonitoringManually = false

    fun startMonitoring(context: Context) {
        usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
                as UsageStatsManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startModernMonitoring()
        } else {
            startLegacyMonitoring()
        }
        isMonitoringManually = true

    }

    private fun startModernMonitoring() {
        // Use queryEvents directly for wider compatibility
        handler.postDelayed(modernPollRunnable, pollInterval)
    }

    private val modernPollRunnable = object : Runnable {
        override fun run() {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - pollInterval
            val query = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            while (query.hasNextEvent()) {
                query.getNextEvent(event)
                handleEvent(event)
            }
            handler.postDelayed(this, pollInterval)
        }
    }

    private fun startLegacyMonitoring() {
        handler.postDelayed(legacyPollRunnable, pollInterval)
    }

    private val legacyPollRunnable = object : Runnable {
        override fun run() {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - pollInterval
            val query = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            while (query.hasNextEvent()) {
                query.getNextEvent(event)
                handleEvent(event)
            }
            handler.postDelayed(this, pollInterval)
        }
    }

    private fun handleEvent(event: UsageEvents.Event) {
        if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
            val packageName = event.packageName

            // 1. Check if app is blocked
            if (storage.getBlockedApps().contains(packageName)) {
                // 2. Check time restrictions first
                if (storage.isTimeRestrictionEnabled(packageName) && isDuringRestrictedTime(packageName)) {
                    Log.d("APP_LOCK", "BLOCKED by time restrictions: $packageName")
                }
                // 3. Then check for delays
                else if (storage.getBlockDelay(packageName) > 0) {
                    Log.d("APP_LOCK", "Would delay ${storage.getBlockDelay(packageName)}s for $packageName")
                }
            }

            // Original detection logging
            onAppLaunched?.invoke(packageName)
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

    fun stopMonitoring() {
        handler.removeCallbacksAndMessages(null)
        isMonitoringManually = false
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
}