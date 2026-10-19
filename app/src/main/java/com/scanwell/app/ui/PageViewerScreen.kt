@file:OptIn(ExperimentalMaterial3Api::class)

package com.scanwell.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import com.scanwell.app.MainViewModel
import com.scanwell.app.R
import com.scanwell.app.data.ScanDocument

/** One page shown large. Pinch to zoom, drag to move around, double-tap to zoom in or out. */
@Composable
fun PageViewerScreen(viewModel: MainViewModel, document: ScanDocument, index: Int) {
    val page = document.pages[index]
    val lastIndex = document.pages.size - 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Page ${index + 1} of ${document.pages.size}") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.back() }) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openPage(document.id, index - 1) },
                        enabled = index > 0,
                    ) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Previous page")
                    }
                    IconButton(
                        onClick = { viewModel.openPage(document.id, index + 1) },
                        enabled = index < lastIndex,
                    ) {
                        Icon(painterResource(R.drawable.ic_forward), contentDescription = "Next page")
                    }
                },
            )
        },
    ) { padding ->
        // key(...) throws away the zoom and position whenever a different page is shown.
        key(page.fileName) {
            var zoom by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            var area by remember { mutableStateOf(IntSize.Zero) }

            // Stops the picture from being dragged completely off the screen.
            fun limit(position: Offset, scale: Float): Offset {
                val maxX = area.width * (scale - 1f) / 2f
                val maxY = area.height * (scale - 1f) / 2f
                return Offset(position.x.coerceIn(-maxX, maxX), position.y.coerceIn(-maxY, maxY))
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Black)
                    .clipToBounds()
                    .onSizeChanged { area = it }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            val newZoom = (zoom * gestureZoom).coerceIn(1f, 6f)
                            zoom = newZoom
                            offset = limit(offset + pan, newZoom)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (zoom > 1f) {
                                    zoom = 1f
                                    offset = Offset.Zero
                                } else {
                                    zoom = 2.5f
                                }
                            },
                        )
                    },
            ) {
                PageImage(
                    file = viewModel.pageFile(document, page),
                    maxSide = 2400,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoom,
                            scaleY = zoom,
                            translationX = offset.x,
                            translationY = offset.y,
                        ),
                    contentDescription = "Page ${index + 1}",
                )
            }
        }
    }
}
