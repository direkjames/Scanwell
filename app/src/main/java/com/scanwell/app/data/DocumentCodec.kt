package com.scanwell.app.data

import org.json.JSONArray
import org.json.JSONObject

/** Turns a [ScanDocument] into JSON text for saving, and back again when loading. */
object DocumentCodec {

    fun toJson(document: ScanDocument): String {
        val pages = JSONArray()
        for (page in document.pages) {
            val item = JSONObject()
            item.put("file", page.fileName)
            if (page.text != null) item.put("text", page.text)
            pages.put(item)
        }
        val root = JSONObject()
        root.put("id", document.id)
        root.put("title", document.title)
        root.put("folder", document.folder)
        root.put("createdAt", document.createdAt)
        root.put("pages", pages)
        return root.toString()
    }

    fun fromJson(json: String): ScanDocument {
        val root = JSONObject(json)
        val array = root.getJSONArray("pages")
        val pages = ArrayList<ScanPage>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            pages.add(
                ScanPage(
                    fileName = item.getString("file"),
                    text = if (item.has("text")) item.getString("text") else null,
                ),
            )
        }
        return ScanDocument(
            id = root.getString("id"),
            title = root.getString("title"),
            folder = root.optString("folder", ""),
            createdAt = root.optLong("createdAt", 0L),
            pages = pages,
        )
    }
}
