package com.scanwell.app.scan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.scanwell.app.util.Bitmaps
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min

/**
 * ID card mode: draws the front and the back of a card onto one white A4 page,
 * front in the top half and back in the bottom half, ready to print or send.
 */
object IdCardComposer {

    // A4 paper at 150 dots per inch.
    private const val PAGE_WIDTH = 1240
    private const val PAGE_HEIGHT = 1754

    fun compose(front: File, back: File?, output: File) {
        val page = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(page)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val sides = listOfNotNull(front, back)
        val slotHeight = PAGE_HEIGHT / 2f
        sides.forEachIndexed { index, file ->
            val card = Bitmaps.decodeSampled(file, 1600)
                ?: throw IOException("The scanned card is not a picture")
            // Each side gets at most 60% of the page width and 70% of its half of the page.
            val scale = min(PAGE_WIDTH * 0.60f / card.width, slotHeight * 0.70f / card.height)
            val width = card.width * scale
            val height = card.height * scale
            val left = (PAGE_WIDTH - width) / 2f
            val top = slotHeight * index + (slotHeight - height) / 2f
            canvas.drawBitmap(card, null, RectF(left, top, left + width, top + height), paint)
            card.recycle()
        }

        FileOutputStream(output).use { sink ->
            page.compress(Bitmap.CompressFormat.JPEG, 92, sink)
        }
        page.recycle()
    }
}
