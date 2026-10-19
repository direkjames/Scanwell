package com.scanwell.app

import com.scanwell.app.data.Library
import com.scanwell.app.data.ScanDocument
import com.scanwell.app.data.ScanPage
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryTest {

    private val receipt = ScanDocument(
        id = "1", title = "Grocery receipt", folder = "Receipts", createdAt = 3,
        pages = listOf(ScanPage("a.jpg", text = "TOTAL 245.50")),
    )
    private val contract = ScanDocument(
        id = "2", title = "Lease contract", folder = "", createdAt = 2,
        pages = listOf(ScanPage("b.jpg", text = "The tenant agrees"), ScanPage("c.jpg", text = null)),
    )
    private val ticket = ScanDocument(
        id = "3", title = "Bus ticket", folder = "receipts", createdAt = 1,
        pages = listOf(ScanPage("d.jpg")),
    )
    private val all = listOf(receipt, contract, ticket)

    @Test
    fun emptySearchShowsEverything() {
        assertEquals(all, Library.filter(all, query = "  ", folder = null))
    }

    @Test
    fun searchLooksAtTitlesIgnoringCapitals() {
        assertEquals(listOf(contract), Library.filter(all, query = "LEASE", folder = null))
    }

    @Test
    fun searchLooksInsideTheTextOfPages() {
        assertEquals(listOf(receipt), Library.filter(all, query = "245.50", folder = null))
        assertEquals(listOf(contract), Library.filter(all, query = "tenant", folder = null))
    }

    @Test
    fun folderAndSearchWorkTogether() {
        assertEquals(listOf(receipt), Library.filter(all, query = "", folder = "Receipts"))
        assertEquals(emptyList<ScanDocument>(), Library.filter(all, query = "lease", folder = "Receipts"))
    }

    @Test
    fun foldersAreListedOnceAndSorted() {
        assertEquals(listOf("Receipts", "receipts"), Library.folders(all))
        assertEquals(emptyList<String>(), Library.folders(listOf(contract)))
    }

    @Test
    fun movingAPageChangesTheOrder() {
        assertEquals(listOf("b", "c", "a"), Library.move(listOf("a", "b", "c"), from = 0, to = 2))
        assertEquals(listOf("c", "a", "b"), Library.move(listOf("a", "b", "c"), from = 2, to = 0))
    }

    @Test
    fun impossibleMovesChangeNothing() {
        val pages = listOf("a", "b", "c")
        assertEquals(pages, Library.move(pages, from = 0, to = 3))
        assertEquals(pages, Library.move(pages, from = -1, to = 1))
        assertEquals(pages, Library.move(pages, from = 1, to = 1))
    }
}
