package com.h9.tvquickmenu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val level = AppPrefs(context).dimLevel
        if (level > 0) {
            DimOverlayService.setLevel(context, level)
        }
    }
}
