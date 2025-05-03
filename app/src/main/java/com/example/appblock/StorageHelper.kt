package com.example.appblock

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class StorageHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("BLOCKED_APPS", Context.MODE_PRIVATE)

    fun saveBlockedApps(packageNames: Set<String>) {
        prefs.edit {
            putStringSet("BLOCKED", packageNames)
        }
    }


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
}