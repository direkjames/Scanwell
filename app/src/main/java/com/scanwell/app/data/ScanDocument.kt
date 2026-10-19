package com.scanwell.app.data

/** One scanned page: a JPEG file plus the text found on it. */
data class ScanPage(
    /** Name of the JPEG inside the document's folder. */
    val fileName: String,
    /** Text read from the page. null means the page has not been read yet. */
    val text: String? = null,
)

/** One scanned document: a title and its pages in order. */
data class ScanDocument(
    val id: String,
    val title: String,
    /** Name of the folder it is filed under. Empty means "no folder". */
    val folder: String,
    /** When it was scanned, in milliseconds since 1970. */
    val createdAt: Long,
    val pages: List<ScanPage>,
)

/** Pure list logic for the library screen. No Android classes, so it is unit tested. */
object Library {

    /** Every folder name in use, sorted A to Z. */
    fun folders(documents: List<ScanDocument>): List<String> =
        documents.map { it.folder }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }

    /**
     * Keeps the documents inside [folder] (null = all) whose title or page text contains [query].
     */
    fun filter(documents: List<ScanDocument>, query: String, folder: String?): List<ScanDocument> {
        val words = query.trim()
        return documents.filter { document ->
            val inFolder = folder == null || document.folder == folder
            val matches = words.isEmpty() ||
                document.title.contains(words, ignoreCase = true) ||
                document.pages.any { page -> page.text?.contains(words, ignoreCase = true) == true }
            inFolder && matches
        }
    }

    /** Returns a copy of [items] with the item at [from] moved to position [to]. */
    fun <T> move(items: List<T>, from: Int, to: Int): List<T> {
        if (from !in items.indices || to !in items.indices || from == to) return items
        val result = items.toMutableList()
        result.add(to, result.removeAt(from))
        return result
    }
}
