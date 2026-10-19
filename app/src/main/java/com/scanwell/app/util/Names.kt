package com.scanwell.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small helpers for titles and file names. No Android classes, so they are unit tested. */
object Names {

    private val forbidden = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
    private val spaces = Regex("\\s+")

    /** Turns any title into a name that is safe to use for a file. */
    fun safeFileName(title: String, fallback: String = "Scan"): String {
        val cleaned = title
            .replace(forbidden, "_")
            .replace(spaces, " ")
            .trim(' ', '.')
            .take(80)
            .trim(' ', '.')
        return cleaned.ifEmpty { fallback }
    }

    /** Example: "Scan 2026-10-12 07.30". */
    fun defaultTitle(nowMillis: Long, prefix: String = "Scan"): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH.mm", Locale.US).format(Date(nowMillis))
        return "$prefix $stamp"
    }
}
