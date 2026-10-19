@file:OptIn(ExperimentalMaterial3Api::class)

package com.scanwell.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scanwell.app.MainViewModel
import com.scanwell.app.R
import com.scanwell.app.data.Library
import com.scanwell.app.data.ScanDocument
import com.scanwell.app.data.ScanPage
import com.scanwell.app.pdf.ExportQuality
import com.scanwell.app.pdf.Protection
import com.scanwell.app.util.Bitmaps
import java.io.File

/** Which text the text dialog shows: the whole document, or a single page. */
private sealed interface TextRequest {
    data object WholeDocument : TextRequest
    data class OnePage(val index: Int) : TextRequest
}

/** One document: all of its pages in a scrolling list, with share, text and add-page buttons. */
@Composable
fun DocumentScreen(
    viewModel: MainViewModel,
    document: ScanDocument,
    allDocuments: List<ScanDocument>,
    readingText: Boolean,
    snackbar: SnackbarHostState,
    onAddPages: () -> Unit,
    onSavePdf: (ExportQuality, Protection?) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var exportLocked by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showFolders by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var textRequest by remember { mutableStateOf<TextRequest?>(null) }
    var pageToDelete by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { Text(document.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.back() }) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(painterResource(R.drawable.ic_more), contentDescription = "More options")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    menuOpen = false
                                    showRename = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Move to folder") },
                                onClick = {
                                    menuOpen = false
                                    showFolders = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete scan") },
                                onClick = {
                                    menuOpen = false
                                    showDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            // A floating pill instead of a full-width bar, so the pages stay the star of the screen.
            Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    Modifier
                        .shadow(10.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    BarButton(R.drawable.ic_share, "Share") {
                        exportLocked = false
                        showExport = true
                    }
                    BarButton(R.drawable.ic_lock, "Lock") {
                        exportLocked = true
                        showExport = true
                    }
                    BarButton(R.drawable.ic_text, "Text") { textRequest = TextRequest.WholeDocument }
                    BarButton(R.drawable.ic_add_page, "Add") { onAddPages() }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            itemsIndexed(document.pages, key = { _, page -> page.fileName }) { index, page ->
                PageItem(
                    file = viewModel.pageFile(document, page),
                    index = index,
                    pageCount = document.pages.size,
                    onOpen = { viewModel.openPage(document.id, index) },
                    onShowText = { textRequest = TextRequest.OnePage(index) },
                    onMoveUp = { viewModel.movePage(document.id, index, index - 1) },
                    onMoveDown = { viewModel.movePage(document.id, index, index + 1) },
                    onDelete = { pageToDelete = index },
                )
            }
        }
    }

    if (showExport) {
        ExportSheet(
            startLocked = exportLocked,
            onDismiss = { showExport = false },
            onSharePdf = { quality, protection ->
                showExport = false
                viewModel.sharePdf(document.id, quality, protection)
            },
            onSavePdf = { quality, protection ->
                showExport = false
                onSavePdf(quality, protection)
            },
            onShareImages = {
                showExport = false
                viewModel.shareImages(document.id)
            },
        )
    }

    if (showRename) {
        RenameDialog(
            currentTitle = document.title,
            onDismiss = { showRename = false },
            onConfirm = { title ->
                viewModel.rename(document.id, title)
                showRename = false
            },
        )
    }

    if (showFolders) {
        FolderDialog(
            folders = Library.folders(allDocuments),
            currentFolder = document.folder,
            onDismiss = { showFolders = false },
            onConfirm = { name ->
                viewModel.setFolder(document.id, name)
                showFolders = false
            },
        )
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete this scan?",
            message = "\"${document.title}\" will be removed from this phone for good.",
            confirmLabel = "Delete",
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                viewModel.deleteDocument(document.id)
            },
        )
    }

    val deleteIndex = pageToDelete
    if (deleteIndex != null) {
        val lastPage = document.pages.size == 1
        ConfirmDialog(
            title = "Delete page ${deleteIndex + 1}?",
            message = if (lastPage) {
                "This is the only page, so the whole scan will be deleted."
            } else {
                "The page will be removed for good."
            },
            confirmLabel = "Delete",
            onDismiss = { pageToDelete = null },
            onConfirm = {
                pageToDelete = null
                viewModel.deletePage(document.id, deleteIndex)
            },
        )
    }

    val request = textRequest
    if (request != null) {
        val pages: List<ScanPage> = when (request) {
            TextRequest.WholeDocument -> document.pages
            is TextRequest.OnePage -> listOfNotNull(document.pages.getOrNull(request.index))
        }
        val allRead = pages.all { it.text != null }
        val text = if (allRead) joinPageText(pages) else null

        // Start reading as soon as the dialog opens, if any page has not been read yet.
        LaunchedEffect(request) {
            if (!allRead) viewModel.readText(document.id)
        }

        TextDialog(
            title = when (request) {
                TextRequest.WholeDocument -> "Text in this scan"
                is TextRequest.OnePage -> "Text on page ${request.index + 1}"
            },
            text = text,
            reading = readingText,
            onRetry = { viewModel.readText(document.id) },
            onCopy = { viewModel.copyText(it) },
            onShare = { viewModel.shareText(document.title, it) },
            onDismiss = { textRequest = null },
        )
    }
}

/** Joins the text of several pages, leaving out pages where nothing was found. */
private fun joinPageText(pages: List<ScanPage>): String =
    pages.mapNotNull { it.text?.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n\n")

/** One page in the list, with its own small menu. */
@Composable
private fun PageItem(
    file: File,
    index: Int,
    pageCount: Int,
    onOpen: () -> Unit,
    onShowText: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    // Reading the shape first lets the list reserve the right space before the picture loads.
    val shape = remember(file) { Bitmaps.aspectRatio(file) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Page ${index + 1} of $pageCount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(painterResource(R.drawable.ic_more), contentDescription = "Page options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Show text") },
                        onClick = {
                            menuOpen = false
                            onShowText()
                        },
                    )
                    if (index > 0) {
                        DropdownMenuItem(
                            text = { Text("Move up") },
                            onClick = {
                                menuOpen = false
                                onMoveUp()
                            },
                        )
                    }
                    if (index < pageCount - 1) {
                        DropdownMenuItem(
                            text = { Text("Move down") },
                            onClick = {
                                menuOpen = false
                                onMoveDown()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete page") },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
        PageImage(
            file = file,
            maxSide = 1400,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(shape)
                .shadow(6.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .clickable(onClick = onOpen),
            contentDescription = "Page ${index + 1}",
        )
    }
}

/** An icon with a small label under it, used in the floating action bar. */
@Composable
private fun BarButton(iconRes: Int, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
