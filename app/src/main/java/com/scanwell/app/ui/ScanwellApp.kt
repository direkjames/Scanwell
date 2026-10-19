package com.scanwell.app.ui

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.scanwell.app.MainViewModel
import com.scanwell.app.ScanTarget
import com.scanwell.app.Screen
import com.scanwell.app.UiEvent
import com.scanwell.app.pdf.ExportQuality
import com.scanwell.app.pdf.Protection
import kotlinx.coroutines.launch

/**
 * The top of the user interface. It looks at which screen the view model wants
 * and shows it, and it handles messages and the phone's Back button.
 */
@Composable
fun ScanwellApp(
    viewModel: MainViewModel,
    onScan: (ScanTarget) -> Unit,
    onSavePdf: (String, ExportQuality, Protection?) -> Unit,
    onPickPdf: () -> Unit,
    onSaveToolResult: (String) -> Unit,
) {
    val documents by viewModel.documents.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val readingText by viewModel.readingText.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Listen for one-off events from the view model.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Message -> launch { snackbar.showSnackbar(event.text) }
                is UiEvent.Launch -> try {
                    context.startActivity(event.intent)
                } catch (e: ActivityNotFoundException) {
                    launch { snackbar.showSnackbar("No app on this phone can do that.") }
                }
            }
        }
    }

    val screen = viewModel.screen
    BackHandler(enabled = screen != Screen.Library) { viewModel.back() }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AnimatedContent(
            targetState = screen,
            // Moving between pages of one document does not fade; only changing screens does.
            contentKey = { it::class },
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "screen",
        ) { current ->
            when (current) {
                Screen.Library -> LibraryScreen(
                    viewModel = viewModel,
                    documents = documents,
                    snackbar = snackbar,
                    onScan = { onScan(ScanTarget.NewDocument) },
                    onScanIdCard = { onScan(ScanTarget.IdCard) },
                )

                is Screen.Document -> {
                    val document = documents.find { it.id == current.id }
                    if (document != null) {
                        DocumentScreen(
                            viewModel = viewModel,
                            document = document,
                            allDocuments = documents,
                            readingText = document.id in readingText,
                            snackbar = snackbar,
                            onAddPages = { onScan(ScanTarget.AddTo(document.id)) },
                            onSavePdf = { quality, protection -> onSavePdf(document.id, quality, protection) },
                        )
                    }
                }

                is Screen.Tool -> ToolsScreen(
                    viewModel = viewModel,
                    snackbar = snackbar,
                    onPickPdf = onPickPdf,
                    onSaveResult = onSaveToolResult,
                )

                is Screen.Page -> {
                    val document = documents.find { it.id == current.id }
                    if (document != null && current.index in document.pages.indices) {
                        PageViewerScreen(
                            viewModel = viewModel,
                            document = document,
                            index = current.index,
                        )
                    }
                }
            }
        }

        if (busy) BusyOverlay()
    }
}

/** Dims the screen and shows a spinner while slow work runs. It also swallows taps. */
@Composable
private fun BusyOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(28.dp),
        ) {
            CircularProgressIndicator()
        }
    }
}
