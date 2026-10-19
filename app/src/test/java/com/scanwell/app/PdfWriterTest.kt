package com.scanwell.app

import com.scanwell.app.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfWriterTest {

    private fun makePdf(vararg pages: ByteArray, title: String = "Test"): ByteArray {
        val out = ByteArrayOutputStream()
        PdfWriter.write(out, pages.map { bytes -> { bytes } }, title)
        return out.toByteArray()
    }

    /** PDFs mix text and raw bytes; ISO-8859-1 turns every byte into exactly one character. */
    private fun asText(pdf: ByteArray): String = String(pdf, Charsets.ISO_8859_1)

    @Test
    fun startsAndEndsLikeAPdf() {
        val text = asText(makePdf(TestImages.portraitColor))
        assertTrue(text.startsWith("%PDF-1.4\n"))
        assertTrue(text.endsWith("%%EOF\n"))
    }

    @Test
    fun hasOnePdfPagePerPicture() {
        val text = asText(makePdf(TestImages.portraitColor, TestImages.landscapeGray, TestImages.portraitColor))
        assertTrue(text.contains("/Count 3"))
        assertEquals(3, Regex("/Type /Page ").findAll(text).count())
    }

    @Test
    fun pictureBytesAreStoredUnchanged() {
        val text = asText(makePdf(TestImages.portraitColor))
        assertTrue(text.contains(asText(TestImages.portraitColor)))
        assertTrue(text.contains("/Length ${TestImages.portraitColor.size} "))
    }

    @Test
    fun portraitPictureGetsPortraitPageAndLandscapeGetsLandscape() {
        val text = asText(makePdf(TestImages.portraitColor, TestImages.landscapeGray))
        assertTrue(text.contains("/MediaBox [0 0 595.00 842.00]"))
        assertTrue(text.contains("/MediaBox [0 0 842.00 595.00]"))
    }

    @Test
    fun grayPictureIsMarkedGrayAndColorIsMarkedColor() {
        val text = asText(makePdf(TestImages.portraitColor, TestImages.landscapeGray))
        assertTrue(text.contains("/ColorSpace /DeviceRGB"))
        assertTrue(text.contains("/ColorSpace /DeviceGray"))
    }

    /** The table at the end must point at the exact byte where each object starts. */
    @Test
    fun crossReferenceTablePointsAtEveryObject() {
        val pdf = makePdf(TestImages.portraitColor, TestImages.landscapeGray)
        val text = asText(pdf)
        val tableStart = text.substringAfterLast("startxref\n").substringBefore("\n").toInt()
        assertTrue(text.startsWith("xref\n", tableStart))

        val lines = text.substring(tableStart).lines()
        val objectCount = lines[1].substringAfter(" ").toInt() // "0 10" means 10 entries
        assertEquals(1 + 3 + 2 * 3, objectCount)
        for (number in 1 until objectCount) {
            val offset = lines[2 + number].substring(0, 10).toInt()
            assertTrue("object $number", text.startsWith("$number 0 obj\n", offset))
        }
    }

    @Test
    fun pictureIsScaledToFitThePageWithoutStretching() {
        val place = PdfWriter.placeOnA4(imageWidth = 1000, imageHeight = 2000)
        assertEquals(842f, place.height, 0.01f)
        assertEquals(421f, place.width, 0.01f)
        assertEquals((595f - 421f) / 2f, place.x, 0.01f) // centered left to right
        assertEquals(0f, place.y, 0.01f)
    }

    @Test
    fun titleInAnyLanguageIsStoredSafely() {
        // "Résumé" as UTF-16 hex, after the FEFF marker.
        val text = asText(makePdf(TestImages.portraitColor, title = "Résumé"))
        assertTrue(text.contains("/Title <FEFF005200E900730075006D00E9>"))
    }

    @Test
    fun refusesToMakeAnEmptyPdf() {
        try {
            PdfWriter.write(ByteArrayOutputStream(), emptyList(), "Empty")
            fail("Expected an error")
        } catch (expected: IllegalArgumentException) {
            // good
        }
    }

    @Test
    fun refusesPagesThatAreNotJpeg() {
        try {
            makePdf("not a picture".toByteArray())
            fail("Expected an error")
        } catch (expected: IllegalArgumentException) {
            // good
        }
    }
}
