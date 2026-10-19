package com.scanwell.app.data

import java.io.File
import java.util.UUID

/**
 * Saves and loads scans on the phone's private storage.
 *
 * Layout on disk:
 *   scans/<document id>/meta.json     title, folder, page order, recognized text
 *   scans/<document id>/<page>.jpg    one picture per page
 *
 * Every function here reads or writes files, so call them from a background thread.
 */
class ScanRepository(private val root: File) {

    @Synchronized
    fun loadAll(): List<ScanDocument> {
        val folders = root.listFiles { file -> file.isDirectory } ?: return emptyList()
        return folders.mapNotNull { folder -> read(folder) }
            .sortedByDescending { it.createdAt }
    }

    @Synchronized
    fun load(id: String): ScanDocument? = read(folderOf(id))

    fun pageFile(documentId: String, fileName: String): File = File(folderOf(documentId), fileName)

    /** Creates a document from picture files. The files are moved into the document's folder. */
    @Synchronized
    fun createDocument(title: String, folder: String, pageFiles: List<File>, now: Long): ScanDocument {
        val id = UUID.randomUUID().toString()
        folderOf(id).mkdirs()
        val pages = pageFiles.map { file -> ScanPage(adopt(id, file)) }
        val document = ScanDocument(id, title, folder, now, pages)
        write(document)
        return document
    }

    @Synchronized
    fun addPages(id: String, pageFiles: List<File>): ScanDocument? {
        val document = load(id) ?: return null
        val added = pageFiles.map { file -> ScanPage(adopt(id, file)) }
        return document.copy(pages = document.pages + added).also { write(it) }
    }

    /** Removes one page. Deleting the last page deletes the whole document (returns null). */
    @Synchronized
    fun deletePage(id: String, index: Int): ScanDocument? {
        val document = load(id) ?: return null
        if (index !in document.pages.indices) return document
        if (document.pages.size == 1) {
            deleteDocument(id)
            return null
        }
        val removed = document.pages[index]
        val updated = document.copy(pages = document.pages.filterIndexed { i, _ -> i != index })
        write(updated)
        pageFile(id, removed.fileName).delete()
        return updated
    }

    @Synchronized
    fun movePage(id: String, from: Int, to: Int): ScanDocument? {
        val document = load(id) ?: return null
        return document.copy(pages = Library.move(document.pages, from, to)).also { write(it) }
    }

    @Synchronized
    fun rename(id: String, title: String): ScanDocument? {
        val document = load(id) ?: return null
        return document.copy(title = title).also { write(it) }
    }

    @Synchronized
    fun setFolder(id: String, folder: String): ScanDocument? {
        val document = load(id) ?: return null
        return document.copy(folder = folder).also { write(it) }
    }

    /** Stores the text that was read from one page. */
    @Synchronized
    fun setPageText(id: String, fileName: String, text: String): ScanDocument? {
        val document = load(id) ?: return null
        val pages = document.pages.map { page ->
            if (page.fileName == fileName) page.copy(text = text) else page
        }
        return document.copy(pages = pages).also { write(it) }
    }

    @Synchronized
    fun deleteDocument(id: String) {
        folderOf(id).deleteRecursively()
    }

    private fun folderOf(id: String): File = File(root, id)

    /** Moves a picture into the document's folder under a new unique name and returns that name. */
    private fun adopt(id: String, source: File): String {
        val name = "page_" + UUID.randomUUID().toString() + ".jpg"
        val target = pageFile(id, name)
        if (!source.renameTo(target)) {
            source.copyTo(target, overwrite = true)
            source.delete()
        }
        return name
    }

    private fun read(folder: File): ScanDocument? {
        val meta = File(folder, META_FILE)
        if (!meta.isFile) return null
        return try {
            DocumentCodec.fromJson(meta.readText())
        } catch (e: Exception) {
            null // A damaged entry is skipped instead of crashing the whole library.
        }
    }

    /** Writes to a temporary file first, so a crash halfway can never leave a broken meta.json. */
    private fun write(document: ScanDocument) {
        val folder = folderOf(document.id)
        folder.mkdirs()
        val temporary = File(folder, "$META_FILE.tmp")
        temporary.writeText(DocumentCodec.toJson(document))
        val target = File(folder, META_FILE)
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
    }

    private companion object {
        const val META_FILE = "meta.json"
    }
}
