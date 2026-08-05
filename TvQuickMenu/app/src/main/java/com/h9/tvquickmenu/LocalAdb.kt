package com.h9.tvquickmenu

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import dadb.AdbKeyPair
import dadb.Dadb
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Выполнение shell-команд через ADB на самом устройстве.
 * ПК нужен только один раз: включить сетевой ADB (adb tcpip 5555).
 */
object LocalAdb {
    private const val TAG = "LocalAdb"
    private val executor = Executors.newSingleThreadExecutor()

    data class ProbeResult(val ok: Boolean, val endpoint: String, val detail: String)

    fun wifiIp(context: Context): String? {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val ip = wm.connectionInfo.ipAddress
            if (ip == 0) null
            else String.format(
                "%d.%d.%d.%d",
                ip and 0xff,
                ip shr 8 and 0xff,
                ip shr 16 and 0xff,
                ip shr 24 and 0xff
            )
        } catch (_: Exception) {
            null
        }
    }

    fun endpoints(context: Context): List<Pair<String, Int>> {
        val prefs = AppPrefs(context)
        val list = linkedSetOf<Pair<String, Int>>()
        list += prefs.adbHost to prefs.adbPort
        list += "127.0.0.1" to 5555
        wifiIp(context)?.let { list += it to 5555 }
        return list.toList()
    }

    /** Ключи ADB в каталоге приложения — иначе dadb падает на .android/adbkey. */
    fun open(context: Context, host: String, port: Int): Dadb {
        val dir = File(context.applicationContext.filesDir, "adb")
        if (!dir.exists()) dir.mkdirs()
        val priv = File(dir, "adbkey")
        val pub = File(dir, "adbkey.pub")
        if (!priv.exists() || !pub.exists()) {
            AdbKeyPair.generate(priv, pub)
        }
        val keyPair = AdbKeyPair.read(priv, pub)
        return Dadb.create(host, port, keyPair)
    }

    fun probe(context: Context): ProbeResult {
        val error = AtomicReference("нет ответа")
        for ((host, port) in endpoints(context)) {
            val endpoint = "$host:$port"
            try {
                open(context, host, port).use { dadb ->
                    val response = dadb.shell("echo tvquickmenu_ok")
                    val out = response.allOutput.trim()
                    if (response.exitCode == 0 && out.contains("tvquickmenu_ok")) {
                        val prefs = AppPrefs(context)
                        prefs.adbHost = host
                        prefs.adbPort = port
                        prefs.adbReady = true
                        return ProbeResult(true, endpoint, out)
                    }
                    error.set("код ${response.exitCode}: $out")
                }
            } catch (e: Exception) {
                Log.w(TAG, "probe failed $endpoint", e)
                error.set(e.message ?: e.javaClass.simpleName)
            }
        }
        AppPrefs(context).adbReady = false
        return ProbeResult(false, "-", error.get())
    }

    fun shellAsync(context: Context, command: String, onDone: ((Boolean, String) -> Unit)? = null) {
        executor.execute {
            val result = shell(context, command)
            onDone?.invoke(result.first, result.second)
        }
    }

    fun shell(context: Context, command: String): Pair<Boolean, String> {
        var lastError = "ADB недоступен"
        for ((host, port) in endpoints(context)) {
            try {
                open(context, host, port).use { dadb ->
                    val response = dadb.shell(command)
                    val out = response.allOutput.trim()
                    if (response.exitCode == 0) {
                        AppPrefs(context).adbReady = true
                        AppPrefs(context).adbHost = host
                        AppPrefs(context).adbPort = port
                        return true to out
                    }
                    lastError = "код ${response.exitCode}: $out"
                }
            } catch (e: Exception) {
                lastError = e.message ?: e.javaClass.simpleName
            }
        }
        AppPrefs(context).adbReady = false
        return false to lastError
    }

    fun awaitShell(context: Context, command: String, timeoutSec: Long = 8): Pair<Boolean, String> {
        val future = executor.submit<Pair<Boolean, String>> { shell(context, command) }
        return try {
            future.get(timeoutSec, TimeUnit.SECONDS)
        } catch (e: Exception) {
            false to (e.message ?: "timeout")
        }
    }
}
