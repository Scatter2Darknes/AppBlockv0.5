// NotificationHelper.kt
package com.example.appblock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import android.graphics.Color

object NotificationHelper {
    const val CHANNEL_ID = "blocking_overlay_channel"

    fun createBlockingNotification(context: Context): Notification {
        createNotificationChannel(context)

        return NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("App Block Active")
            .setContentText("Waiting for delay to complete")
            .setSmallIcon(R.drawable.ic_block)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Block Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows app blocking status and alerts"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}