package com.scanwell.app

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/** Adds the "Scan" shortcut that appears when you long-press the app icon. */
object Shortcuts {

    const val ACTION_SCAN = "com.scanwell.app.action.SCAN"

    fun install(context: Context) {
        val intent = Intent(context, MainActivity::class.java).setAction(ACTION_SCAN)
        val shortcut = ShortcutInfoCompat.Builder(context, "scan")
            .setShortLabel(context.getString(R.string.shortcut_scan_short))
            .setLongLabel(context.getString(R.string.shortcut_scan_long))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_scan))
            .setIntent(intent)
            .build()
        try {
            ShortcutManagerCompat.setDynamicShortcuts(context, listOf(shortcut))
        } catch (e: Exception) {
            // Some launchers do not support shortcuts. The app works fine without them.
        }
    }
}
