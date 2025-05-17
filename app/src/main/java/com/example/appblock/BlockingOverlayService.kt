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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return try {
            // 1. Create notification FIRST
            createNotificationChannel()
            startForeground(1, createNotification())

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
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

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

            // For visual debugging - bright red background
            overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_blocking, null).apply {
                setBackgroundColor(Color.argb(200, 255, 0, 0)) // Semi-transparent red
                findViewById<TextView>(R.id.txt_countdown).apply {
                    text = "BLOCKING OVERLAY ACTIVE"
                    setTextColor(Color.WHITE)
                    textSize = 24f
                }
                setOnSystemUiVisibilityChangeListener { visibility ->
                    // Keep overlay on top
                    systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                }
            }

            windowManager.addView(overlayView, params)
            Log.d("OVERLAY", "Overlay view added successfully")

            // Start countdown if needed
            intent?.getLongExtra("delaySeconds", 0L)?.let { delay ->
                startCountdown(delay * 1000)
            }

        } catch (e: Exception) {
            Log.e("OVERLAY", "Failed to create overlay", e)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "BLOCKING_CHANNEL",
                "Blocking Notifications",
                NotificationManager.IMPORTANCE_LOW
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
        } catch (e: Exception) {
            Log.e("CLEANUP_ERROR", "Error cleaning up", e)
        }
        super.onDestroy()
    }
}