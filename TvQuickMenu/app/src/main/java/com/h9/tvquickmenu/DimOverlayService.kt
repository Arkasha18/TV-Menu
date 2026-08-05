package com.h9.tvquickmenu

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class DimOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var dimView: View? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val level = intent?.getIntExtra(EXTRA_LEVEL, AppPrefs(this).dimLevel) ?: 0
        AppPrefs(this).dimLevel = level

        if (level <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIF_ID, buildNotification(level))
        ensureView()
        applyLevel(level)
        return START_STICKY
    }

    private fun buildNotification(level: Int): Notification {
        return NotificationCompat.Builder(this, TvQuickMenuApp.CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.dim_notification) + ": $level/5")
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .build()
    }

    private fun ensureView() {
        if (dimView != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        dimView = View(this).apply {
            setBackgroundColor(Color.BLACK)
            // Не перехватываем нажатия — контент под затемнением остаётся управляемым.
            isClickable = false
            isFocusable = false
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager?.addView(dimView, params)
    }

    private fun applyLevel(level: Int) {
        // Уровни 1..5: от лёгкого затемнения к сильному.
        val alpha = when (level.coerceIn(0, 5)) {
            0 -> 0
            1 -> 40
            2 -> 80
            3 -> 120
            4 -> 160
            else -> 200
        }
        dimView?.setBackgroundColor(Color.argb(alpha, 0, 0, 0))
    }

    override fun onDestroy() {
        dimView?.let { v ->
            try {
                windowManager?.removeView(v)
            } catch (_: Exception) {
            }
        }
        dimView = null
        windowManager = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 43
        private const val EXTRA_LEVEL = "level"

        fun setLevel(context: Context, level: Int) {
            val app = context.applicationContext
            AppPrefs(app).dimLevel = level
            val intent = Intent(app, DimOverlayService::class.java).putExtra(EXTRA_LEVEL, level)
            if (level <= 0) {
                app.stopService(Intent(app, DimOverlayService::class.java))
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                app.startForegroundService(intent)
            } else {
                app.startService(intent)
            }
        }
    }
}
