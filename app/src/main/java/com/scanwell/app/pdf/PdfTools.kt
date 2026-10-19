package com.scanwell.app.pdf

import android.content.Context
import android.graphics.Bitmap
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.File
import java.io.IOException
import java.security.SecureRandom
import kotlinx.coroutines.CancellationException

/** What a PDF file looks like before any work is done on it. */
data class PdfInfo(
    val pageCount: Int,
    /** True when the file needs a password to open. */
    val needsPassword: Boolean,
)

/** What a finished compression produced. */
data class CompressionResult(
    val file: File,
    val beforeBytes: Long,
    val afterBytes: Long,
    val picturesShrunk: Int,
)

/** A problem with a PDF that the person can understand. [message] is safe to show on screen. */
class PdfException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Password protection and compression for PDF files, done entirely on the phone with PDFBox.
 *
 * Every function reads [input] and writes a brand new [output] file. The input is never
 * changed. Output is written to a ".part" file first and renamed at the end, so a failure or
 * a full disk never leaves a half-written PDF behind. Passwords are used and forgotten: they
 * are never stored and never logged.
 *
 * All functions read and write files, so call them from a background thread.
 */
class PdfTools(context: Context) {

    private val appContext = context.applicationContext

    /** Tells PDFBox where the phone's app assets are. Loads a few small tables; done once. */
    private fun prepare() {
        synchronized(initLock) {
            if (!initialised) {
                PDFBoxResourceLoader.init(appContext)
                initialised = true
            }
        }
    }

    /** Looks at a PDF without changing it. Throws [PdfException] when it is not a usable PDF. */
    fun inspect(input: File): PdfInfo {
        prepare()
        return try {
            open(input, "").use { PdfInfo(it.numberOfPages, false) }
        } catch (e: InvalidPasswordException) {
            PdfInfo(0, true)
        } catch (e: IOException) {
            throw unreadable(e)
        } catch (e: RuntimeException) {
            throw unreadable(e)
        }
    }

    /** Checks a password and returns the page count. Throws [PdfException] if it is wrong. */
    fun verifyPassword(input: File, password: String): PdfInfo {
        prepare()
        return guard {
            open(input, password).use { PdfInfo(it.numberOfPages, true) }
        }
    }

    /**
     * Writes a copy of [input] that asks for a password to open, with AES-256 encryption.
     * [openPassword] is only needed when [input] is already protected.
     */
    fun protect(input: File, output: File, protection: Protection, openPassword: String? = null) {
        prepare()
        val owner = protection.ownerPassword?.takeIf { protection.restrict && it.isNotEmpty() }
            ?: randomPassword()
        val permission = AccessPermission()
        if (protection.restrict) {
            permission.setCanModify(false)
            permission.setCanModifyAnnotations(false)
            permission.setCanExtractContent(false)
            permission.setCanAssembleDocument(false)
            permission.setCanFillInForm(false)
        }
        // A file that already has a password is first unlocked into a temporary copy,
        // so the new password replaces the old one instead of being stacked on top of it.
        var source = input
        var unlocked: File? = null
        if (!openPassword.isNullOrEmpty()) {
            unlocked = File(output.path + ".unlocked")
            unlock(input, unlocked, openPassword)
            source = unlocked
        }
        try {
            writeAtomically(output) { target ->
                guard {
                    open(source, "").use { document ->
                        val policy = StandardProtectionPolicy(owner, protection.userPassword, permission)
                        policy.encryptionKeyLength = 256
                        policy.isPreferAES = true
                        document.protect(policy)
                        document.save(target)
                    }
                }
            }
        } finally {
            unlocked?.delete()
        }
    }

    /** Writes a copy of [input] with its password removed. [password] must open the file. */
    fun unlock(input: File, output: File, password: String) {
        prepare()
        writeAtomically(output) { target ->
            guard {
                open(input, password).use { document ->
                    document.isAllSecurityToBeRemoved = true
                    document.save(target)
                }
            }
        }
    }

    /**
     * Writes a smaller copy of [input] by shrinking the pictures inside it.
     * A password-protected file stays protected with the same password (editing limits are not kept). When the copy would not be smaller, [output]
     * is not created and null is returned, so the caller can say "already as small as it gets".
     *
     * @param onProgress called with 0..1 as pages are finished.
     * @param isCancelled polled between pictures; the work stops early when it returns true.
     */
    fun compress(
        input: File,
        output: File,
        level: CompressionLevel,
        password: String? = null,
        onProgress: (Float) -> Unit = {},
        isCancelled: () -> Boolean = { false },
    ): CompressionResult? {
        prepare()
        val before = input.length()
        var shrunk = 0
        // PDFBox cannot save a password-protected file back with its old protection, so the
        // smaller copy is written unprotected first and then locked again with the same password.
        val plain = File(output.path + ".plain")
        try {
            writeAtomically(plain) { target ->
                guard {
                    open(input, password.orEmpty()).use { document ->
                        if (document.isEncrypted) document.isAllSecurityToBeRemoved = true
                        val replaced = HashMap<Any, PDImageXObject>()
                        val pageCount = document.numberOfPages.coerceAtLeast(1)
                        var done = 0
                        for (page in document.pages) {
                            if (isCancelled()) throw CancellationException("Compression cancelled")
                            shrunk += shrinkPictures(document, page.resources, level, replaced, 0, isCancelled)
                            if (level.stripExtras) page.cosObject.removeItem(COSName.THUMB)
                            done++
                            onProgress(done.toFloat() / pageCount)
                        }
                        if (level.stripExtras) document.documentCatalog.metadata = null
                        document.save(target)
                    }
                }
            }
            if (password.isNullOrEmpty()) {
                if (!plain.renameTo(output)) {
                    plain.copyTo(output, overwrite = true)
                }
            } else {
                protect(plain, output, Protection(password, null, false))
            }
        } catch (e: Throwable) {
            output.delete()
            throw e
        } finally {
            plain.delete()
        }
        val after = output.length()
        if (after <= 0L || after >= before) {
            output.delete()
            return null
        }
        return CompressionResult(output, before, after, shrunk)
    }

