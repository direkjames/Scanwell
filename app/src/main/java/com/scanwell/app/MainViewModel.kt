package com.scanwell.app

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanwell.app.data.ScanDocument
import com.scanwell.app.data.ScanPage
import com.scanwell.app.data.ScanRepository
import com.scanwell.app.pdf.CompressionLevel
import com.scanwell.app.pdf.ExportQuality
import com.scanwell.app.pdf.PdfException
import com.scanwell.app.pdf.PdfTools
import com.scanwell.app.pdf.Protection
import com.scanwell.app.scan.IdCardComposer
import com.scanwell.app.scan.OcrEngine
import com.scanwell.app.scan.PageImporter
import com.scanwell.app.share.Exporter
import com.scanwell.app.util.Names
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Which screen is showing. */
sealed interface Screen {
    data object Library : Screen
    data class Document(val id: String) : Screen
    data class Page(val id: String, val index: Int) : Screen
    data class Tool(val mode: ToolMode) : Screen
}

/** The three things the PDF tools screen can do to a PDF file. */
enum class ToolMode(val title: String, val tagline: String) {
    COMPRESS("Compress PDF", "Make a PDF smaller for email and chat"),
    PROTECT("Protect PDF", "Lock a PDF with a password"),
    UNLOCK("Unlock PDF", "Remove the password from a PDF"),
}

/** A PDF the person picked for a tool. */
data class PickedPdf(
    val name: String,
    val sizeBytes: Long,
    val pageCount: Int,
    /** True while the file still needs its password before anything can be done with it. */
    val locked: Boolean,
    /** True when the file had a password and the person has entered it correctly. */
    val wasProtected: Boolean,
)

/** What a tool produced. The file itself stays in the cache until it is saved or shared. */
sealed interface ToolResult {
    val fileName: String

    data class Compressed(
        override val fileName: String,
        val beforeBytes: Long,
        val afterBytes: Long,
        val picturesShrunk: Int,
    ) : ToolResult

    /** The file could not get any smaller, so nothing was produced. */
    data class AlreadySmall(override val fileName: String, val sizeBytes: Long) : ToolResult

    data class Finished(override val fileName: String, val sizeBytes: Long, val note: String) : ToolResult
}

/**
 * What the person has typed into the PDF tools screen. It lives here, not in the screen, so it
 * survives rotating the phone. It is held in memory only (never saved, never logged) and is
 * emptied whenever the tool is left or a new file is chosen.
 */
class ToolForm {
    var openPassword by mutableStateOf("")
    var password by mutableStateOf("")
    var confirm by mutableStateOf("")
    var ownerPassword by mutableStateOf("")
    var restrict by mutableStateOf(false)
    var level by mutableStateOf(CompressionLevel.MEDIUM)

    fun clear() {
        openPassword = ""
        password = ""
        confirm = ""
        ownerPassword = ""
        restrict = false
    }
}

/** Everything the PDF tools screen needs to draw itself. */
data class ToolUiState(
    val mode: ToolMode,
    val picked: PickedPdf? = null,
    val working: Boolean = false,
    /** 0..1 while compressing, null when the length of the work is unknown. */
    val progress: Float? = null,
    val result: ToolResult? = null,
    val error: String? = null,
)

/** What should happen with the pages when the scanner closes. */
sealed interface ScanTarget {
    data object NewDocument : ScanTarget
    data object IdCard : ScanTarget
    data class AddTo(val documentId: String) : ScanTarget
}

/** One-off things the screen should do: show a short message, or open another app. */
sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
    data class Launch(val intent: Intent) : UiEvent
}

/** Remembers which document is being saved while the "choose where to save" screen is open. */
data class SaveRequest(
    val documentId: String,
    val quality: ExportQuality,
    val protection: Protection? = null,
)

