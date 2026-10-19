package com.scanwell.app

import com.scanwell.app.util.Sizes
import org.junit.Assert.assertEquals
import org.junit.Test

class SizesTest {

    @Test
    fun formatsEachRange() {
        assertEquals("0 B", Sizes.format(0))
        assertEquals("1023 B", Sizes.format(1023))
        assertEquals("1.5 KB", Sizes.format(1536))
        assertEquals("2.0 MB", Sizes.format(2L * 1024 * 1024))
        assertEquals("1.50 GB", Sizes.format(1536L * 1024 * 1024))
    }

    @Test
    fun negativeSizesAreTreatedAsZero() {
        assertEquals("0 B", Sizes.format(-5))
    }

    @Test
    fun percentSaved() {
        assertEquals(50, Sizes.percentSaved(1000, 500))
        assertEquals(99, Sizes.percentSaved(1000, 10))
        assertEquals(0, Sizes.percentSaved(1000, 1000))
        assertEquals(0, Sizes.percentSaved(1000, 1500))
        assertEquals(0, Sizes.percentSaved(0, 0))
    }
}
