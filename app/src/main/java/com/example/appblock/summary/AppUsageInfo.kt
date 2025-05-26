package com.example.appblock.summary

data class AppUsageInfo(
    val appName: String,
    val packageName: String,
    val usageTime: String,
    val usageTimeMs: Long,
    val appIcon: android.graphics.drawable.Drawable
)