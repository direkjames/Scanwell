package com.scanwell.app

import com.scanwell.app.pdf.Protection
import com.scanwell.app.util.PasswordRules
import com.scanwell.app.util.PasswordRules.Strength
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PasswordRulesTest {

    @Test
    fun rejectsEmptyShortAndTooLongPasswords() {
        assertNotNull(PasswordRules.problem(""))
        assertNotNull(PasswordRules.problem("abc"))
        assertNull(PasswordRules.problem("abcd"))
        assertNull(PasswordRules.problem("a".repeat(PasswordRules.MAX_LENGTH)))
        assertNotNull(PasswordRules.problem("a".repeat(PasswordRules.MAX_LENGTH + 1)))
    }

    @Test
    fun strengthGrowsWithLengthAndVariety() {
        assertEquals(Strength.EMPTY, PasswordRules.strength(""))
        assertEquals(Strength.WEAK, PasswordRules.strength("aaaa"))
        assertEquals(Strength.WEAK, PasswordRules.strength("1234"))
        assertEquals(Strength.FAIR, PasswordRules.strength("Summer24"))
        assertEquals(Strength.GOOD, PasswordRules.strength("Summer2024!x"))
        assertEquals(Strength.STRONG, PasswordRules.strength("Correct-Horse-Battery-9"))
    }

    @Test
    fun protectionNeverPrintsItsPasswords() {
        val text = Protection("hunter2", "owner-secret", true).toString()
        assertFalse(text.contains("hunter2"))
        assertFalse(text.contains("owner-secret"))
    }
}
