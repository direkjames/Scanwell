package com.scanwell.app

import com.scanwell.app.pdf.CompressionLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompressionLevelTest {

    @Test
    fun smallPicturesAreNeverEnlarged() {
        assertEquals(800 to 600, CompressionLevel.HIGH.targetSize(800, 600))
        assertEquals(1100 to 1100, CompressionLevel.HIGH.targetSize(1100, 1100))
    }

    @Test
    fun largePicturesShrinkToTheLimitAndKeepTheirShape() {
        val (w, h) = CompressionLevel.MEDIUM.targetSize(3200, 2400)
        assertEquals(1600, w)
        assertEquals(1200, h)

        val (pw, ph) = CompressionLevel.HIGH.targetSize(2000, 4000)
        assertEquals(550, pw)
        assertEquals(1100, ph)
    }

    @Test
    fun aVerySlimPictureNeverCollapsesToZero() {
        val (w, h) = CompressionLevel.HIGH.targetSize(20000, 1)
        assertEquals(1100, w)
        assertTrue(h >= 1)
    }

    @Test
    fun higherLevelsAreStrictlySmallerAndLowerQuality() {
        val levels = CompressionLevel.entries
        for (i in 1 until levels.size) {
            assertTrue(levels[i].maxSide < levels[i - 1].maxSide)
            assertTrue(levels[i].jpegQuality < levels[i - 1].jpegQuality)
        }
    }
}
