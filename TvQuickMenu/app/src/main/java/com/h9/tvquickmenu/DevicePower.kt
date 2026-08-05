package com.h9.tvquickmenu

import android.content.Context
import android.util.Log

/**
 * Усыпление / выключение ТВ.
 * Основной путь без ПК — системное действие службы специальных возможностей.
 * Локальный ADB используется только как дополнительный вариант, если уже настроен.
 */
object DevicePower {
    private const val TAG = "DevicePower"

    fun sleepNow(context: Context) {
        val app = context.applicationContext

        if (HotkeyAccessibilityService.instance?.lockScreen() == true) {
            Log.i(TAG, "LOCK_SCREEN action accepted")
            return
        }

        if (!AppPrefs(app).adbReady) {
            fallbackRuntime()
            return
        }

        // Дополнительный путь через ранее настроенный локальный ADB.
        LocalAdb.shellAsync(app, "input keyevent 223") { okSleep, detailSleep ->
            Log.i(TAG, "SLEEP keyevent ok=$okSleep detail=$detailSleep")
            if (!okSleep) {
                // 2) Кнопка питания (часто открывает меню выключения или гасит экран).
                LocalAdb.shellAsync(app, "input keyevent 26") { okPower, detailPower ->
                    Log.i(TAG, "POWER keyevent ok=$okPower detail=$detailPower")
                    if (!okPower) {
                        fallbackRuntime()
                    }
                }
            }
        }
    }

    private fun fallbackRuntime() {
        try {
            Runtime.getRuntime().exec(arrayOf("sh", "-c", "input keyevent 223"))
        } catch (e: Exception) {
            Log.w(TAG, "runtime sleep failed", e)
        }
    }
}
