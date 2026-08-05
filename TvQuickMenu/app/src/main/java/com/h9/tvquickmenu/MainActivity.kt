package com.h9.tvquickmenu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: AppPrefs
    private lateinit var statusOverlay: TextView
    private lateinit var statusAccessibility: TextView
    private lateinit var statusHotkey: TextView
    private lateinit var statusTimer: TextView
    private lateinit var statusDim: TextView
    private lateinit var statusAdb: TextView
    private lateinit var programHint: TextView

    private val programmedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshStatus()
            programHint.visibility = View.GONE
            Toast.makeText(
                this@MainActivity,
                "Кнопка сохранена: ${prefs.hotkeyLabel()}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = AppPrefs(this)

        statusOverlay = findViewById(R.id.statusOverlay)
        statusAccessibility = findViewById(R.id.statusAccessibility)
        statusHotkey = findViewById(R.id.statusHotkey)
        statusTimer = findViewById(R.id.statusTimer)
        statusDim = findViewById(R.id.statusDim)
        statusAdb = findViewById(R.id.statusAdb)
        programHint = findViewById(R.id.programHint)

        findViewById<Button>(R.id.btnOverlayPermission).setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.btnRestrictedSettings).setOnClickListener {
            // На Google TV приложения не из магазина часто блокируют службу,
            // пока не разрешены «ограниченные настройки» в карточке приложения.
            Toast.makeText(
                this,
                "В карточке приложения откройте меню и включите «Разрешить ограниченные настройки», затем снова службу кнопок.",
                Toast.LENGTH_LONG
            ).show()
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        }

        findViewById<Button>(R.id.btnProgramKey).setOnClickListener {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(
                    this,
                    "Сначала включите службу «TV Меню»",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@setOnClickListener
            }
            prefs.programmingMode = true
            programHint.visibility = View.VISIBLE
            programHint.text =
                "Нажмите Иви / Wink / другую специальную кнопку."
            Toast.makeText(this, "Ожидаю нажатие кнопки пульта", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnCheckAdb).setOnClickListener {
            statusAdb.text = "Локальный ADB: проверка…"
            thread {
                val result = LocalAdb.probe(this)
                runOnUiThread {
                    refreshStatus()
                    Toast.makeText(
                        this,
                        if (result.ok) "ADB готов: ${result.endpoint}"
                        else "ADB не настроен — основной режим всё равно работает",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        findViewById<Button>(R.id.btnTestSleep).setOnClickListener {
            Toast.makeText(this, "Пробую усыпить устройство…", Toast.LENGTH_SHORT).show()
            DevicePower.sleepNow(this)
        }

        findViewById<Button>(R.id.btnClearKey).setOnClickListener {
            prefs.programmingMode = false
            prefs.clearHotkey()
            programHint.visibility = View.GONE
            refreshStatus()
        }

        findViewById<Button>(R.id.btnOpenMenu).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                // Для Activity-меню разрешение «поверх окон» не обязательно,
                // но затемнение по-прежнему его использует.
            }
            startActivity(
                Intent(this, MenuActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
            )
        }

        findViewById<Button>(R.id.btnCancelSleep).setOnClickListener {
            SleepScheduler.cancel(this)
            refreshStatus()
            Toast.makeText(this, "Таймер сна отменён", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(HotkeyAccessibilityService.ACTION_HOTKEY_PROGRAMMED)
        }
        ContextCompat.registerReceiver(
            this,
            programmedReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        try {
            unregisterReceiver(programmedReceiver)
        } catch (_: Exception) {
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (prefs.programmingMode &&
            event.action == KeyEvent.ACTION_DOWN &&
            event.repeatCount == 0
        ) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                prefs.programmingMode = false
                programHint.visibility = View.GONE
                Toast.makeText(this, "Программирование отменено", Toast.LENGTH_SHORT).show()
                return true
            }
            if (event.keyCode != KeyEvent.KEYCODE_HOME &&
                event.keyCode != KeyEvent.KEYCODE_APP_SWITCH
            ) {
                // Служба кнопок сохранит нажатие раньше экрана настройки.
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun refreshStatus() {
        val overlayOk = Settings.canDrawOverlays(this)
        statusOverlay.text =
            if (overlayOk) "Поверх окон: разрешено" else "Поверх окон: нужно разрешить"
        statusOverlay.setTextColor(
            ContextCompat.getColor(this, if (overlayOk) R.color.ok else R.color.danger)
        )

        val accessibilityOk = isAccessibilityEnabled()
        statusAccessibility.text =
            if (accessibilityOk) {
                "Служба кнопок: включена"
            } else {
                "Служба кнопок: выключена. Если переключатель отскакивает — разрешите ограниченные настройки."
            }
        statusAccessibility.setTextColor(
            ContextCompat.getColor(
                this,
                if (accessibilityOk) R.color.ok else R.color.danger
            )
        )

        statusHotkey.text = "Кнопка пульта: ${prefs.hotkeyLabel()}"
        statusTimer.text = SleepScheduler.remainingLabel(this)
        statusDim.text =
            if (prefs.dimLevel <= 0) "затемнение выключено"
            else "затемнение: уровень ${prefs.dimLevel} из 5"

        statusAdb.text =
            if (prefs.adbReady) "Дополнительный ADB: готов (${prefs.adbHost}:${prefs.adbPort})"
            else "Дополнительный ADB: не настроен (не обязательно)"
        statusAdb.setTextColor(
            ContextCompat.getColor(this, if (prefs.adbReady) R.color.ok else R.color.muted)
        )
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = "$packageName/${HotkeyAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
