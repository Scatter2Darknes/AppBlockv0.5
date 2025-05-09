package com.example.appblock

import TimeRange
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StorageHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("BLOCKED_APPS", Context.MODE_PRIVATE)

    private val gson = Gson()

    fun saveTimeRanges(packageName: String, ranges: List<TimeRange>) {
        prefs.edit {
            putString("TIME_$packageName", gson.toJson(ranges))
        }
    }

    fun getTimeRanges(packageName: String): List<TimeRange> {
        val json = prefs.getString("TIME_$packageName", null) ?: return emptyList()
        val type = object : TypeToken<List<TimeRange>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // For saving
    fun saveBlockedApps(packageNames: Set<String>) {
        prefs.edit { putStringSet("BLOCKED", packageNames) }
    }

    // For retrieving
    fun getBlockedApps(): Set<String> {
        return prefs.getStringSet("BLOCKED", emptySet()) ?: emptySet()
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

    // Add to StorageHelper class
    fun saveTimeRestrictionEnabled(packageName: String, enabled: Boolean) {
        prefs.edit {
            putBoolean("TIME_ENABLED_$packageName", enabled)
        }
    }

    fun isTimeRestrictionEnabled(packageName: String): Boolean {
        return prefs.getBoolean("TIME_ENABLED_$packageName", false)
    }

}