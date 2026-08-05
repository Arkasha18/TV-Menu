package com.h9.tvquickmenu

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock

object SleepScheduler {
    fun schedule(context: Context, minutes: Int) {
        val app = context.applicationContext
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        AppPrefs(app).sleepAtMillis = triggerAt

        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(app)
        val elapsed = SystemClock.elapsedRealtime() + minutes * 60_000L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, elapsed, pi)
        } else {
            am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, elapsed, pi)
        }
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        AppPrefs(app).sleepAtMillis = 0L
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(app))
    }

    fun remainingLabel(context: Context): String {
        val at = AppPrefs(context).sleepAtMillis
        if (at <= 0L) return "таймер сна не задан"
        val leftMs = at - System.currentTimeMillis()
        if (leftMs <= 0L) return "таймер сна истёк"
        val minutes = ((leftMs + 59_999) / 60_000).toInt()
        return if (minutes < 60) {
            "сон через $minutes мин"
        } else {
            val h = minutes / 60
            val m = minutes % 60
            "сон через ${h} ч ${m} мин"
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SleepAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class SleepAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        AppPrefs(context).sleepAtMillis = 0L
        DevicePower.sleepNow(context)
    }
}
