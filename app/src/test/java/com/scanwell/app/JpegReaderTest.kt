package com.scanwell.app

import com.scanwell.app.pdf.JpegInfo
import com.scanwell.app.pdf.JpegReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JpegReaderTest {

    @Test
    fun readsSizeOfColorPicture() {
        assertEquals(JpegInfo(width = 16, height = 24, components = 3), JpegReader.read(TestImages.portraitColor))
    }

    @Test
    fun readsSizeOfGrayPicture() {
        assertEquals(JpegInfo(width = 32, height = 8, components = 1), JpegReader.read(TestImages.landscapeGray))
    }

    @Test
    fun rejectsThingsThatAreNotJpeg() {
        assertNull(JpegReader.read(ByteArray(0)))
        assertNull(JpegReader.read("%PDF-1.4 this is not a picture".toByteArray()))
        // A PNG file starts with these bytes.
        assertNull(JpegReader.read(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)))
    }

    @Test
    fun rejectsJpegThatIsCutOffBeforeItsSize() {
        assertNull(JpegReader.read(TestImages.portraitColor.copyOf(12)))
    }
}
