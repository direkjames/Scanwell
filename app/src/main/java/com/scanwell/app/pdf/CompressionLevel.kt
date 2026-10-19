package com.scanwell.app.pdf

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * How hard to squeeze the pictures inside a PDF.
 * [maxSide] is the longest side, in pixels, a picture may keep; bigger ones are scaled down.
 * [jpegQuality] goes from 0 (tiny, ugly) to 100 (best). [stripExtras] also drops page
 * thumbnails and hidden metadata.
 */
enum class CompressionLevel(
    val label: String,
    val hint: String,
    val maxSide: Int,
    val jpegQuality: Int,
    val stripExtras: Boolean,
) {
    LOW("Low", "Best quality, modest savings", 2400, 85, false),
    MEDIUM("Medium", "Balanced for email and chat", 1600, 70, false),
    HIGH("High", "Smallest file, softer images", 1100, 50, true);

    /**
     * The size a picture of [width] x [height] pixels should be shrunk to.
     * Never enlarges, never goes below one pixel, keeps the shape.
     */
    fun targetSize(width: Int, height: Int): Pair<Int, Int> {
        val longest = max(width, height)
        if (longest <= maxSide || longest <= 0) return width to height
        val scale = maxSide.toFloat() / longest
        return (width * scale).roundToInt().coerceAtLeast(1) to
            (height * scale).roundToInt().coerceAtLeast(1)
    }
}
