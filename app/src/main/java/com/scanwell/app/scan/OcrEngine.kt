package com.scanwell.app.scan

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Reads the text on a scanned page (OCR = optical character recognition) with Google ML Kit.
 * Everything runs on the phone; no picture is uploaded anywhere.
 */
class OcrEngine(private val context: Context) {

    // Created on first use rather than when the app starts, so a problem with the text
    // reader can only affect reading text, never stop the app from opening.
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    /** Returns the page's text ("" when there is none). Throws if the text reader is not ready. */
    suspend fun recognize(file: File): String = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromFilePath(context, Uri.fromFile(file))
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    if (continuation.isActive) continuation.resume(result.text)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        } catch (error: Exception) {
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }
}
