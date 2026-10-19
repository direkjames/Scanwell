package com.scanwell.app.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.scanwell.app.pdf.JpegReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Copies the pictures the scanner produced into our own temporary files, and makes sure
 * each one is a plain upright JPEG (which is what the rest of the app expects).
 */
object PageImporter {

    fun importAll(context: Context, uris: List<Uri>): List<File> {
        val folder = File(context.cacheDir, "incoming")
        folder.mkdirs()
        return uris.mapIndexed { index, uri ->
            val target = File(folder, "page_${System.nanoTime()}_$index.jpg")
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Could not open scanned page ${index + 1}")
            input.use { source ->
                FileOutputStream(target).use { sink -> source.copyTo(sink) }
            }
            normalize(target)
            target
        }
    }

    /** Re-saves the picture when it is rotated through a camera tag, or is not a JPEG at all. */
    private fun normalize(file: File) {
        val degrees = rotationOf(file)
        val isJpeg = JpegReader.read(file.readBytes()) != null
        if (degrees == 0 && isJpeg) return

        val original = BitmapFactory.decodeFile(file.path)
            ?: throw IOException("The scanned page is not a picture")
        val upright = if (degrees == 0) {
            original
        } else {
            val matrix = Matrix()
            matrix.postRotate(degrees.toFloat())
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        }
        FileOutputStream(file).use { sink ->
            upright.compress(Bitmap.CompressFormat.JPEG, 95, sink)
        }
    }

    private fun rotationOf(file: File): Int {
        val orientation = try {
            ExifInterface(file.path)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }
}
