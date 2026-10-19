@file:OptIn(ExperimentalMaterial3Api::class)

package com.scanwell.app.ui

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scanwell.app.MainViewModel
import com.scanwell.app.R
import com.scanwell.app.ToolMode
import com.scanwell.app.data.Library
import com.scanwell.app.data.ScanDocument
import com.scanwell.app.ui.theme.ScanwellTheme
import java.io.File

/**
 * The home screen. Scanning comes first (a big Scan card), then the PDF tools, then the
 * person's recent scans. With no scans yet it shows a friendly welcome instead of a blank page.
 */
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    documents: List<ScanDocument>,
    snackbar: SnackbarHostState,
    onScan: () -> Unit,
    onScanIdCard: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var chosenFolder by rememberSaveable { mutableStateOf<String?>(null) }

    // Which document a dialog is open for (null = that dialog is closed).
    var renaming by remember { mutableStateOf<ScanDocument?>(null) }
    var moving by remember { mutableStateOf<ScanDocument?>(null) }
    var deleting by remember { mutableStateOf<ScanDocument?>(null) }

    val folders = remember(documents) { Library.folders(documents) }
    // If the chosen folder became empty and disappeared, fall back to showing everything.
    val folder = chosenFolder?.takeIf { it in folders }
    val visible = remember(documents, query, folder) { Library.filter(documents, query, folder) }
    val filtering = query.isNotBlank() || folder != null
    val listState = rememberLazyListState()
    val showFab by remember { derivedStateOf { listState.firstVisibleItemIndex >= 2 } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
            ) {
                ExtendedFloatingActionButton(
                    onClick = onScan,
                    icon = { Icon(painterResource(R.drawable.ic_scan), contentDescription = null) },
                    text = { Text("Scan") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            // The bottom space keeps the last row clear of the Scan button.
            contentPadding = PaddingValues(bottom = 112.dp),
        ) {
            item(key = "header") {
                HomeHeader(
                    documentCount = documents.size,
                    pageCount = documents.sumOf { it.pages.size },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
            item(key = "hero") {
                ScanHero(
                    onScan = onScan,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            item(key = "tools") {
                QuickTools(
                    onCompress = { viewModel.openTool(ToolMode.COMPRESS) },
                    onProtect = { viewModel.openTool(ToolMode.PROTECT) },
                    onUnlock = { viewModel.openTool(ToolMode.UNLOCK) },
                    onIdCard = onScanIdCard,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }

            if (documents.isEmpty()) {
                item(key = "welcome") {
                    WelcomeShowcase(Modifier.padding(horizontal = 20.dp, vertical = 28.dp))
                }
            } else {
                item(key = "search") {
                    SearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp),
                    )
                }
                if (folders.isNotEmpty()) {
                    item(key = "folders") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 12.dp),
                        ) {
                            item {
                                FilterChip(
                                    selected = folder == null,
                                    onClick = { chosenFolder = null },
                                    label = { Text("All") },
                                    shape = CircleShape,
                                )
                            }
                            items(folders) { name ->
                                FilterChip(
                                    selected = folder == name,
                                    onClick = { chosenFolder = name },
                                    label = { Text(name) },
                                    leadingIcon = {
                                        Icon(painterResource(R.drawable.ic_folder), contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                    shape = CircleShape,
                                )
                            }
                        }
                    }
                }

                if (!filtering) {
                    item(key = "recent-title") {
                        SectionTitle("Recent", Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp))
                    }
                    item(key = "recent") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(documents.take(RECENT_COUNT), key = { it.id }) { document ->
                                RecentCard(
                                    document = document,
                                    thumbnail = document.pages.firstOrNull()?.let { viewModel.pageFile(document, it) },
                                    onOpen = { viewModel.openDocument(document.id) },
                                )
                            }
                        }
                    }
                }

                item(key = "all-title") {
                    SectionTitle(
                        text = if (filtering) "Results" else "All scans",
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp),
                        trailing = {
                            Text(
                                text = if (visible.size == 1) "1 scan" else "${visible.size} scans",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }

                if (visible.isEmpty()) {
                    item(key = "nothing") {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            IconBadge(R.drawable.ic_search, size = 56.dp, shape = CircleShape)
                            Spacer(Modifier.height(12.dp))
                            Text("Nothing matches", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Try a different word. Search looks inside the text of your scans too.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                items(visible, key = { it.id }) { document ->
                    DocumentRow(
                        document = document,
                        thumbnail = document.pages.firstOrNull()?.let { viewModel.pageFile(document, it) },
                        onOpen = { viewModel.openDocument(document.id) },
                        onRename = { renaming = document },
                        onMove = { moving = document },
                        onDelete = { deleting = document },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }

    renaming?.let { document ->
        RenameDialog(
            currentTitle = document.title,
            onDismiss = { renaming = null },
            onConfirm = { title ->
                viewModel.rename(document.id, title)
                renaming = null
            },
        )
    }
    moving?.let { document ->
        FolderDialog(
            folders = folders,
            currentFolder = document.folder,
            onDismiss = { moving = null },
            onConfirm = { name ->
                viewModel.setFolder(document.id, name)
                moving = null
            },
        )
    }
    deleting?.let { document ->
        ConfirmDialog(
            title = "Delete this scan?",
            message = "\"${document.title}\" will be removed from this phone for good.",
            confirmLabel = "Delete",
            onDismiss = { deleting = null },
            onConfirm = {
                viewModel.deleteDocument(document.id)
                deleting = null
            },
        )
    }
}

private const val RECENT_COUNT = 8

/** The logo, the app name and a small summary of what is stored. */
@Composable
private fun HomeHeader(documentCount: Int, pageCount: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colorResource(R.color.ic_launcher_background)),
            contentAlignment = Alignment.Center,
        ) {
            // The app icon artwork, enlarged so the page fills the badge.
            Icon(
                painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.requiredSize(88.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Scanwell", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Private. Offline. No watermark.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (documentCount > 0) {
            val label = (if (documentCount == 1) "1 scan" else "$documentCount scans") +
                " · " + (if (pageCount == 1) "1 page" else "$pageCount pages")
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** The big card at the top: one tap starts scanning. */
@Composable
private fun ScanHero(onScan: () -> Unit, modifier: Modifier = Modifier) {
    val extras = ScanwellTheme.extras
    val shape = MaterialTheme.shapes.extraLarge
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onScan,
        shape = shape,
        container = Color.Transparent,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(extras.heroBrush)
                .padding(24.dp),
        ) {
            // Soft decorative circles give the card depth without needing any image.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .requiredSize(180.dp)
                    .background(
                        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)),
                        CircleShape,
                    ),
            )
            Column {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_scan),
                        contentDescription = null,
                        tint = extras.onHero,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text("Scan a document", style = MaterialTheme.typography.headlineMedium, color = extras.onHero)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Edges found for you. Crisp pages in seconds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extras.onHero.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Start scanning", style = MaterialTheme.typography.labelLarge, color = Color(0xFF3A32C8))
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painterResource(R.drawable.ic_arrow_right),
                        contentDescription = null,
                        tint = Color(0xFF3A32C8),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** A row of shortcuts to the PDF tools and ID card mode. */
@Composable
private fun QuickTools(
    onCompress: () -> Unit,
    onProtect: () -> Unit,
    onUnlock: () -> Unit,
    onIdCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SectionTitle("Tools", Modifier.padding(horizontal = 20.dp, vertical = 0.dp))
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ToolTile(R.drawable.ic_compress, "Compress", "Shrink a PDF", onCompress) }
            item { ToolTile(R.drawable.ic_lock, "Protect", "Add a password", onProtect) }
            item { ToolTile(R.drawable.ic_unlock, "Unlock", "Remove password", onUnlock) }
            item { ToolTile(R.drawable.ic_id_card, "ID card", "Front and back", onIdCard) }
        }
    }
}

@Composable
private fun ToolTile(icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    SoftCard(onClick = onClick, modifier = Modifier.width(132.dp)) {
        Column(Modifier.padding(14.dp)) {
            IconBadge(icon, size = 42.dp)
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search names and text inside scans") },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
    )
}

/** Shown when there are no scans yet: explains the app and what it can do. */
@Composable
private fun WelcomeShowcase(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        SectionTitle("Your scans will appear here")
        Spacer(Modifier.height(4.dp))
        Text(
            "Everything stays on this phone. No account, no ads, no page limit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 8.dp)) {
                FeatureLine(R.drawable.ic_scan, "Automatic edge detection", "Point, tap, and the page is cropped and cleaned up.")
                FeatureLine(R.drawable.ic_text, "Copy the text", "Read words from any scan and search them later.")
                FeatureLine(R.drawable.ic_lock, "Password-protect PDFs", "Lock a file with AES-256 before you send it.")
                FeatureLine(R.drawable.ic_compress, "Shrink big files", "Squeeze a PDF to fit an email or upload limit.")
            }
        }
    }
}

@Composable
private fun FeatureLine(icon: Int, title: String, subtitle: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, size = 40.dp)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A tall card with a big preview of the first page, used in the "Recent" strip. */
@Composable
private fun RecentCard(document: ScanDocument, thumbnail: File?, onOpen: () -> Unit) {
    SoftCard(onClick = onOpen, modifier = Modifier.width(156.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.78f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (thumbnail != null) {
                PageImage(
                    file = thumbnail,
                    maxSide = 480,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            PageCountBadge(
                count = document.pages.size,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
            )
        }
        Column(Modifier.padding(12.dp)) {
            Text(
                document.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                relativeDate(document.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PageCountBadge(count: Int, modifier: Modifier = Modifier) {
    Text(
        text = if (count == 1) "1 page" else "$count pages",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** One line in the list: thumbnail, title, details, and a "more" menu. */
@Composable
private fun DocumentRow(
    document: ScanDocument,
    thumbnail: File?,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }

    SoftCard(onClick = onOpen, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 56.dp, height = 74.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (thumbnail != null) {
                    PageImage(
                        file = thumbnail,
                        maxSide = 320,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = describe(document),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (document.folder.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_folder),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            document.folder,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(painterResource(R.drawable.ic_more), contentDescription = "More options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Move to folder") },
                        onClick = {
                            menuOpen = false
                            onMove()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

/** Example: "3 pages · Today". */
private fun describe(document: ScanDocument): String {
    val count = document.pages.size
    val pages = if (count == 1) "1 page" else "$count pages"
    return "$pages · ${relativeDate(document.createdAt)}"
}

/** "Today", "Yesterday", "3 days ago", or a date once it is older than a week. */
private fun relativeDate(millis: Long): String =
    DateUtils.getRelativeTimeSpanString(
        millis,
        System.currentTimeMillis(),
        DateUtils.DAY_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
