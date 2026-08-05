package com.h9.tvquickmenu

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Обрабатывает только назначенную специальную кнопку.
 * «ОК», стрелки и остальные кнопки всегда проходят в систему без изменений.
 *
 * Тип обратной связи службы равен нулю (как у tvQuickActions). Поэтому
 * Android TV Home не заменяет короткое «ОК» контекстным меню.
 */
class HotkeyAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val openingMenu = AtomicBoolean(false)

    private var lastHotkeyElapsed = 0L
    private var suppressForeignUntil = 0L
    private var lastForeignDismissElapsed = 0L
    private var returnTarget = RETURN_NONE

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        migrateStoredHotkey()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val now = SystemClock.elapsedRealtime()
        if (now > suppressForeignUntil) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || isIgnoredSystemPackage(pkg)) return
        if (now - lastForeignDismissElapsed < 250) return

        lastForeignDismissElapsed = now
        performGlobalAction(GLOBAL_ACTION_BACK)
        mainHandler.postDelayed({
            when (returnTarget) {
                RETURN_MENU -> bringMenuToFront()
                RETURN_SETTINGS -> bringSettingsToFront()
            }
        }, 100)
    }

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val prefs = AppPrefs(this)
        val code = normalizeKey(event.keyCode)

        if (prefs.programmingMode) {
            return handleProgramming(event, code, prefs)
        }

        val hotkey = selectedHotkeyCode(prefs)
        if (hotkey != KeyEvent.KEYCODE_UNKNOWN && code == normalizeKey(hotkey)) {
            return handleHotkey(event)
        }

        // «ОК», стрелки, Back, Home и остальные кнопки не изменяем.
        return false
    }

    private fun handleProgramming(event: KeyEvent, code: Int, prefs: AppPrefs): Boolean {
        if (code == KeyEvent.KEYCODE_BACK ||
            code == KeyEvent.KEYCODE_HOME ||
            code == KeyEvent.KEYCODE_APP_SWITCH
        ) {
            return false
        }

        // «ОК» нужен для управления экраном настройки, его не назначаем.
        if (isCenterKey(code)) return false

        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            prefs.hotkeyCode = code
            prefs.geteventSignature = ""
            prefs.programmingMode = false
            armForeignSuppression(RETURN_SETTINGS)
            sendBroadcast(Intent(ACTION_HOTKEY_PROGRAMMED).setPackage(packageName))
        }
        return true
    }

    private fun handleHotkey(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return true

        val now = SystemClock.elapsedRealtime()
        if (now - lastHotkeyElapsed < 800) return true
        lastHotkeyElapsed = now

        armForeignSuppression(RETURN_MENU)
        if (!MenuActivity.isOpen) {
            openMenuNow()
        }
        return true
    }

    private fun armForeignSuppression(target: Int) {
        returnTarget = target
        suppressForeignUntil = SystemClock.elapsedRealtime() + SUPPRESS_MS
    }

    private fun openMenuNow() {
        if (!openingMenu.compareAndSet(false, true)) return
        bringMenuToFront()
        mainHandler.postDelayed({ openingMenu.set(false) }, 500)
    }

    private fun bringMenuToFront() {
        startActivity(
            Intent(this, MenuActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            }
        )
    }

    private fun bringSettingsToFront() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            }
        )
    }

    fun lockScreen(): Boolean = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)

    private fun migrateStoredHotkey() {
        val prefs = AppPrefs(this)
        if (prefs.hotkeyCode != KeyEvent.KEYCODE_UNKNOWN) return
        val migrated = XiaomiAppButtons.androidKeyForSignature(prefs.geteventSignature) ?: return
        prefs.hotkeyCode = migrated
        prefs.geteventSignature = ""
    }

    private fun selectedHotkeyCode(prefs: AppPrefs): Int {
        if (prefs.hotkeyCode != KeyEvent.KEYCODE_UNKNOWN) return prefs.hotkeyCode
        return XiaomiAppButtons.androidKeyForSignature(prefs.geteventSignature)
            ?: KeyEvent.KEYCODE_UNKNOWN
    }

    private fun normalizeKey(code: Int): Int =
        when (code) {
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> KeyEvent.KEYCODE_DPAD_CENTER
            else -> code
        }

    private fun isCenterKey(code: Int): Boolean =
        code == KeyEvent.KEYCODE_DPAD_CENTER

    private fun isIgnoredSystemPackage(pkg: String): Boolean =
        pkg == "android" ||
            pkg.startsWith("com.android.systemui") ||
            pkg.startsWith("com.google.android.inputmethod") ||
            pkg.startsWith("com.google.android.tvlauncher") ||
            pkg.startsWith("com.mitv.tvhome")

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_HOTKEY_PROGRAMMED = "com.h9.tvquickmenu.HOTKEY_PROGRAMMED"

        private const val SUPPRESS_MS = 2500L
        private const val RETURN_NONE = 0
        private const val RETURN_MENU = 1
        private const val RETURN_SETTINGS = 2

        @Volatile
        var instance: HotkeyAccessibilityService? = null
            private set
    }
}
