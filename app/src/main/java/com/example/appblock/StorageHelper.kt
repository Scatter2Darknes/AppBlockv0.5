package com.example.appblock

import TimeRange
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// For enabled state
private const val TIME_ENABLED_PREFIX = "TIME_ENABLED_"

// For time ranges
private const val TIME_RANGES_PREFIX = "TIME_RANGES_"

class StorageHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("BLOCKED_APPS", Context.MODE_PRIVATE)

    private val gson = Gson()

    // for saving
    fun saveBlockedApps(packageNames: Set<String>) {
        prefs.edit {
            putStringSet("BLOCKED", packageNames)
        }
    }

    // for retrieving
    fun getBlockedApps(): Set<String> {
        return HashSet(prefs.getStringSet("BLOCKED", emptySet()) ?: emptySet())
    }

    // Add these methods
    fun saveBlockDelay(packageName: String, delay: Int) {
        prefs.edit {
            putInt("DELAY_$packageName", delay)
        }
    }

    fun getBlockDelay(packageName: String): Int {
        return prefs.getInt("DELAY_$packageName", 10) // Default 10 seconds
    }

    fun saveTimeRestrictionEnabled(packageName: String, enabled: Boolean) {
        prefs.edit { putBoolean("$TIME_ENABLED_PREFIX$packageName", enabled) }
    }

    fun isTimeRestrictionEnabled(packageName: String): Boolean {
        return prefs.getBoolean("$TIME_ENABLED_PREFIX$packageName", false)
    }

    fun saveTimeRanges(packageName: String, ranges: List<TimeRange>) {
        Log.d("STORAGE", "Saving time ranges for $packageName: ${gson.toJson(ranges)}")
        prefs.edit { putString("$TIME_RANGES_PREFIX$packageName", gson.toJson(ranges)) }
    }

    fun getTimeRanges(packageName: String): List<TimeRange> {
        val json = prefs.getString("$TIME_RANGES_PREFIX$packageName", null)
        Log.d("STORAGE", "Loaded time ranges for $packageName: $json")
        return if (json != null) gson.fromJson(json, object : TypeToken<List<TimeRange>>() {}.type) else emptyList()
    }
}