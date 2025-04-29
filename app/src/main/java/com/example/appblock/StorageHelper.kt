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
}