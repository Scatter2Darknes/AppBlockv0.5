package com.example.appblock

import android.app.Application

class AppBlockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}