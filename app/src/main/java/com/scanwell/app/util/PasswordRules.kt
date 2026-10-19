package com.scanwell.app.util

/** Rules and a strength guess for PDF passwords. No Android classes, so it is unit tested. */
object PasswordRules {

    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 64

    enum class Strength(val label: String, val fraction: Float) {
        EMPTY("", 0f),
        WEAK("Weak", 0.25f),
        FAIR("Fair", 0.5f),
        GOOD("Good", 0.75f),
        STRONG("Strong", 1f),
    }

    /** Null when the password is acceptable, otherwise a short message for the person. */
    fun problem(password: String): String? = when {
        password.isEmpty() -> "Enter a password"
        password.length < MIN_LENGTH -> "Use at least $MIN_LENGTH characters"
        password.length > MAX_LENGTH -> "Use at most $MAX_LENGTH characters"
        else -> null
    }

    fun strength(password: String): Strength {
        if (password.isEmpty()) return Strength.EMPTY
        var classes = 0
        if (password.any { it.isLowerCase() }) classes++
        if (password.any { it.isUpperCase() }) classes++
        if (password.any { it.isDigit() }) classes++
        if (password.any { !it.isLetterOrDigit() }) classes++
        val distinct = password.toSet().size
        val score = when {
            password.length < MIN_LENGTH || distinct < 3 -> 0
            password.length >= 14 && classes >= 3 -> 3
            password.length >= 10 && classes >= 3 -> 2
            password.length >= 8 && classes >= 2 -> 1
            password.length >= 12 -> 2
            else -> 0
        }
        return when (score) {
            3 -> Strength.STRONG
            2 -> Strength.GOOD
            1 -> Strength.FAIR
            else -> Strength.WEAK
        }
    }
}
