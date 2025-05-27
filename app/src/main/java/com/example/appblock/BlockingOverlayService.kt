package com.example.appblock

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import com.example.appblock.R
import android.graphics.Color
import android.provider.Settings

class BlockingOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var countdownTimer: CountDownTimer? = null
    private val FOREGROUND_SERVICE_TYPE_SYSTEM_ALERT_WINDOW = 0x00000020

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ForegroundServiceType","WrongConstant")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return try {
            android.widget.Toast.makeText(this, "BlockingOverlayService started!", android.widget.Toast.LENGTH_SHORT).show()
            Log.d("OVERLAY", "BlockingOverlayService started with intent: $intent")
            // 1. Create notification FIRST
            createNotificationChannel()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 0x00000020 is the value for FOREGROUND_SERVICE_TYPE_SYSTEM_ALERT_WINDOW
                startForeground(
                    1,
                    createNotification(),
                    FOREGROUND_SERVICE_TYPE_SYSTEM_ALERT_WINDOW
                )
            } else {
                startForeground(1, createNotification())
            }



            // 2. Ensure overlay permission
            if (!canDrawOverlays()) {
                stopSelf()
                return START_NOT_STICKY
            }

            // 3. Setup overlay with visual feedback
            setupOverlay(intent)

            START_STICKY
        } catch (e: Exception) {
            Log.e("OVERLAY", "Service failed to start", e)
            stopSelf()
            START_NOT_STICKY
        }
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun setupOverlay(intent: Intent?) {
        try {
            Log.d("OVERLAY", "setupOverlay: Entered")
            android.widget.Toast.makeText(this, "setupOverlay Entered", android.widget.Toast.LENGTH_SHORT).show()

            Log.d("OVERLAY", "setupOverlay: Before windowManager init")
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            Log.d("OVERLAY", "setupOverlay: After windowManager init")

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

            overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_blocking, null)
            Log.d("OVERLAY", "setupOverlay: About to add overlayView")
            windowManager.addView(overlayView, params)
            Log.d("OVERLAY", "setupOverlay: overlayView added")
            android.widget.Toast.makeText(this, "Overlay added!", android.widget.Toast.LENGTH_SHORT).show()

            val textView = overlayView?.findViewById<TextView>(R.id.txt_countdown)
            val isTimeRestricted = intent?.getBooleanExtra("timeRestricted", false) ?: false

            if (isTimeRestricted) {
                // HARD-LOCK: Show hard lock message
                textView?.text = "This app is blocked right now.\nPlease try again later."
            } else {
                // SOFT-LOCK: Show countdown
                val delay = intent?.getLongExtra("delaySeconds", 0L) ?: 0L
                if (delay > 0) {
                    startCountdown(delay * 1000)
                } else {
                    textView?.text = "This app is currently delayed."
                }
            }

            // OPTIONAL: Dismiss on tap for debugging or until you implement strict locking
            overlayView?.setOnClickListener {
                stopSelf()
            }

        } catch (e: Exception) {
            Log.e("OVERLAY", "Failed to create overlay", e)
            android.widget.Toast.makeText(this, "Overlay ERROR: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "BLOCKING_CHANNEL",
                "Blocking Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "BLOCKING_CHANNEL")
            .setContentTitle("App Block Active")
            .setContentText("Blocking apps...")
            .setSmallIcon(R.drawable.ic_block)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }



    private fun startCountdown(millis: Long) {
        countdownTimer = object : CountDownTimer(millis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                overlayView?.findViewById<TextView>(R.id.txt_countdown)?.text =
                    "Wait ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                stopSelf()
            }
        }.start()
    }

    override fun onDestroy() {
        try {
            countdownTimer?.cancel()
            overlayView?.let { windowManager.removeView(it) }
            stopForeground(true)
        } catch (e: Exception) {
            Log.e("CLEANUP_ERROR", "Error cleaning up", e)
        }
        super.onDestroy()
    }

}