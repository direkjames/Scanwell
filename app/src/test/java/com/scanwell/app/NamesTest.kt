package com.scanwell.app

import com.scanwell.app.util.Names
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NamesTest {

    @Test
    fun keepsNormalTitles() {
        assertEquals("Lease contract 2026", Names.safeFileName("Lease contract 2026"))
    }

    @Test
    fun replacesCharactersThatFileNamesCannotHave() {
        assertEquals("Invoice 10_12 _final_", Names.safeFileName("Invoice 10/12 <final>"))
    }

    @Test
    fun tidiesUpSpacesAndDots() {
        assertEquals("My scan", Names.safeFileName("   My    scan ...  "))
    }

    @Test
    fun usesFallbackWhenNothingIsLeft() {
        assertEquals("Scan", Names.safeFileName("   "))
        assertEquals("Scan", Names.safeFileName("..."))
    }

    @Test
    fun cutsVeryLongTitles() {
        assertEquals(80, Names.safeFileName("a".repeat(500)).length)
    }

    @Test
    fun defaultTitleContainsTheDate() {
        val title = Names.defaultTitle(nowMillis = 0L)
        assertTrue(title, Regex("Scan \\d{4}-\\d{2}-\\d{2} \\d{2}\\.\\d{2}").matches(title))
    }
}
