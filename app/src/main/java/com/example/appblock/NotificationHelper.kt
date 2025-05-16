// NotificationHelper.kt
package com.example.appblock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    private const val CHANNEL_ID = "blocking_overlay_channel"

    fun createBlockingNotification(context: Context): Notification {
        createNotificationChannel(context)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
                .setContentTitle("App Block Active")
                .setContentText("Waiting for delay to complete")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build()
        } else {
            Notification.Builder(context)
                .setContentTitle("App Block Active")
                .setContentText("Waiting for delay to complete")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build()
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Blocking Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when app delay is active"
            }

            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}