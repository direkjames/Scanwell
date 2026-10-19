package com.scanwell.app.pdf

/** The password choices for [PdfTools.protect]. */
class Protection(
    val userPassword: String,
    /** Needed only when [restrict] is on. Otherwise a random one is used and thrown away. */
    val ownerPassword: String?,
    /** Also forbid editing and copying text (printing stays allowed). */
    val restrict: Boolean,
) {
    // Passwords must never end up in a log or a crash report.
    override fun toString(): String = "Protection(redacted)"
}
