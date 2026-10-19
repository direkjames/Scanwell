package com.scanwell.app.ui

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.scanwell.app.util.Bitmaps
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Keeps the 40 most recently shown thumbnails in memory so scrolling the list stays smooth. */
private val thumbnailCache = LruCache<String, ImageBitmap>(40)

/**
 * Shows a scanned page. The picture is loaded on a background thread at roughly
 * [maxSide] pixels; a gray box is shown until it is ready.
 */
@Composable
fun PageImage(
    file: File,
    maxSide: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
) {
    val key = file.path + "@" + maxSide
    val isThumbnail = maxSide <= 512
    val image by produceState<ImageBitmap?>(
        initialValue = if (isThumbnail) thumbnailCache.get(key) else null,
        key1 = key,
    ) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) {
                Bitmaps.decodeSampled(file, maxSide)?.asImageBitmap()
            }
            if (loaded != null && isThumbnail) thumbnailCache.put(key, loaded)
            value = loaded
        }
    }

    val current = image
    if (current != null) {
        Image(
            bitmap = current,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
