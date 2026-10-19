package com.scanwell.app.pdf

/** The three facts about a JPEG picture that a PDF file needs to know. */
data class JpegInfo(
    val width: Int,
    val height: Int,
    /** 1 = grayscale, 3 = color (RGB), 4 = CMYK. */
    val components: Int,
)

/**
 * Reads the size of a JPEG straight from its header bytes, without decoding the picture.
 * A JPEG is a chain of "segments"; the size is stored in the one called SOF (start of frame).
 */
object JpegReader {

    /** Returns null when [bytes] is not a JPEG picture. */
    fun read(bytes: ByteArray): JpegInfo? {
        if (bytes.size < 4 || unsigned(bytes[0]) != 0xFF || unsigned(bytes[1]) != 0xD8) return null

        var i = 2
        while (i + 3 < bytes.size) {
            if (unsigned(bytes[i]) != 0xFF) {
                i++
                continue
            }
            val marker = unsigned(bytes[i + 1])
            if (marker == 0xFF) { // padding byte, the real marker follows
                i++
                continue
            }
            i += 2

            // Markers that stand alone and carry no data.
            if (marker == 0x01 || marker == 0xD8 || marker in 0xD0..0xD7) continue
            // End of image, or the picture data started before we found a size.
            if (marker == 0xD9 || marker == 0xDA) return null

            if (i + 1 >= bytes.size) return null
            val length = (unsigned(bytes[i]) shl 8) or unsigned(bytes[i + 1])
            if (length < 2) return null

            val isStartOfFrame = marker in 0xC0..0xCF &&
                marker != 0xC4 && marker != 0xC8 && marker != 0xCC
            if (isStartOfFrame) {
                if (i + 7 >= bytes.size) return null
                val height = (unsigned(bytes[i + 3]) shl 8) or unsigned(bytes[i + 4])
                val width = (unsigned(bytes[i + 5]) shl 8) or unsigned(bytes[i + 6])
                val components = unsigned(bytes[i + 7])
                if (width <= 0 || height <= 0) return null
                return JpegInfo(width, height, components)
            }
            i += length
        }
        return null
    }

    private fun unsigned(b: Byte): Int = b.toInt() and 0xFF
}
