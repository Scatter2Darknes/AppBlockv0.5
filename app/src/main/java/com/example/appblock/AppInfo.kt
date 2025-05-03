package com.example.appblock

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    var isBlocked: Boolean = false,
    var blockDelay: Int = 10 // New property
)