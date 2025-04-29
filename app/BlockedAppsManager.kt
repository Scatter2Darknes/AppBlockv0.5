package com.example.appblock

import android.content.Context
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BlockedAppsManager(context: Context) {
    private val prefs = context.getSharedPreferences("AppBlockPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "BLOCKED_APPS"

    fun saveBlockedApps(packageNames: Set<String>) {
        prefs.edit().putString(key, gson.toJson(packageNames)).apply()
    }

    fun getBlockedApps(): Set<String> {
        val json = prefs.getString(key, null) ?: return emptySet()
        val type = object : TypeToken<Set<String>>() {}.type
        return gson.fromJson(json, type) ?: emptySet()
    }
}