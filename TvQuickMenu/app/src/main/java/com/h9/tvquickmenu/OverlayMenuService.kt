package com.h9.tvquickmenu

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayMenuService : Service() {
    private var windowManager: WindowManager? = null
    private var rootView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        if (rootView == null) {
            showMenu()
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, TvQuickMenuApp.CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.overlay_notification))
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .build()
    }

    private fun showMenu() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_menu, null)
        rootView = view
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED

        bindUi(view)
        windowManager?.addView(view, params)
        view.isFocusableInTouchMode = true
        view.requestFocus()
    }

    private fun bindUi(view: View) {
        val panelMain = view.findViewById<LinearLayout>(R.id.panelMain)
        val panelSleep = view.findViewById<LinearLayout>(R.id.panelSleep)
        val panelDim = view.findViewById<LinearLayout>(R.id.panelDim)
        val sleepChoices = view.findViewById<LinearLayout>(R.id.sleepChoices)
        val dimChoices = view.findViewById<LinearLayout>(R.id.dimChoices)
        val subtitle = view.findViewById<TextView>(R.id.menuSubtitle)

        fun showMain() {
            panelMain.visibility = View.VISIBLE
            panelSleep.visibility = View.GONE
            panelDim.visibility = View.GONE
            subtitle.text = "Выберите действие"
            view.findViewById<Button>(R.id.btnSleepTimer).requestFocus()
        }

        view.findViewById<Button>(R.id.btnClose).setOnClickListener { stopSelf() }
        view.findViewById<View>(R.id.overlayRoot).setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                if (panelMain.visibility != View.VISIBLE) {
                    showMain()
                } else {
                    stopSelf()
                }
                true
            } else {
                false
            }
        }

        view.findViewById<Button>(R.id.btnSleepTimer).setOnClickListener {
            panelMain.visibility = View.GONE
            panelSleep.visibility = View.VISIBLE
            panelDim.visibility = View.GONE
            subtitle.text = "Таймер до сна"
            sleepChoices.removeAllViews()
            AppPrefs.SLEEP_MINUTES.forEach { minutes ->
                val btn = Button(this, null, 0, R.style.TvButton)
                btn.text = formatMinutes(minutes)
                btn.setOnClickListener {
                    SleepScheduler.schedule(this, minutes)
                    stopSelf()
                }
                sleepChoices.addView(btn)
            }
            view.findViewById<Button>(R.id.btnSleepBack).requestFocus()
        }

        view.findViewById<Button>(R.id.btnSleepBack).setOnClickListener { showMain() }

        view.findViewById<Button>(R.id.btnDim).setOnClickListener {
            panelMain.visibility = View.GONE
            panelSleep.visibility = View.GONE
            panelDim.visibility = View.VISIBLE
            subtitle.text = "Ночное затемнение"
            dimChoices.removeAllViews()
            for (level in 0..5) {
                val btn = Button(this, null, 0, R.style.TvButton)
                btn.text = if (level == 0) "Выключить затемнение" else "Уровень $level из 5"
                btn.setOnClickListener {
                    DimOverlayService.setLevel(this, level)
                    stopSelf()
                }
                dimChoices.addView(btn)
            }
            view.findViewById<Button>(R.id.btnDimBack).requestFocus()
        }

        view.findViewById<Button>(R.id.btnDimBack).setOnClickListener { showMain() }

        view.findViewById<Button>(R.id.btnSleepNow).setOnClickListener {
            DevicePower.sleepNow(this)
            stopSelf()
        }

        showMain()
    }

    private fun formatMinutes(minutes: Int): String {
        return when {
            minutes < 60 -> "$minutes мин"
            minutes % 60 == 0 -> "${minutes / 60} ч"
            else -> "${minutes / 60} ч ${minutes % 60} мин"
        }
    }

    override fun onDestroy() {
        rootView?.let { v ->
            try {
                windowManager?.removeView(v)
            } catch (_: Exception) {
            }
        }
        rootView = null
        windowManager = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 42

        fun show(context: Context) {
            val intent = Intent(context, OverlayMenuService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
