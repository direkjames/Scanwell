package com.scanwell.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** Helpers for loading pictures without using more memory than needed. */
object Bitmaps {

    /** Width and height of a picture file, read from its header only. null if it is not a picture. */
    fun size(file: File): Pair<Int, Int>? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.path, options)
        return if (options.outWidth > 0 && options.outHeight > 0) {
            options.outWidth to options.outHeight
        } else {
            null
        }
    }

    /** Width divided by height. Falls back to the shape of A4 paper. */
    fun aspectRatio(file: File): Float {
        val (width, height) = size(file) ?: return 1f / 1.414f
        return width.toFloat() / height.toFloat()
    }

    /**
     * Loads a picture at reduced size: the result's longest side is at least [maxSide]
     * (when the original is that big) but less than twice that. Much faster and lighter
     * than loading a full camera picture just to show a small thumbnail.
     */
    fun decodeSampled(file: File, maxSide: Int): Bitmap? {
        val (width, height) = size(file) ?: return null
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxSide) sample *= 2
        val options = BitmapFactory.Options()
        options.inSampleSize = sample
        return BitmapFactory.decodeFile(file.path, options)
    }

    /** Shrinks [bitmap] so its longest side is at most [maxSide]. Never enlarges. */
    fun scaleToMaxSide(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val scale = maxSide.toFloat() / longest
        val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
