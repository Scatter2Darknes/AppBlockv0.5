// BlockingOverlayService.kt
package com.example.appblock

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

class BlockingOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var countdownTimer: CountDownTimer
    private var remainingSeconds = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()
        startForeground(1, NotificationHelper.createBlockingNotification(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.extras?.let {
            val packageName = it.getString("packageName") ?: return START_STICKY
            remainingSeconds = it.getLong("delaySeconds", 0L)
            startCountdown(packageName)
        }
        return START_STICKY
    }

    private fun createOverlay() {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_blocking, null)
        overlayView.setOnTouchListener { _, _ -> true } // Block all touches
        windowManager.addView(overlayView, layoutParams)
    }

    private fun startCountdown(packageName: String) {
        countdownTimer = object : CountDownTimer(remainingSeconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
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
        countdownTimer.cancel()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
        super.onDestroy()
    }
}