package com.h9.tvquickmenu

import android.view.KeyEvent

/**
 * Спецкнопки пульта Xiaomi: код Android ↔ сырой код getevent.
 */
object XiaomiAppButtons {
    private val dedicatedSignatures = setOf(
        "0001 01b1", // Иви
        "0001 0098", // Okko / соседняя кнопка
        "0001 0243", // Кинопоиск
        "0001 01a5"  // Hotstar / соседняя кнопка
    )

    /** linux keycode → getevent "0001 xxxx" */
    fun signatureForAndroidKey(androidKeyCode: Int): String? {
        val linux = when (androidKeyCode) {
            KeyEvent.KEYCODE_BUTTON_12 -> 433 // Иви
            KeyEvent.KEYCODE_BUTTON_13 -> 152 // Okko / рядом
            KeyEvent.KEYCODE_BUTTON_14 -> 579 // Кинопоиск и т.п.
            KeyEvent.KEYCODE_BUTTON_15 -> 421
            else -> return null
        }
        return "0001 %04x".format(linux)
    }

    fun androidKeyForSignature(signature: String): Int? =
        when (signature.lowercase()) {
            "0001 01b1" -> KeyEvent.KEYCODE_BUTTON_12
            "0001 0098" -> KeyEvent.KEYCODE_BUTTON_13
            "0001 0243" -> KeyEvent.KEYCODE_BUTTON_14
            "0001 01a5" -> KeyEvent.KEYCODE_BUTTON_15
            else -> null
        }

    fun isDedicatedAppButton(signature: String): Boolean =
        dedicatedSignatures.any { signature.equals(it, ignoreCase = true) }

    fun matchesStoredHotkey(prefs: AppPrefs, signature: String): Boolean {
        val wanted = prefs.geteventSignature
        if (wanted.isNotBlank()) {
            return signature.equals(wanted, ignoreCase = true)
        }
        val mapped = signatureForAndroidKey(prefs.hotkeyCode) ?: return false
        return signature.equals(mapped, ignoreCase = true)
    }

    /** Если кнопка сохранена только как KeyEvent — дописать сырую подпись. */
    fun ensureGeteventSignature(prefs: AppPrefs) {
        if (prefs.geteventSignature.isNotBlank()) return
        val mapped = signatureForAndroidKey(prefs.hotkeyCode) ?: return
        prefs.geteventSignature = mapped
    }
}
