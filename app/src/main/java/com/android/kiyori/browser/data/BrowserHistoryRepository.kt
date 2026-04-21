package com.android.kiyori.browser.data

import android.content.Context
import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class BrowserHistoryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val sharedPreferences = appContext.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )
    private val previewDir = File(appContext.filesDir, PREVIEW_DIR_NAME).apply {
        if (!exists()) {
            mkdirs()
        }
    }

    data class HistoryItem(
        val id: Long,
        val title: String,
        val url: String,
        val previewPath: String,
        val createdAt: Long
    )

    fun getHistory(): List<HistoryItem> {
        val raw = sharedPreferences.getString(KEY_HISTORY_LIST, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        HistoryItem(
                            id = item.optLong("id"),
                            title = item.optString("title"),
                            url = item.optString("url"),
                            previewPath = item.optString("previewPath"),
                            createdAt = item.optLong("createdAt")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun upsertHistory(
        title: String,
        url: String,
        previewBitmap: Bitmap?
    ) {
        if (url.isBlank()) {
            return
        }

        val normalizedUrl = url.trim()
        val currentItems = getHistory().toMutableList()
        val existing = currentItems.firstOrNull { it.url == normalizedUrl }
        currentItems.removeAll { it.url == normalizedUrl }

        val previewPath = when {
            previewBitmap != null -> savePreview(previewBitmap)
            existing?.previewPath?.isNotBlank() == true -> existing.previewPath
            else -> ""
        }

        if (existing != null && existing.previewPath.isNotBlank() && existing.previewPath != previewPath) {
            deletePreviewFile(existing.previewPath)
        }

        currentItems.add(
            0,
            HistoryItem(
                id = System.currentTimeMillis(),
                title = title.trim().ifBlank { normalizedUrl.substringAfter("://").substringBefore("/") },
                url = normalizedUrl,
                previewPath = previewPath,
                createdAt = System.currentTimeMillis()
            )
        )

        val trimmedItems = currentItems.take(MAX_HISTORY_ITEMS)
        currentItems.drop(MAX_HISTORY_ITEMS).forEach { deletePreviewFile(it.previewPath) }
        persistHistory(trimmedItems)
    }

    fun deleteHistory(id: Long) {
        val items = getHistory()
        items.firstOrNull { it.id == id }?.let { deletePreviewFile(it.previewPath) }
        persistHistory(items.filterNot { it.id == id })
    }

    fun clearHistory() {
        getHistory().forEach { deletePreviewFile(it.previewPath) }
        sharedPreferences.edit().remove(KEY_HISTORY_LIST).apply()
        previewDir.listFiles()?.forEach { file ->
            runCatching { file.delete() }
        }
    }

    private fun persistHistory(items: List<HistoryItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("url", item.url)
                    put("previewPath", item.previewPath)
                    put("createdAt", item.createdAt)
                }
            )
        }
        sharedPreferences.edit().putString(KEY_HISTORY_LIST, array.toString()).apply()
    }

    private fun savePreview(bitmap: Bitmap): String {
        val previewFile = File(previewDir, "preview_${System.currentTimeMillis()}.jpg")
        FileOutputStream(previewFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, PREVIEW_QUALITY, output)
        }
        return previewFile.absolutePath
    }

    private fun deletePreviewFile(path: String) {
        if (path.isBlank()) {
            return
        }
        runCatching {
            File(path).delete()
        }
    }

    companion object {
        private const val PREF_NAME = "browser_history"
        private const val KEY_HISTORY_LIST = "browser_history_list"
        private const val PREVIEW_DIR_NAME = "browser_history_previews"
        private const val MAX_HISTORY_ITEMS = 30
        private const val PREVIEW_QUALITY = 82
    }
}

