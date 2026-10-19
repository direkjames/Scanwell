package com.scanwell.app.util

import java.util.Locale

/** Turns byte counts into short, readable text. No Android classes, so it is unit tested. */
object Sizes {

    /** Example: 1536 -> "1.5 KB". */
    fun format(bytes: Long): String {
        val value = bytes.coerceAtLeast(0L).toDouble()
        return when {
            value < 1024 -> "${value.toLong()} B"
            value < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", value / 1024)
            value < 1024.0 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", value / (1024 * 1024))
            else -> String.format(Locale.US, "%.2f GB", value / (1024.0 * 1024 * 1024))
        }
    }

    /** Whole percent saved, from 0 to 100. 0 when the file did not shrink or [before] is empty. */
    fun percentSaved(before: Long, after: Long): Int {
        if (before <= 0L || after >= before) return 0
        return (((before - after) * 100.0) / before).toInt().coerceIn(0, 100)
    }
}
