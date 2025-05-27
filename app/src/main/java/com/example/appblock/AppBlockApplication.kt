// File: AppBlockApplication.kt
package com.example.appblock

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class AppBlockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createBlockingChannel()
    }

    private fun createBlockingChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationHelper.CHANNEL_ID,
                "AppBlock Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when a blocked app is launched"
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
