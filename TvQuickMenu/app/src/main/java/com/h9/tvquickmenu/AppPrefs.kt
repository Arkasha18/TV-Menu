package com.h9.tvquickmenu

import android.content.Context
import android.view.KeyEvent

class AppPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var hotkeyCode: Int
        get() = prefs.getInt(KEY_HOTKEY, KeyEvent.KEYCODE_UNKNOWN)
        set(value) = prefs.edit().putInt(KEY_HOTKEY, value).apply()

    /**
     * Сырой код кнопки из getevent, например "0001 00e6".
     * Так ловятся спецкнопки Xiaomi (Иви и т.п.), которые не дают обычный KeyEvent.
     */
    var geteventSignature: String
        get() = prefs.getString(KEY_GETEVENT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GETEVENT, value).apply()

    var programmingMode: Boolean
        get() = prefs.getBoolean(KEY_PROGRAMMING, false)
        set(value) = prefs.edit().putBoolean(KEY_PROGRAMMING, value).apply()

    var dimLevel: Int
        get() = prefs.getInt(KEY_DIM, 0).coerceIn(0, 5)
        set(value) = prefs.edit().putInt(KEY_DIM, value.coerceIn(0, 5)).apply()

    var sleepAtMillis: Long
        get() = prefs.getLong(KEY_SLEEP_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_SLEEP_AT, value).apply()

    var adbHost: String
        get() = prefs.getString(KEY_ADB_HOST, "127.0.0.1") ?: "127.0.0.1"
        set(value) = prefs.edit().putString(KEY_ADB_HOST, value).apply()

    var adbPort: Int
        get() = prefs.getInt(KEY_ADB_PORT, 5555)
        set(value) = prefs.edit().putInt(KEY_ADB_PORT, value).apply()

    var adbReady: Boolean
        get() = prefs.getBoolean(KEY_ADB_READY, false)
        set(value) = prefs.edit().putBoolean(KEY_ADB_READY, value).apply()

    fun clearHotkey() {
        prefs.edit()
            .remove(KEY_HOTKEY)
            .remove(KEY_GETEVENT)
            .apply()
    }

    fun hasHotkey(): Boolean =
        geteventSignature.isNotBlank() || hotkeyCode != KeyEvent.KEYCODE_UNKNOWN

    fun hotkeyLabel(): String {
        val ge = geteventSignature
        if (ge.isNotBlank()) return "getevent $ge"
        return KeyNames.describe(hotkeyCode)
    }

    companion object {
        private const val PREFS = "tv_quick_menu"
        private const val KEY_HOTKEY = "hotkey_code"
        private const val KEY_GETEVENT = "getevent_sig"
        private const val KEY_PROGRAMMING = "programming_mode"
        private const val KEY_DIM = "dim_level"
        private const val KEY_SLEEP_AT = "sleep_at_millis"
        private const val KEY_ADB_HOST = "adb_host"
        private const val KEY_ADB_PORT = "adb_port"
        private const val KEY_ADB_READY = "adb_ready"

        val SLEEP_MINUTES = listOf(1, 30, 60, 90, 120, 150, 180)
    }
}
