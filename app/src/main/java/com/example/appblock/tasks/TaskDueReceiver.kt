
package com.example.appblock.tasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.appblock.R

class TaskDueReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskName = intent.getStringExtra("task_name") ?: "Task"

        val channelId = "task_notifications"
        val channelName = "Task Notifications"

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Task Overdue!")
            .setContentText("$taskName is overdue!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        //NotificationManagerCompat.from(context).notify(taskName.hashCode(), notification)
    }
}
