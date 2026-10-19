package com.scanwell.app.pdf

/**
 * How much to shrink pictures when exporting.
 * [maxSide] is the longest side in pixels and [jpegQuality] goes from 0 (tiny, ugly) to 100 (best).
 */
enum class ExportQuality(
    val label: String,
    val hint: String,
    val maxSide: Int,
    val jpegQuality: Int,
) {
    ORIGINAL("Original", "Best quality, largest file", 0, 100),
    MEDIUM("Medium", "Smaller file, still sharp", 1800, 75),
    SMALL("Small", "Fits email and upload limits", 1200, 55),
}
