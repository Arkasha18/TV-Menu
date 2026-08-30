package com.h9.tvquickmenu

import android.content.Context
import android.os.Build

/**
 * Включение службы кнопок через локальный ADB:
 * на Android 13+ снимает блокировку ограниченных настроек, затем включает accessibility.
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
        }

        val currentServices = LocalAdb.shell(
            context,
            "settings get secure enabled_accessibility_services"
        )
        if (!currentServices.first) {
            return Result(
                false,
                "Не удалось прочитать список включённых служб.\n${currentServices.second}"
            )
        }

        val enabledServices = mergeEnabledServices(currentServices.second, SERVICE)
        val setService = LocalAdb.shell(
            context,
            "settings put secure enabled_accessibility_services ${shellQuote(enabledServices)}"
        )
        if (!setService.first) {
            return Result(
                false,
                "Службу включить не удалось.\n${setService.second}"
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
            "Готово. Служба TV Меню включена, другие службы сохранены.\n" +
                "ADB: ${probe.endpoint}"
        )
    }

    internal fun mergeEnabledServices(current: String, service: String): String {
        val entries = current
            .trim()
            .takeUnless { it.isEmpty() || it.equals("null", ignoreCase = true) }
            ?.split(':')
            .orEmpty()
            .map(String::trim)
            .filter(String::isNotEmpty)

        val merged = mutableListOf<String>()
        for (entry in entries + service) {
            if (merged.none { it.equals(entry, ignoreCase = true) }) {
                merged += entry
            }
        }
        return merged.joinToString(":")
    }

    internal fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"
}