    // ---------- Internals ----------

    /** Opens a PDF using a temporary file for the heavy parts, so big files do not exhaust memory. */
    private fun open(file: File, password: String): PDDocument =
        PDDocument.load(file, password, MemoryUsageSetting.setupTempFileOnly())

    /**
     * Shrinks the pictures in [resources] (and in the forms inside it) and returns how many
     * were replaced. A picture is replaced only when the new one is really smaller.
     */
    private fun shrinkPictures(
        document: PDDocument,
        resources: PDResources?,
        level: CompressionLevel,
        replaced: MutableMap<Any, PDImageXObject>,
        depth: Int,
        isCancelled: () -> Boolean,
    ): Int {
        if (resources == null || depth > MAX_FORM_DEPTH) return 0
        var count = 0
        for (name in resources.xObjectNames.toList()) {
            if (isCancelled()) throw CancellationException("Compression cancelled")
            val xObject = try {
                resources.getXObject(name)
            } catch (e: IOException) {
                continue // A damaged picture is left exactly as it was.
            }
            when (xObject) {
                is PDImageXObject -> {
                    val key: Any = xObject.cosObject
                    val known = replaced[key]
                    if (known != null) {
                        resources.put(name, known) // The same picture is used in several places.
                        count++
                    } else {
                        val smaller = smallerCopy(document, xObject, level)
                        if (smaller != null) {
                            replaced[key] = smaller
                            resources.put(name, smaller)
                            count++
                        }
                    }
                }
                is PDFormXObject ->
                    count += shrinkPictures(document, xObject.resources, level, replaced, depth + 1, isCancelled)
                else -> Unit
            }
        }
        return count
    }

    /** A smaller JPEG version of [image], or null when it should be left alone. */
    private fun smallerCopy(document: PDDocument, image: PDImageXObject, level: CompressionLevel): PDImageXObject? {
        val dictionary = image.cosObject
        // Masks and see-through pictures would lose their shape as a flat JPEG.
        if (image.isStencil || dictionary.containsKey(COSName.SMASK) || dictionary.containsKey(COSName.MASK)) return null
        val width = image.width
        val height = image.height
        // Tiny pictures (icons, lines) gain nothing; huge ones could run the phone out of memory.
        if (width * height.toLong() < MIN_PIXELS || width * height.toLong() > MAX_PIXELS) return null

        var decoded: Bitmap? = null
        var scaled: Bitmap? = null
        try {
            decoded = image.image ?: return null
            val (targetWidth, targetHeight) = level.targetSize(decoded.width, decoded.height)
            scaled = if (targetWidth != decoded.width || targetHeight != decoded.height) {
                Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true)
            } else {
                decoded
            }
            val candidate = JPEGFactory.createFromImage(document, scaled, level.jpegQuality / 100f)
            return if (rawSize(candidate) < rawSize(image)) candidate else null
        } catch (e: OutOfMemoryError) {
            return null // Not enough memory for this picture: keep it as it is and carry on.
        } catch (e: IOException) {
            return null
        } catch (e: RuntimeException) {
            return null
        } finally {
            if (scaled != null && scaled !== decoded) scaled.recycle()
            decoded?.recycle()
        }
    }

    /** Number of stored (still compressed) bytes in a picture. */
    private fun rawSize(image: PDImageXObject): Long {
        var total = 0L
        val buffer = ByteArray(16 * 1024)
        image.cosObject.createRawInputStream().use { stream ->
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
            }
        }
        return total
    }

    /** Runs [work] against a ".part" file, then renames it into place. Cleans up on failure. */
    private fun writeAtomically(output: File, work: (File) -> Unit) {
        output.parentFile?.mkdirs()
        val part = File(output.path + ".part")
        part.delete()
        try {
            work(part)
            if (!part.renameTo(output)) {
                part.copyTo(output, overwrite = true)
                part.delete()
            }
        } catch (e: Throwable) {
            part.delete()
            output.delete()
            throw e
        }
    }

    /** Turns the many ways PDFBox can fail into one message a person can read. */
    private inline fun <T> guard(work: () -> T): T {
        try {
            return work()
        } catch (e: CancellationException) {
            throw e
        } catch (e: PdfException) {
            throw e
        } catch (e: InvalidPasswordException) {
            throw PdfException("That password is not right.", e)
        } catch (e: OutOfMemoryError) {
            throw PdfException("This PDF is too large to process on this phone.", e)
        } catch (e: IOException) {
            val full = e.message?.contains("ENOSPC", ignoreCase = true) == true ||
                e.message?.contains("No space left", ignoreCase = true) == true
            throw if (full) PdfException("The phone is out of storage space.", e) else unreadable(e)
        } catch (e: RuntimeException) {
            throw unreadable(e)
        }
    }

    private fun unreadable(cause: Throwable) =
        PdfException("This file could not be read. It may be damaged or not a real PDF.", cause)

    private fun randomPassword(): String {
        val random = SecureRandom()
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
        return String(CharArray(32) { alphabet[random.nextInt(alphabet.length)] })
    }

    private companion object {
        val initLock = Any()
        var initialised = false

        const val MAX_FORM_DEPTH = 4
        const val MIN_PIXELS = 160L * 160L
        const val MAX_PIXELS = 48_000_000L
    }
}