/**
 * The brain of the app. It owns the list of scans and does the slow work
 * (saving files, building PDFs, reading text) on background threads.
 * The screens only show what is in here and call these functions when the user taps something.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app: Application = application
    private val repository = ScanRepository(File(application.filesDir, "scans"))
    private val exporter = Exporter(application, repository)
    private val ocr = OcrEngine(application)
    private val pdfTools = PdfTools(application)

    /** Where tool work files live. Wiped on start, on leaving the tool, and when the app closes. */
    private val toolFolder = File(application.cacheDir, "tools")

    private val _documents = MutableStateFlow<List<ScanDocument>>(emptyList())
    val documents: StateFlow<List<ScanDocument>> = _documents

    /** True while slow work is running; the screen shows a spinner. */
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    /** IDs of the documents whose text is being read right now. */
    private val _readingText = MutableStateFlow<Set<String>>(emptySet())
    val readingText: StateFlow<Set<String>> = _readingText

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    var screen: Screen by mutableStateOf(Screen.Library)
        private set

    val toolForm = ToolForm()

    private val _tool = MutableStateFlow(ToolUiState(ToolMode.COMPRESS))
    val tool: StateFlow<ToolUiState> = _tool

    /** The password that opens the picked PDF. Kept in memory only, never saved or logged. */
    private var openPassword: String? = null
    private var toolJob: Job? = null

    var pendingScan: ScanTarget = ScanTarget.NewDocument
    var pendingSave: SaveRequest? = null

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { toolFolder.deleteRecursively() }
            refresh()
        }
    }

    override fun onCleared() {
        openPassword = null
        toolFolder.deleteRecursively()
    }

    // ---------- Moving between screens ----------

    fun openDocument(id: String) {
        screen = Screen.Document(id)
    }

    fun openPage(id: String, index: Int) {
        screen = Screen.Page(id, index)
    }

    fun back() {
        val current = screen
        screen = when (current) {
            is Screen.Page -> Screen.Document(current.id)
            is Screen.Document -> Screen.Library
            is Screen.Tool -> {
                resetTool()
                Screen.Library
            }
            Screen.Library -> Screen.Library
        }
    }

    fun findDocument(id: String): ScanDocument? = _documents.value.find { it.id == id }

    fun pageFile(document: ScanDocument, page: ScanPage): File =
        repository.pageFile(document.id, page.fileName)

    fun showMessage(text: String) {
        _events.trySend(UiEvent.Message(text))
    }

    // ---------- Scanning ----------

    /** Called when the scanner closes with pictures. */
    fun onScanFinished(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val target = pendingScan
        viewModelScope.launch {
            _busy.value = true
            try {
                val documentId = withContext(Dispatchers.IO) { savePages(target, uris) }
                refresh()
                if (findDocument(documentId) != null) {
                    screen = Screen.Document(documentId)
                    readText(documentId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage("Could not save the scan. ${e.message ?: ""}".trim())
            } finally {
                _busy.value = false
            }
        }
    }

    private fun savePages(target: ScanTarget, uris: List<Uri>): String {
        val files = PageImporter.importAll(app, uris)
        val now = System.currentTimeMillis()
        return when (target) {
            ScanTarget.NewDocument ->
                repository.createDocument(Names.defaultTitle(now), "", files, now).id

            ScanTarget.IdCard -> {
                val page = File(app.cacheDir, "idcard_${System.nanoTime()}.jpg")
                IdCardComposer.compose(files[0], files.getOrNull(1), page)
                files.forEach { it.delete() }
                repository.createDocument(Names.defaultTitle(now, "ID card"), "", listOf(page), now).id
            }

            is ScanTarget.AddTo -> {
                repository.addPages(target.documentId, files)
                    ?: throw IOException("That document no longer exists.")
                target.documentId
            }
        }
    }

    // ---------- Reading text (OCR) ----------

    /** Reads the text of every page that has not been read yet. Safe to call repeatedly. */
    fun readText(documentId: String) {
        if (documentId in _readingText.value) return
        _readingText.value = _readingText.value + documentId
        viewModelScope.launch {
            try {
                val document = withContext(Dispatchers.IO) { repository.load(documentId) }
                if (document != null) {
                    for (page in document.pages) {
                        if (page.text != null) continue
                        val text = ocr.recognize(repository.pageFile(documentId, page.fileName))
                        withContext(Dispatchers.IO) {
                            repository.setPageText(documentId, page.fileName, text)
                        }
                        refresh()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage("The text reader is not ready yet. Go online once, then try again.")
            } finally {
                _readingText.value = _readingText.value - documentId
            }
        }
    }

    // ---------- Editing ----------

    fun rename(id: String, title: String) = edit { repository.rename(id, title) }

    fun setFolder(id: String, folder: String) = edit { repository.setFolder(id, folder.trim()) }

    fun deleteDocument(id: String) = edit { repository.deleteDocument(id) }

    fun deletePage(id: String, index: Int) = edit { repository.deletePage(id, index) }

    fun movePage(id: String, from: Int, to: Int) = edit { repository.movePage(id, from, to) }

    /** Runs a change on a background thread, then reloads the list. */
    private fun edit(change: () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { change() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage("That did not work. ${e.message ?: ""}".trim())
            }
            refresh()
        }
    }

    // ---------- Sharing and saving ----------

    fun sharePdf(id: String, quality: ExportQuality, protection: Protection? = null) = export(id) { document ->
        if (protection == null) {
            exporter.sharePdf(document, quality)
        } else {
            val locked = buildProtectedPdf(document, quality, protection)
            try {
                exporter.sharePdfFile(locked, document.title)
            } finally {
                toolFolder.deleteRecursively()
            }
        }
    }

    fun shareImages(id: String) = export(id) { exporter.shareImages(it) }

    fun shareText(title: String, text: String) {
        _events.trySend(UiEvent.Launch(exporter.shareText(title, text)))
    }

    fun copyText(text: String) {
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Scanned text", text))
        showMessage("Text copied")
    }

    /** Called after the user picked where to save the PDF. */
    fun savePdfTo(destination: Uri) {
        val request = pendingSave ?: return
        pendingSave = null
        val document = findDocument(request.documentId) ?: return
        viewModelScope.launch {
            _busy.value = true
            try {
                withContext(Dispatchers.IO) {
                    val protection = request.protection
                    val out = app.contentResolver.openOutputStream(destination)
                        ?: throw IOException("Could not open that location.")
                    out.use { sink ->
                        if (protection == null) {
                            exporter.writePdf(document, request.quality, sink)
                        } else {
                            try {
                                buildProtectedPdf(document, request.quality, protection).inputStream().use { it.copyTo(sink) }
                            } finally {
                                toolFolder.deleteRecursively()
                            }
                        }
                    }
                }
                showMessage(if (request.protection == null) "PDF saved" else "Protected PDF saved")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage("Could not save the PDF. ${e.message ?: ""}".trim())
            } finally {
                _busy.value = false
            }
        }
    }

    private fun export(id: String, build: (ScanDocument) -> Intent) {
        val document = findDocument(id) ?: return
        viewModelScope.launch {
            _busy.value = true
            try {
                val intent = withContext(Dispatchers.IO) { build(document) }
                _events.send(UiEvent.Launch(intent))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage("Could not prepare the file. ${e.message ?: ""}".trim())
            } finally {
                _busy.value = false
            }
        }
    }

    /** Builds the document as a PDF and locks it. Returns the locked file in the tool folder. */
    private fun buildProtectedPdf(document: ScanDocument, quality: ExportQuality, protection: Protection): File {
        toolFolder.deleteRecursively()
        toolFolder.mkdirs()
        val plain = File(toolFolder, "scan.pdf")
        val locked = File(toolFolder, "scan_locked.pdf")
        FileOutputStream(plain).use { exporter.writePdf(document, quality, it) }
        try {
            pdfTools.protect(plain, locked, protection)
        } finally {
            plain.delete()
        }
        return locked
    }

    // ---------- PDF tools (compress, protect, unlock) ----------

    private val toolInput get() = File(toolFolder, "input.pdf")
    private val toolOutput get() = File(toolFolder, "output.pdf")

    fun openTool(mode: ToolMode) {
        resetTool()
        _tool.value = ToolUiState(mode)
        screen = Screen.Tool(mode)
    }

    /** Throws away the picked file, any result, and the remembered password. */
    fun resetTool() {
        toolJob?.cancel()
        toolJob = null
        openPassword = null
        toolForm.clear()
        toolFolder.deleteRecursively()
        _tool.value = ToolUiState(_tool.value.mode)
    }

    /** Goes back from a result to the options, keeping the same file. */
    fun clearToolResult() {
        toolOutput.delete()
        _tool.value = _tool.value.copy(result = null, error = null)
    }

    /** Starts over inside the tool, to work on another file. */
    fun toolPickAnother() {
        val mode = _tool.value.mode
        resetTool()
        _tool.value = ToolUiState(mode)
    }

    /** Copies the PDF the person picked into the cache (the original is never touched) and looks at it. */
    fun pickPdf(uri: Uri) {
        toolJob?.cancel()
        val mode = _tool.value.mode
        openPassword = null
        toolForm.clear()
        _tool.value = ToolUiState(mode, working = true)
        toolJob = viewModelScope.launch {
            try {
                val picked = withContext(Dispatchers.IO) { importPicked(uri) }
                _tool.value = ToolUiState(mode, picked = picked)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toolFolder.deleteRecursively()
                _tool.value = ToolUiState(mode, error = messageFor(e, "That file could not be opened."))
            }
        }
    }

    private fun importPicked(uri: Uri): PickedPdf {
        var name = "document.pdf"
        var size = -1L
        app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)?.let { name = it }
                if (!cursor.isNull(1)) size = cursor.getLong(1)
            }
        }
        if (size > MAX_PDF_BYTES) throw PdfException("That PDF is larger than ${MAX_PDF_BYTES / (1024 * 1024)} MB, which is too big to process on a phone.")
        // Room for the copy, a working file and the result.
        if (size > 0 && app.cacheDir.usableSpace < size * 3 + FREE_SPACE_MARGIN) {
            throw PdfException("There is not enough free storage on the phone for this PDF.")
        }

        toolFolder.deleteRecursively()
        toolFolder.mkdirs()
        val input = app.contentResolver.openInputStream(uri) ?: throw IOException("Could not open that file.")
        input.use { source ->
            FileOutputStream(toolInput).use { sink ->
                var total = 0L
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = source.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_PDF_BYTES) throw PdfException("That PDF is too big to process on a phone.")
                    sink.write(buffer, 0, read)
                }
            }
        }
        if (toolInput.length() == 0L) throw PdfException("That file is empty.")
        val info = pdfTools.inspect(toolInput)
        return PickedPdf(
            name = Names.safeFileName(name.removeSuffix(".pdf").removeSuffix(".PDF"), "Document"),
            sizeBytes = toolInput.length(),
            pageCount = info.pageCount,
            locked = info.needsPassword,
            wasProtected = info.needsPassword,
        )
    }

    /** Tries a password on the picked PDF. */
    fun enterOpenPassword(password: String) {
        val state = _tool.value
        val picked = state.picked ?: return
        toolJob?.cancel()
        _tool.value = state.copy(working = true, error = null)
        toolJob = viewModelScope.launch {
            try {
                val info = withContext(Dispatchers.IO) { pdfTools.verifyPassword(toolInput, password) }
                openPassword = password
                toolForm.openPassword = ""
                _tool.value = _tool.value.copy(
                    working = false,
                    picked = picked.copy(locked = false, pageCount = info.pageCount),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _tool.value = _tool.value.copy(working = false, error = messageFor(e, "That password is not right."))
            }
        }
    }

    fun runCompress(level: CompressionLevel) {
        val picked = _tool.value.picked ?: return
        runTool { 
            val result = pdfTools.compress(
                input = toolInput,
                output = toolOutput,
                level = level,
                password = openPassword,
                onProgress = { fraction -> _tool.value = _tool.value.copy(progress = fraction) },
                isCancelled = { !isActive },
            )
            val name = picked.name + " (compressed).pdf"
            if (result == null) {
                ToolResult.AlreadySmall(name, picked.sizeBytes)
            } else {
                ToolResult.Compressed(name, result.beforeBytes, result.afterBytes, result.picturesShrunk)
            }
        }
    }

    fun runProtect(protection: Protection) {
        val picked = _tool.value.picked ?: return
        runTool {
            pdfTools.protect(toolInput, toolOutput, protection, openPassword)
            ToolResult.Finished(
                fileName = picked.name + " (protected).pdf",
                sizeBytes = toolOutput.length(),
                note = if (protection.restrict) "Opening needs a password. Editing and copying are locked." else "Opening this PDF now needs the password.",
            )
        }
    }

    fun runUnlock() {
        val picked = _tool.value.picked ?: return
        val password = openPassword ?: ""
        runTool {
            pdfTools.unlock(toolInput, toolOutput, password)
            ToolResult.Finished(
                fileName = picked.name + " (unlocked).pdf",
                sizeBytes = toolOutput.length(),
                note = "The password has been removed. Anyone with the file can open it.",
            )
        }
    }

    private fun runTool(work: suspend kotlinx.coroutines.CoroutineScope.() -> ToolResult) {
        toolJob?.cancel()
        _tool.value = _tool.value.copy(working = true, progress = null, result = null, error = null)
        toolJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { work() }
                toolForm.clear()
                _tool.value = _tool.value.copy(working = false, progress = null, result = result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toolOutput.delete()
                _tool.value = _tool.value.copy(working = false, progress = null, error = messageFor(e, "That did not work."))
            }
        }
    }

    /** The suggested file name for the result, used by the "save" screen. */
    fun toolResultName(): String? = _tool.value.result?.fileName

    /** Copies the finished file to the place the person chose. */
    fun saveToolResultTo(destination: Uri) {
        val name = toolResultName() ?: return
        viewModelScope.launch {
            _busy.value = true
            try {
                withContext(Dispatchers.IO) {
                    val out = app.contentResolver.openOutputStream(destination)
                        ?: throw IOException("Could not open that location.")
                    out.use { sink -> toolOutput.inputStream().use { it.copyTo(sink) } }
                }
                showMessage("Saved $name")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage(messageFor(e, "Could not save the PDF."))
            } finally {
                _busy.value = false
            }
        }
    }

    fun shareToolResult() {
        val name = toolResultName() ?: return
        viewModelScope.launch {
            try {
                val intent = withContext(Dispatchers.IO) {
                    exporter.sharePdfFile(toolOutput, name.removeSuffix(".pdf"))
                }
                _events.send(UiEvent.Launch(intent))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage(messageFor(e, "Could not prepare the file."))
            }
        }
    }

    /** PDF problems already have a readable message; anything else gets the [fallback]. */
    private fun messageFor(error: Exception, fallback: String): String =
        if (error is PdfException) error.message ?: fallback else fallback

    // ---------- Loading ----------

    private suspend fun refresh() {
        val loaded = withContext(Dispatchers.IO) { repository.loadAll() }
        _documents.value = loaded

        // If the document or page on screen was deleted, step back to a screen that still exists.
        val current = screen
        if (current is Screen.Document && loaded.none { it.id == current.id }) {
            screen = Screen.Library
        } else if (current is Screen.Page) {
            val document = loaded.find { it.id == current.id }
            if (document == null) {
                screen = Screen.Library
            } else if (current.index !in document.pages.indices) {
                screen = Screen.Document(current.id)
            }
        }
    }

    private companion object {
        const val MAX_PDF_BYTES = 500L * 1024 * 1024
        const val FREE_SPACE_MARGIN = 32L * 1024 * 1024
    }
}
