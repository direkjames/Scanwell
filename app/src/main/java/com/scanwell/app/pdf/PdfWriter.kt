package com.scanwell.app.pdf

import java.io.OutputStream
import java.util.Locale

/**
 * Writes a PDF file where every page is one JPEG picture.
 *
 * The JPEG bytes are placed into the PDF exactly as they are (PDF can show JPEGs
 * natively), so the PDF is about the same size as the pictures and loses no quality.
 * This file uses no Android classes, which is why it can be unit tested on a computer.
 */
object PdfWriter {

    // A4 paper measured in PDF points (1 point = 1/72 inch).
    const val A4_SHORT_SIDE = 595f
    const val A4_LONG_SIDE = 842f

    /** Where a picture sits on its page. */
    data class Placement(
        val pageWidth: Float,
        val pageHeight: Float,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
    )

    /**
     * Picks portrait or landscape A4 to match the picture, then scales the picture
     * to fill the page as much as possible without stretching it.
     */
    fun placeOnA4(imageWidth: Int, imageHeight: Int): Placement {
        val landscape = imageWidth > imageHeight
        val pageWidth = if (landscape) A4_LONG_SIDE else A4_SHORT_SIDE
        val pageHeight = if (landscape) A4_SHORT_SIDE else A4_LONG_SIDE
        val scale = minOf(pageWidth / imageWidth, pageHeight / imageHeight)
        val width = imageWidth * scale
        val height = imageHeight * scale
        return Placement(
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            x = (pageWidth - width) / 2f,
            y = (pageHeight - height) / 2f,
            width = width,
            height = height,
        )
    }

    /**
     * @param pages one function per page that returns that page's JPEG bytes. Using functions
     *   means only one page has to be held in memory at a time.
     * @throws IllegalArgumentException if there are no pages or a page is not a JPEG.
     */
    fun write(out: OutputStream, pages: List<() -> ByteArray>, title: String) {
        require(pages.isNotEmpty()) { "A PDF needs at least one page" }

        val writer = CountingWriter(out)
        // A PDF is a list of numbered objects. We use: 1 = catalog, 2 = page list, 3 = info,
        // then three objects per page (the page, its drawing commands, its picture).
        val objectCount = 3 + pages.size * 3
        val offsets = LongArray(objectCount + 1)

        writer.ascii("%PDF-1.4\n")
        // A comment with four non-text bytes tells other programs this file is binary.
        writer.bytes(byteArrayOf('%'.code.toByte(), 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), '\n'.code.toByte()))

        offsets[1] = writer.position
        writer.ascii("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        offsets[2] = writer.position
        val kids = pages.indices.joinToString(" ") { index -> "${pageObject(index)} 0 R" }
        writer.ascii("2 0 obj\n<< /Type /Pages /Count ${pages.size} /Kids [$kids] >>\nendobj\n")

        offsets[3] = writer.position
        writer.ascii("3 0 obj\n<< /Title ${pdfText(title)} /Producer ${pdfText("Scanwell")} >>\nendobj\n")

        pages.forEachIndexed { index, loadPage ->
            val jpeg = loadPage()
            val info = JpegReader.read(jpeg)
                ?: throw IllegalArgumentException("Page ${index + 1} is not a JPEG picture")
            val place = placeOnA4(info.width, info.height)
            val pageNumber = pageObject(index)
            val contentNumber = pageNumber + 1
            val imageNumber = pageNumber + 2

            offsets[pageNumber] = writer.position
            writer.ascii(
                "$pageNumber 0 obj\n" +
                    "<< /Type /Page /Parent 2 0 R " +
                    "/MediaBox [0 0 ${number(place.pageWidth)} ${number(place.pageHeight)}] " +
                    "/Resources << /XObject << /Im0 $imageNumber 0 R >> >> " +
                    "/Contents $contentNumber 0 R >>\nendobj\n",
            )

            // Drawing commands: save state, scale and move, draw picture, restore state.
            val commands = "q ${number(place.width)} 0 0 ${number(place.height)} " +
                "${number(place.x)} ${number(place.y)} cm /Im0 Do Q"
            offsets[contentNumber] = writer.position
            writer.ascii("$contentNumber 0 obj\n<< /Length ${commands.length} >>\nstream\n$commands\nendstream\nendobj\n")

            val colorSpace = when (info.components) {
                1 -> "/DeviceGray"
                4 -> "/DeviceCMYK /Decode [1 0 1 0 1 0 1 0]"
                else -> "/DeviceRGB"
            }
            offsets[imageNumber] = writer.position
            writer.ascii(
                "$imageNumber 0 obj\n" +
                    "<< /Type /XObject /Subtype /Image /Width ${info.width} /Height ${info.height} " +
                    "/ColorSpace $colorSpace /BitsPerComponent 8 /Filter /DCTDecode " +
                    "/Length ${jpeg.size} >>\nstream\n",
            )
            writer.bytes(jpeg)
            writer.ascii("\nendstream\nendobj\n")
        }

        // The cross-reference table: the byte position of every object,
        // so a PDF reader can jump straight to any page.
        val tableStart = writer.position
        writer.ascii("xref\n0 ${objectCount + 1}\n")
        writer.ascii("0000000000 65535 f \n")
        for (objectNumber in 1..objectCount) {
            writer.ascii(String.format(Locale.US, "%010d 00000 n \n", offsets[objectNumber]))
        }
        writer.ascii("trailer\n<< /Size ${objectCount + 1} /Root 1 0 R /Info 3 0 R >>\n")
        writer.ascii("startxref\n$tableStart\n%%EOF\n")
        out.flush()
    }

    private fun pageObject(pageIndex: Int): Int = 4 + pageIndex * 3

    private fun number(value: Float): String = String.format(Locale.US, "%.2f", value)

    /** PDF text written as hexadecimal UTF-16, which is safe for every language and symbol. */
    private fun pdfText(text: String): String {
        val hex = StringBuilder("<FEFF")
        for (byte in text.toByteArray(Charsets.UTF_16BE)) {
            hex.append(String.format(Locale.US, "%02X", byte.toInt() and 0xFF))
        }
        return hex.append('>').toString()
    }

    /** Writes to [out] while remembering how many bytes have gone out so far. */
    private class CountingWriter(private val out: OutputStream) {
        var position = 0L
            private set

        fun ascii(text: String) = bytes(text.toByteArray(Charsets.US_ASCII))

        fun bytes(data: ByteArray) {
            out.write(data)
            position += data.size
        }
    }
}
