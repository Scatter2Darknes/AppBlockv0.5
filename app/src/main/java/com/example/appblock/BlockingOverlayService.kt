// BlockingOverlayService.kt
package com.example.appblock

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

class BlockingOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var countdownTimer: CountDownTimer
    private var remainingSeconds = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val notification = createNotificationWithIntent()

            // 1. Create notification first
            startForeground(1, notification)

            // 2. Create overlay
            createOverlay()

            // 3. Get parameters from intent
            intent?.extras?.let {
                val packageName = it.getString("packageName") ?: return@let
                remainingSeconds = it.getLong("delaySeconds", 0L)
                startCountdown(packageName)
            }

        } catch (e: Exception) {
            Log.e("OVERLAY", "Service failed to start", e)
            stopSelf()
        }
        return START_STICKY
    }

    private fun createNotificationWithIntent(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setContentTitle("App Block Active")
            .setContentText("Tap to manage settings")
            .setSmallIcon(R.drawable.ic_block)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun createOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_blocking, null).apply {
            setOnTouchListener { _, _ -> true } // Block all touches
        }

        windowManager.addView(overlayView, layoutParams)
    }

    private fun startCountdown(packageName: String) {
        Log.d("BLOCK_OVERLAY", "Starting countdown for $packageName: $remainingSeconds seconds")

        countdownTimer = object : CountDownTimer(remainingSeconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d("BLOCK_OVERLAY", "Remaining: ${millisUntilFinished/1000}s")
                overlayView.findViewById<TextView>(R.id.txt_countdown).text =
                    "Wait ${millisUntilFinished / 1000} seconds"
            }

            override fun onFinish() {
                Log.d("BLOCK_OVERLAY", "Delay completed for $packageName")
                stopSelf()
            }
        }.start()
    }

    override fun onDestroy() {
        if (::countdownTimer.isInitialized) countdownTimer.cancel()
        if (::overlayView.isInitialized) windowManager.removeView(overlayView)
        super.onDestroy()
    }
}