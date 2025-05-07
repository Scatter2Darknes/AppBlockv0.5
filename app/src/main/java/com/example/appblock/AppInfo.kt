package com.example.appblock

import TimeRange
import android.graphics.drawable.Drawable


data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    var isBlocked: Boolean = false,
    var blockDelay: Int = 10,
    var timeRanges: List<TimeRange> = emptyList()
)