package com.android.kiyori.browser.data

import android.content.Context
import com.android.kiyori.browser.domain.BrowserBookmarkFolder
import com.android.kiyori.browser.domain.BrowserBookmarkItem
import org.json.JSONArray
import org.json.JSONObject

class BrowserBookmarkRepository(context: Context) {
    private val appContext = context.applicationContext
    private val sharedPreferences = appContext.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    init {
        migrateLegacySeededFolders()
    }

    fun getFolders(): List<BrowserBookmarkFolder> {
        val raw = sharedPreferences.getString(KEY_FOLDERS, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        BrowserBookmarkFolder(
                            id = item.optLong("id"),
                            title = item.optString("title"),
                            parentId = item.optLong("parentId").takeIf { !item.isNull("parentId") },
                            createdAt = item.optLong("createdAt")
                        )
                    )
                }
            }.sortedBy { it.createdAt }
        }.getOrDefault(emptyList())
    }

    fun getBookmarks(): List<BrowserBookmarkItem> {
        val raw = sharedPreferences.getString(KEY_BOOKMARKS, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        BrowserBookmarkItem(
                            id = item.optLong("id"),
                            title = item.optString("title"),
                            url = item.optString("url"),
                            iconUrl = item.optString("iconUrl"),
                            folderId = item.optLong("folderId").takeIf { !item.isNull("folderId") },
                            createdAt = item.optLong("createdAt")
                        )
                    )
                }
            }.sortedByDescending { it.createdAt }
        }.getOrDefault(emptyList())
    }

    fun addFolder(title: String, parentId: Long?): BrowserBookmarkFolder {
        val normalizedTitle = title.trim().ifBlank { "新建文件夹" }
        val folders = getFolders().toMutableList()
        val folder = BrowserBookmarkFolder(
            id = System.currentTimeMillis(),
            title = normalizedTitle,
            parentId = parentId,
            createdAt = System.currentTimeMillis()
        )
        folders.add(folder)
        persistFolders(folders)
        return folder
    }

    fun renameFolder(folderId: Long, title: String) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank()) {
            return
        }
        val updatedFolders = getFolders().map { folder ->
            if (folder.id == folderId) {
                folder.copy(title = normalizedTitle)
            } else {
                folder
            }
        }
        persistFolders(updatedFolders)
    }

    fun moveFolder(folderId: Long, targetParentId: Long?): Boolean {
        if (folderId == targetParentId) {
            return false
        }

        val folders = getFolders()
        val descendantIds = collectDescendantIds(folderId, folders)
        if (targetParentId != null && descendantIds.contains(targetParentId)) {
            return false
        }

        val updatedFolders = folders.map { folder ->
            if (folder.id == folderId) {
                folder.copy(parentId = targetParentId)
            } else {
                folder
            }
        }
        persistFolders(updatedFolders)
        return true
    }

    fun deleteFolder(folderId: Long) {
        val folders = getFolders()
        val descendantIds = collectDescendantIds(folderId, folders) + folderId
        persistFolders(folders.filterNot { descendantIds.contains(it.id) })
        persistBookmarks(getBookmarks().filterNot { descendantIds.contains(it.folderId) })
    }

    fun upsertBookmark(
        title: String,
        url: String,
        iconUrl: String,
        folderId: Long?
    ): BrowserBookmarkItem? {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isBlank()) {
            return null
        }

        val normalizedTitle = title.trim().ifBlank {
            normalizedUrl.substringAfter("://").substringBefore("/").ifBlank { normalizedUrl }
        }
        val normalizedIconUrl = iconUrl.trim()
        val currentBookmarks = getBookmarks().toMutableList()
        val existing = currentBookmarks.firstOrNull { it.url == normalizedUrl && it.folderId == folderId }
        currentBookmarks.removeAll { it.url == normalizedUrl && it.folderId == folderId }

        val bookmark = BrowserBookmarkItem(
            id = existing?.id ?: System.currentTimeMillis(),
            title = normalizedTitle,
            url = normalizedUrl,
            iconUrl = normalizedIconUrl,
            folderId = folderId,
            createdAt = System.currentTimeMillis()
        )
        currentBookmarks.add(0, bookmark)
        persistBookmarks(currentBookmarks)
        return bookmark
    }

    fun deleteBookmark(bookmarkId: Long) {
        val updatedBookmarks = getBookmarks().filterNot { it.id == bookmarkId }
        persistBookmarks(updatedBookmarks)
    }

    private fun migrateLegacySeededFolders() {
        val folders = getFolders()
        if (folders.isEmpty()) {
            return
        }

        val isExactLegacySeed = folders.size == LEGACY_ROOT_FOLDERS.size &&
            folders.all { it.parentId == null } &&
            folders.map { it.title } == LEGACY_ROOT_FOLDERS

        if (!isExactLegacySeed) {
            return
        }

        val migratedBookmarks = getBookmarks().map { bookmark ->
            if (folders.any { it.id == bookmark.folderId }) {
                bookmark.copy(folderId = null)
            } else {
                bookmark
            }
        }
        persistFolders(emptyList())
        persistBookmarks(migratedBookmarks)
    }

    private fun collectDescendantIds(
        folderId: Long,
        folders: List<BrowserBookmarkFolder>
    ): Set<Long> {
        val descendants = mutableSetOf<Long>()
        fun traverse(parentId: Long) {
            folders
                .filter { it.parentId == parentId }
                .forEach { child ->
                    if (descendants.add(child.id)) {
                        traverse(child.id)
                    }
                }
        }
        traverse(folderId)
        return descendants
    }

    private fun persistFolders(folders: List<BrowserBookmarkFolder>) {
        val array = JSONArray()
        folders.forEach { folder ->
            array.put(
                JSONObject().apply {
                    put("id", folder.id)
                    put("title", folder.title)
                    if (folder.parentId == null) {
                        put("parentId", JSONObject.NULL)
                    } else {
                        put("parentId", folder.parentId)
                    }
                    put("createdAt", folder.createdAt)
                }
            )
        }
        sharedPreferences.edit().putString(KEY_FOLDERS, array.toString()).apply()
    }

    private fun persistBookmarks(bookmarks: List<BrowserBookmarkItem>) {
        val array = JSONArray()
        bookmarks.forEach { bookmark ->
            array.put(
                JSONObject().apply {
                    put("id", bookmark.id)
                    put("title", bookmark.title)
                    put("url", bookmark.url)
                    put("iconUrl", bookmark.iconUrl)
                    if (bookmark.folderId == null) {
                        put("folderId", JSONObject.NULL)
                    } else {
                        put("folderId", bookmark.folderId)
                    }
                    put("createdAt", bookmark.createdAt)
                }
            )
        }
        sharedPreferences.edit().putString(KEY_BOOKMARKS, array.toString()).apply()
    }

    companion object {
        private const val PREF_NAME = "browser_bookmarks"
        private const val KEY_FOLDERS = "browser_bookmark_folders"
        private const val KEY_BOOKMARKS = "browser_bookmark_items"

        private val LEGACY_ROOT_FOLDERS = listOf(
            "主页导航",
            "人工智能",
            "动漫影视",
            "小说漫画",
            "成人动漫",
            "成人视频",
            "服务器"
        )
    }
}
