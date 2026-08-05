package com.h9.tvquickmenu

import android.view.KeyEvent

object KeyNames {
    fun describe(keyCode: Int): String {
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) return "не задана"
        val friendly = when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_12 -> "кнопка приложения (Иви/Wink и т.п.)"
            KeyEvent.KEYCODE_BUTTON_13 -> "кнопка приложения (Okko и т.п.)"
            KeyEvent.KEYCODE_BUTTON_14 -> "кнопка приложения (Кинопоиск и т.п.)"
            KeyEvent.KEYCODE_BUTTON_3 -> "кнопка приложения (YouTube и т.п.)"
            KeyEvent.KEYCODE_BUTTON_6 -> "кнопка Netflix"
            KeyEvent.KEYCODE_BUTTON_7 -> "кнопка Prime Video"
            else -> null
        }
        val raw = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
        return if (friendly != null) "$friendly [$raw/$keyCode]" else "$raw ($keyCode)"
    }
}
