package com.scanwell.app.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.scanwell.app.data.ScanDocument
import com.scanwell.app.data.ScanRepository
import com.scanwell.app.pdf.ExportQuality
import com.scanwell.app.pdf.PdfWriter
import com.scanwell.app.util.Bitmaps
import com.scanwell.app.util.Names
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/** Builds PDF files and the "share" screens that send scans to other apps. */
class Exporter(private val context: Context, private val repository: ScanRepository) {

    /** Writes the whole document as a PDF into [out]. */
    fun writePdf(document: ScanDocument, quality: ExportQuality, out: OutputStream) {
        val pages = document.pages.map { page ->
            val file = repository.pageFile(document.id, page.fileName)
            val loader: () -> ByteArray = { jpegBytes(file, quality) }
            loader
        }
        PdfWriter.write(out, pages, document.title)
    }

    /** Makes a PDF in the cache and returns a share screen for it. */
    fun sharePdf(document: ScanDocument, quality: ExportQuality): Intent {
        val file = File(emptyShareFolder(), Names.safeFileName(document.title) + ".pdf")
        FileOutputStream(file).use { out -> writePdf(document, quality, out) }
        return shareFiles(listOf(file), "application/pdf", document.title)
    }

    /** Returns a share screen for a PDF that already exists. The original file is copied, not moved. */
    fun sharePdfFile(source: File, title: String): Intent {
        val file = File(emptyShareFolder(), Names.safeFileName(title) + ".pdf")
        source.copyTo(file, overwrite = true)
        return shareFiles(listOf(file), "application/pdf", title)
    }

    /** Copies every page as a separate JPEG and returns a share screen for them. */
    fun shareImages(document: ScanDocument): Intent {
        val folder = emptyShareFolder()
        val base = Names.safeFileName(document.title)
        val files = document.pages.mapIndexed { index, page ->
            val target = File(folder, "$base - page ${index + 1}.jpg")
            repository.pageFile(document.id, page.fileName).copyTo(target, overwrite = true)
            target
        }
        return shareFiles(files, "image/jpeg", document.title)
    }

    fun shareText(title: String, text: String): Intent {
        val send = Intent(Intent.ACTION_SEND)
        send.type = "text/plain"
        send.putExtra(Intent.EXTRA_SUBJECT, title)
        send.putExtra(Intent.EXTRA_TEXT, text)
        return Intent.createChooser(send, "Share text")
    }

    /** The JPEG bytes of one page, shrunk according to [quality]. */
    private fun jpegBytes(file: File, quality: ExportQuality): ByteArray {
        if (quality == ExportQuality.ORIGINAL) return file.readBytes()
        val loaded = Bitmaps.decodeSampled(file, quality.maxSide) ?: return file.readBytes()
        val scaled = Bitmaps.scaleToMaxSide(loaded, quality.maxSide)
        val bytes = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality.jpegQuality, bytes)
        if (scaled !== loaded) scaled.recycle()
        loaded.recycle()
        return bytes.toByteArray()
    }

    private fun shareFiles(files: List<File>, mimeType: String, title: String): Intent {
        val authority = context.packageName + ".fileprovider"
        val uris = files.map { file -> FileProvider.getUriForFile(context, authority, file) }

        val send: Intent
        if (uris.size == 1) {
            send = Intent(Intent.ACTION_SEND)
            send.putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            send = Intent(Intent.ACTION_SEND_MULTIPLE)
            send.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
        }
        send.type = mimeType
        send.putExtra(Intent.EXTRA_SUBJECT, title)

        // The clip data and the flag together give the receiving app permission to read the files.
        val clip = ClipData.newUri(context.contentResolver, title, uris[0])
        for (uri in uris.drop(1)) clip.addItem(ClipData.Item(uri))
        send.clipData = clip
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        return Intent.createChooser(send, "Share")
    }

    /** cache/shared/ is the only folder other apps may read from (see res/xml/file_paths.xml). */
    private fun emptyShareFolder(): File {
        val folder = File(context.cacheDir, "shared")
        folder.deleteRecursively()
        folder.mkdirs()
        return folder
    }
}
