package com.h9.tvquickmenu

import android.content.Context

/**
 * Включение службы кнопок через локальный ADB:
 * сначала снимает блокировку ограниченных настроек, затем включает accessibility.
 */
object AccessibilityFix {
    private const val SERVICE =
        "com.h9.tvquickmenu/com.h9.tvquickmenu.HotkeyAccessibilityService"

    data class Result(val ok: Boolean, val message: String)

    fun enableViaLocalAdb(context: Context): Result {
        val probe = LocalAdb.probe(context)
        if (!probe.ok) {
            return Result(
                false,
                "Локальный ADB недоступен (${probe.detail}).\n" +
                    "Включите отладку по USB / Wi‑Fi в параметрах разработчика " +
                    "и разрешите запрос на экране ТВ, затем повторите."
            )
        }

        val appops = LocalAdb.shell(
            context,
            "cmd appops set com.h9.tvquickmenu ACCESS_RESTRICTED_SETTINGS allow"
        )
        if (!appops.first) {
            return Result(
                false,
                "Не удалось снять ограничение настроек.\n${appops.second}"
            )
        }

        val setService = LocalAdb.shell(
            context,
            "settings put secure enabled_accessibility_services $SERVICE"
        )
        if (!setService.first) {
            return Result(
                false,
                "Ограничение снято, но службу включить не удалось.\n${setService.second}"
            )
        }

        val setEnabled = LocalAdb.shell(
            context,
            "settings put secure accessibility_enabled 1"
        )
        if (!setEnabled.first) {
            return Result(
                false,
                "Служба записана, но флаг accessibility_enabled не выставился.\n${setEnabled.second}"
            )
        }

        return Result(
            true,
            "Готово. Ограничение снято, служба TV Меню включена.\nADB: ${probe.endpoint}"
        )
    }
}
