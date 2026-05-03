package com.android.kiyori.browser.data

import android.content.Context
import com.android.kiyori.browser.domain.BrowserBookmarkFolder
import com.android.kiyori.browser.domain.BrowserBookmarkItem
import org.json.JSONArray
import org.json.JSONException
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
                            createdAt = item.optLong("createdAt"),
                            order = item.optLong("order", item.optLong("createdAt")),
                            secret = item.optBoolean("secret", false)
                        )
                    )
                }
            }.sortedWith(compareBy<BrowserBookmarkFolder> { it.order }.thenBy { it.createdAt })
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
                            createdAt = item.optLong("createdAt"),
                            order = item.optLong("order", item.optLong("createdAt")),
                            secret = item.optBoolean("secret", false)
                        )
                    )
                }
            }.sortedWith(compareBy<BrowserBookmarkItem> { it.order }.thenByDescending { it.createdAt })
        }.getOrDefault(emptyList())
    }

    fun addFolder(title: String, parentId: Long?, secret: Boolean = false): BrowserBookmarkFolder {
        val normalizedTitle = title.trim().ifBlank { "新建文件夹" }
        val folders = getFolders().toMutableList()
        val targetParentId = parentId?.takeIf { candidateId ->
            folders.any { it.id == candidateId && it.secret == secret }
        }
        folders.firstOrNull {
            it.parentId == targetParentId && it.title == normalizedTitle && it.secret == secret
        }?.let {
            return it
        }
        val now = System.currentTimeMillis()
        val folder = BrowserBookmarkFolder(
            id = nextStorageId(),
            title = normalizedTitle,
            parentId = targetParentId,
            createdAt = now,
            order = nextFolderOrder(targetParentId, secret),
            secret = secret
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

        val folderToMove = folders.firstOrNull { it.id == folderId } ?: return false
        if (targetParentId != null) {
            val targetParent = folders.firstOrNull { it.id == targetParentId } ?: return false
            if (targetParent.secret != folderToMove.secret) {
                return false
            }
        }
        val targetOrder = if (folderToMove.parentId == targetParentId) {
            folderToMove.order
        } else {
            (folders
                .filter { it.parentId == targetParentId && it.id != folderId && it.secret == folderToMove.secret }
                .maxOfOrNull { it.order } ?: -1L) + 1L
        }

        val updatedFolders = folders.map { folder ->
            if (folder.id == folderId) {
                folder.copy(parentId = targetParentId, order = targetOrder)
            } else {
                folder
            }
        }
        persistFolders(updatedFolders)
        return true
    }

    fun deleteFolder(folderId: Long, secret: Boolean? = null) {
        val folders = getFolders()
        val descendantIds = collectDescendantIds(folderId, folders) + folderId
        val remainingBookmarks = getBookmarks().filterNot { bookmark ->
            descendantIds.contains(bookmark.folderId) &&
                (secret == null || bookmark.secret == secret)
        }
        val preservedFolderIds = collectReferencedFolderTree(
            bookmarks = remainingBookmarks,
            folders = folders,
            allowedFolderIds = descendantIds
        ).toMutableSet()
        if (secret != null) {
            folders
                .filter { descendantIds.contains(it.id) && it.secret != secret }
                .forEach { folder ->
                    preservedFolderIds += collectFolderAndAncestors(
                        folderId = folder.id,
                        folders = folders,
                        allowedFolderIds = descendantIds
                    )
                }
        }
        persistFolders(
            folders
                .filterNot { folder ->
                    descendantIds.contains(folder.id) && !preservedFolderIds.contains(folder.id)
                }
                .map { folder ->
                    if (secret != null && descendantIds.contains(folder.id) && folder.secret == secret) {
                        folder.copy(secret = !secret)
                    } else {
                        folder
                    }
                }
        )
        persistBookmarks(remainingBookmarks)
    }

    fun upsertBookmark(
        title: String,
        url: String,
        iconUrl: String,
        folderId: Long?,
        secret: Boolean = false
    ): BrowserBookmarkItem? {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isBlank()) {
            return null
        }
        val targetFolderId = folderId?.takeIf { candidateId ->
            getFolders().any { it.id == candidateId && it.secret == secret }
        }

        val normalizedTitle = title.trim().ifBlank {
            normalizedUrl.substringAfter("://").substringBefore("/").ifBlank { normalizedUrl }
        }
        val normalizedIconUrl = iconUrl.trim()
        val now = System.currentTimeMillis()
        val currentBookmarks = getBookmarks().toMutableList()
        val existing = currentBookmarks.firstOrNull {
            it.url == normalizedUrl && it.folderId == targetFolderId && it.secret == secret
        }
        currentBookmarks.removeAll {
            it.url == normalizedUrl && it.folderId == targetFolderId && it.secret == secret
        }

        val bookmark = BrowserBookmarkItem(
            id = existing?.id ?: nextStorageId(),
            title = normalizedTitle,
            url = normalizedUrl,
            iconUrl = normalizedIconUrl,
            folderId = targetFolderId,
            createdAt = now,
            order = existing?.order ?: nextBookmarkOrder(targetFolderId, secret),
            secret = secret
        )
        currentBookmarks.add(bookmark)
        persistBookmarks(currentBookmarks)
        return bookmark
    }

    fun updateBookmark(
        bookmarkId: Long,
        title: String,
        url: String,
        iconUrl: String,
        folderId: Long?
    ): BrowserBookmarkItem? {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isBlank()) {
            return null
        }
        val currentBookmarks = getBookmarks()
        val original = currentBookmarks.firstOrNull { it.id == bookmarkId } ?: return null
        val targetFolderId = folderId?.takeIf { candidateId ->
            getFolders().any { it.id == candidateId && it.secret == original.secret }
        }
        var updated: BrowserBookmarkItem? = null
        val nextBookmarks = currentBookmarks.mapNotNull { bookmark ->
            if (bookmark.id == bookmarkId) {
                val target = bookmark.copy(
                    title = title.trim().ifBlank {
                        normalizedUrl.substringAfter("://").substringBefore("/").ifBlank { normalizedUrl }
                    },
                    url = normalizedUrl,
                    iconUrl = iconUrl.trim(),
                    folderId = targetFolderId
                )
                updated = target
                target
            } else if (
                bookmark.url == normalizedUrl &&
                bookmark.folderId == targetFolderId &&
                bookmark.secret == original.secret
            ) {
                null
            } else {
                bookmark
            }
        }
        persistBookmarks(nextBookmarks)
        return updated
    }

    fun moveBookmark(bookmarkId: Long, folderId: Long?): Boolean {
        val bookmarks = getBookmarks()
        val bookmarkToMove = bookmarks.firstOrNull { it.id == bookmarkId } ?: return false
        if (folderId != null) {
            val targetFolder = getFolders().firstOrNull { it.id == folderId } ?: return false
            if (targetFolder.secret != bookmarkToMove.secret) {
                return false
            }
        }

        var moved = false
        val updated = bookmarks.map { bookmark ->
            if (bookmark.id == bookmarkId) {
                moved = true
                bookmark.copy(folderId = folderId, order = nextBookmarkOrder(folderId, bookmark.secret))
            } else {
                bookmark
            }
        }
        if (moved) {
            persistBookmarks(updated)
        }
        return moved
    }

    fun setBookmarkSecret(bookmarkId: Long, secret: Boolean): Boolean {
        val bookmarks = getBookmarks()
        val bookmarkToMove = bookmarks.firstOrNull { it.id == bookmarkId } ?: return false
        if (bookmarkToMove.secret == secret) {
            return false
        }

        var folders = getFolders()
        val targetFolderId = bookmarkToMove.folderId?.let { folderId ->
            val sourceFolder = folders.firstOrNull { it.id == folderId }
            when {
                sourceFolder == null -> null
                sourceFolder.secret == secret -> folderId
                else -> {
                    val mirrored = mirrorFolderPathForSecret(
                        sourceFolderId = folderId,
                        targetSecret = secret,
                        folders = folders,
                        bookmarks = bookmarks
                    )
                    folders = mirrored.folders
                    mirrored.folderId
                }
            }
        }
        val targetOrder = nextBookmarkOrder(
            bookmarks = bookmarks.filterNot { it.id == bookmarkId },
            folderId = targetFolderId,
            secret = secret
        )
        val updated = bookmarks.mapNotNull { bookmark ->
            if (bookmark.id == bookmarkId) {
                bookmark.copy(
                    folderId = targetFolderId,
                    order = targetOrder,
                    secret = secret
                )
            } else if (
                bookmark.url == bookmarkToMove.url &&
                bookmark.folderId == targetFolderId &&
                bookmark.secret == secret
            ) {
                null
            } else {
                bookmark
            }
        }
        persistFolders(folders)
        persistBookmarks(updated)
        return true
    }

    fun addBookmarkToHomeNavigation(bookmarkId: Long): Boolean {
        val bookmark = getBookmarks().firstOrNull { it.id == bookmarkId } ?: return false
        val homeFolderId = addFolder(HOME_NAVIGATION_FOLDER_TITLE, parentId = null).id
        return upsertBookmark(
            title = bookmark.title,
            url = bookmark.url,
            iconUrl = bookmark.iconUrl,
            folderId = homeFolderId,
            secret = false
        ) != null
    }

    fun addFolderBookmarksToHomeNavigation(folderId: Long): Int {
        val folders = getFolders()
        val includedFolderIds = collectDescendantIds(folderId, folders) + folderId
        val bookmarksToAdd = getBookmarks()
            .filter { bookmark -> includedFolderIds.contains(bookmark.folderId) && !bookmark.secret }
        if (bookmarksToAdd.isEmpty()) {
            return 0
        }

        val homeFolderId = addFolder(HOME_NAVIGATION_FOLDER_TITLE, parentId = null).id
        bookmarksToAdd.forEach { bookmark ->
            upsertBookmark(
                title = bookmark.title,
                url = bookmark.url,
                iconUrl = bookmark.iconUrl,
                folderId = homeFolderId,
                secret = false
            )
        }
        return bookmarksToAdd.size
    }

    fun deleteBookmark(bookmarkId: Long) {
        val updatedBookmarks = getBookmarks().filterNot { it.id == bookmarkId }
        persistBookmarks(updatedBookmarks)
    }

    fun deleteAll() {
        persistFolders(emptyList())
        persistBookmarks(emptyList())
    }

    fun deleteAllInSecretSpace(secret: Boolean) {
        val folders = getFolders()
        val remainingBookmarks = getBookmarks().filter { it.secret != secret }
        val preservedFolderIds = collectReferencedFolderTree(
            bookmarks = remainingBookmarks,
            folders = folders
        ).toMutableSet()
        folders
            .filter { it.secret != secret }
            .forEach { folder ->
                preservedFolderIds += collectFolderAndAncestors(
                    folderId = folder.id,
                    folders = folders
                )
            }
        persistFolders(
            folders
                .filter { preservedFolderIds.contains(it.id) }
                .map { folder ->
                    if (folder.secret == secret) {
                        folder.copy(secret = !secret)
                    } else {
                        folder
                    }
                }
        )
        persistBookmarks(remainingBookmarks)
    }

    fun deleteBookmarksInFolder(folderId: Long?, secret: Boolean? = null) {
        val folders = getFolders()
        val includedFolderIds = folderId?.let { collectDescendantIds(it, folders) + it }
        persistBookmarks(
            getBookmarks().filterNot { bookmark ->
                (includedFolderIds?.contains(bookmark.folderId) ?: (bookmark.folderId == folderId)) &&
                    (secret == null || bookmark.secret == secret)
            }
        )
    }

    fun reorderFolder(folderId: Long, direction: Int): Boolean {
        val folder = getFolders().firstOrNull { it.id == folderId } ?: return false
        val siblings = getFolders().filter { it.parentId == folder.parentId && it.secret == folder.secret }
        val index = siblings.indexOfFirst { it.id == folderId }
        val targetIndex = index + direction
        if (index < 0 || targetIndex !in siblings.indices) {
            return false
        }
        val reordered = siblings.toMutableList().apply {
            add(targetIndex, removeAt(index))
        }
        val orderById = reordered.mapIndexed { position, item -> item.id to position.toLong() }.toMap()
        persistFolders(
            getFolders().map { item ->
                orderById[item.id]?.let { item.copy(order = it) } ?: item
            }
        )
        return true
    }

    fun reorderBookmark(bookmarkId: Long, direction: Int): Boolean {
        val bookmark = getBookmarks().firstOrNull { it.id == bookmarkId } ?: return false
        val siblings = getBookmarks().filter { it.folderId == bookmark.folderId && it.secret == bookmark.secret }
        val index = siblings.indexOfFirst { it.id == bookmarkId }
        val targetIndex = index + direction
        if (index < 0 || targetIndex !in siblings.indices) {
            return false
        }
        val reordered = siblings.toMutableList().apply {
            add(targetIndex, removeAt(index))
        }
        val orderById = reordered.mapIndexed { position, item -> item.id to position.toLong() }.toMap()
        persistBookmarks(
            getBookmarks().map { item ->
                orderById[item.id]?.let { item.copy(order = it) } ?: item
            }
        )
        return true
    }

    fun exportJson(folderId: Long? = null, secret: Boolean? = null): String {
        val folders = getFolders()
        val includedFolderIds = folderId?.let { collectDescendantIds(it, folders) + it }
        val exportBookmarks = getBookmarks().filter { bookmark ->
            (includedFolderIds?.contains(bookmark.folderId) ?: true) &&
                (secret == null || bookmark.secret == secret)
        }
        val exportFolderIds = collectReferencedFolderTree(
            bookmarks = exportBookmarks,
            folders = folders,
            allowedFolderIds = includedFolderIds
        ).toMutableSet()
        folders.forEach { folder ->
            val inRequestedScope = includedFolderIds?.contains(folder.id) ?: true
            val inRequestedSpace = secret == null || folder.secret == secret
            if (inRequestedScope && inRequestedSpace) {
                exportFolderIds += folder.id
            }
        }
        val exportFolders = folders.filter { exportFolderIds.contains(it.id) }
        return JSONObject().apply {
            put("version", 1)
            put("folders", foldersToJson(exportFolders))
            put("bookmarks", bookmarksToJson(exportBookmarks))
        }.toString()
    }

    fun importJson(raw: String, replace: Boolean = false, targetSecret: Boolean? = null): ImportResult {
        val trimmed = unwrapHikerShareCommand(raw.trim())
        if (trimmed.isBlank()) {
            return ImportResult(0, 0, 0)
        }
        return runCatching {
            val foldersToImport = mutableListOf<BrowserBookmarkFolder>()
            val bookmarksToImport = mutableListOf<BrowserBookmarkItem>()
            var hikerImport = false
            if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                readFolders(root.optJSONArray("folders")).let(foldersToImport::addAll)
                readBookmarks(root.optJSONArray("bookmarks")).let(bookmarksToImport::addAll)
            } else {
                val array = JSONArray(trimmed)
                if (looksLikeHikerBookmarkArray(array)) {
                    hikerImport = true
                    val converted = readHikerBookmarks(array)
                    foldersToImport += converted.first
                    bookmarksToImport += converted.second
                } else {
                    readBookmarks(array).let(bookmarksToImport::addAll)
                }
            }

            if (replace) {
                persistFolders(emptyList())
                persistBookmarks(emptyList())
            }

            val currentFolders = getFolders().toMutableList()
            val currentBookmarks = getBookmarks().toMutableList()
            val folderIdMap = mutableMapOf<Long, Long>()
            val usedIds = (currentFolders.map { it.id } + currentBookmarks.map { it.id }).toMutableSet()
            var idSeed = System.currentTimeMillis()
            fun nextId(): Long {
                while (usedIds.contains(idSeed)) {
                    idSeed++
                }
                usedIds += idSeed
                return idSeed++
            }
            fun nextFolderOrderIn(parentId: Long?, secret: Boolean): Long {
                return (currentFolders
                    .filter { it.parentId == parentId && it.secret == secret }
                    .maxOfOrNull { it.order } ?: -1L) + 1L
            }
            fun nextBookmarkOrderIn(folderId: Long?, secret: Boolean): Long {
                return (currentBookmarks
                    .filter { it.folderId == folderId && it.secret == secret }
                    .maxOfOrNull { it.order } ?: -1L) + 1L
            }
            fun importFolder(folder: BrowserBookmarkFolder): Long {
                folderIdMap[folder.id]?.let { return it }
                val importedFolder = folder.copy(secret = targetSecret ?: folder.secret)
                val targetParentId = folder.parentId?.let { parentId ->
                    foldersToImport.firstOrNull { it.id == parentId }?.let(::importFolder)
                }
                val existing = currentFolders.firstOrNull {
                    it.title == importedFolder.title &&
                        it.parentId == targetParentId &&
                        it.secret == importedFolder.secret
                }
                val targetId = existing?.id ?: nextId()
                folderIdMap[folder.id] = targetId
                if (existing == null) {
                    currentFolders.add(
                        importedFolder.copy(
                            id = targetId,
                            parentId = targetParentId,
                            createdAt = System.currentTimeMillis(),
                            order = nextFolderOrderIn(targetParentId, importedFolder.secret)
                        )
                    )
                }
                return targetId
            }
            var importedFolders = 0
            foldersToImport.forEach { folder ->
                val before = currentFolders.size
                importFolder(folder)
                if (currentFolders.size > before) {
                    importedFolders++
                }
            }

            var importedBookmarks = 0
            bookmarksToImport.forEach { bookmark ->
                val importedSecret = targetSecret ?: bookmark.secret
                val targetFolderId = bookmark.folderId?.let { sourceFolderId ->
                    folderIdMap[sourceFolderId]
                        ?: currentFolders.firstOrNull {
                            it.id == sourceFolderId && it.secret == importedSecret
                        }?.id
                }
                if (hikerImport) {
                    val duplicateIndex = currentBookmarks.indexOfFirst {
                        it.url == bookmark.url && it.secret == importedSecret
                    }
                    if (duplicateIndex >= 0) {
                        val existing = currentBookmarks[duplicateIndex]
                        currentBookmarks[duplicateIndex] = existing.copy(
                            title = bookmark.title.ifBlank { existing.title },
                            iconUrl = bookmark.iconUrl.ifBlank { existing.iconUrl }
                        )
                        importedBookmarks++
                    } else if (bookmark.url.isNotBlank()) {
                        currentBookmarks.add(
                            bookmark.copy(
                                id = nextId(),
                                folderId = targetFolderId,
                                createdAt = System.currentTimeMillis(),
                                order = nextBookmarkOrderIn(targetFolderId, importedSecret),
                                secret = importedSecret
                            )
                        )
                        importedBookmarks++
                    }
                } else {
                    val duplicate = currentBookmarks.any {
                        it.url == bookmark.url &&
                            it.folderId == targetFolderId &&
                            it.secret == importedSecret
                    }
                    if (!duplicate && bookmark.url.isNotBlank()) {
                        currentBookmarks.add(
                            bookmark.copy(
                                id = nextId(),
                                folderId = targetFolderId,
                                createdAt = System.currentTimeMillis(),
                                order = nextBookmarkOrderIn(targetFolderId, importedSecret),
                                secret = importedSecret
                            )
                        )
                        importedBookmarks++
                    }
                }
            }

            persistFolders(currentFolders)
            persistBookmarks(currentBookmarks)
            ImportResult(importedFolders, importedBookmarks, bookmarksToImport.size - importedBookmarks)
        }.getOrElse { error ->
            if (error is JSONException || error is IllegalArgumentException) {
                ImportResult(0, 0, 0)
            } else {
                throw error
            }
        }
    }

    data class ImportResult(
        val folderCount: Int,
        val bookmarkCount: Int,
        val skippedCount: Int
    )

    fun exportHikerJson(folderId: Long? = null, secret: Boolean? = null): String {
        val folders = getFolders()
        val includedFolderIds = folderId?.let { collectDescendantIds(it, folders) + it }
        val exportFolders = folders.filter { folder ->
            (includedFolderIds?.contains(folder.id) ?: true) &&
                (secret == null || folder.secret == secret)
        }
        val exportBookmarks = getBookmarks().filter { bookmark ->
            (includedFolderIds?.contains(bookmark.folderId) ?: true) &&
                (secret == null || bookmark.secret == secret)
        }
        val folderById = folders.associateBy { it.id }
        fun groupName(targetFolderId: Long?): String {
            if (targetFolderId == null) {
                return ""
            }
            val path = mutableListOf<String>()
            var currentId: Long? = targetFolderId
            while (currentId != null) {
                val folder = folderById[currentId] ?: break
                path += folder.title
                currentId = folder.parentId
            }
            return path.asReversed().joinToString("/")
        }

        val array = JSONArray()
        exportFolders
            .sortedWith(compareBy<BrowserBookmarkFolder> { groupName(it.id) }.thenBy { it.order })
            .forEach { folder ->
                array.put(
                    JSONObject().apply {
                        put("title", groupName(folder.id))
                        put("url", "")
                        put("group", "")
                        put("order", folder.order.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt())
                    }
                )
            }
        exportBookmarks
            .sortedWith(compareBy<BrowserBookmarkItem> { groupName(it.folderId) }.thenBy { it.order })
            .forEach { bookmark ->
                array.put(
                    JSONObject().apply {
                        put("title", bookmark.title)
                        put("url", bookmark.url)
                        put("group", groupName(bookmark.folderId))
                        put("order", bookmark.order.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt())
                    }
                )
            }
        return array.toString()
    }

    fun exportHikerShareCommand(folderId: Long? = null, secret: Boolean? = null): String {
        return "海阔视界规则分享，当前分享的是：书签规则￥bookmark￥" +
            exportHikerJson(folderId = folderId, secret = secret)
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

    private fun collectReferencedFolderTree(
        bookmarks: List<BrowserBookmarkItem>,
        folders: List<BrowserBookmarkFolder>,
        allowedFolderIds: Set<Long>? = null
    ): Set<Long> {
        val folderById = folders.associateBy { it.id }
        val referencedIds = mutableSetOf<Long>()
        bookmarks.mapNotNull { it.folderId }.forEach { folderId ->
            var currentId: Long? = folderId
            while (currentId != null) {
                if (allowedFolderIds == null || allowedFolderIds.contains(currentId)) {
                    referencedIds += currentId
                }
                currentId = folderById[currentId]?.parentId
            }
        }
        return referencedIds
    }

    private fun collectFolderAndAncestors(
        folderId: Long,
        folders: List<BrowserBookmarkFolder>,
        allowedFolderIds: Set<Long>? = null
    ): Set<Long> {
        val folderById = folders.associateBy { it.id }
        val ids = mutableSetOf<Long>()
        var currentId: Long? = folderId
        while (currentId != null) {
            if (allowedFolderIds == null || allowedFolderIds.contains(currentId)) {
                ids += currentId
            }
            currentId = folderById[currentId]?.parentId
        }
        return ids
    }

    private data class MirroredFolderPath(
        val folderId: Long?,
        val folders: List<BrowserBookmarkFolder>
    )

    private fun mirrorFolderPathForSecret(
        sourceFolderId: Long,
        targetSecret: Boolean,
        folders: List<BrowserBookmarkFolder>,
        bookmarks: List<BrowserBookmarkItem>
    ): MirroredFolderPath {
        val folderById = folders.associateBy { it.id }
        val sourcePath = mutableListOf<BrowserBookmarkFolder>()
        var currentId: Long? = sourceFolderId
        while (currentId != null) {
            val folder = folderById[currentId] ?: break
            sourcePath += folder
            currentId = folder.parentId
        }
        if (sourcePath.isEmpty()) {
            return MirroredFolderPath(folderId = null, folders = folders)
        }

        val mutableFolders = folders.toMutableList()
        val usedIds = (folders.map { it.id } + bookmarks.map { it.id }).toMutableSet()
        var idSeed = System.currentTimeMillis()
        fun nextId(): Long {
            while (usedIds.contains(idSeed)) {
                idSeed++
            }
            usedIds += idSeed
            return idSeed++
        }
        fun nextOrder(parentId: Long?): Long {
            return (mutableFolders
                .filter { it.parentId == parentId && it.secret == targetSecret }
                .maxOfOrNull { it.order } ?: -1L) + 1L
        }

        var targetParentId: Long? = null
        sourcePath.asReversed().forEach { sourceFolder ->
            val existing = mutableFolders.firstOrNull {
                it.parentId == targetParentId &&
                    it.title == sourceFolder.title &&
                    it.secret == targetSecret
            }
            targetParentId = existing?.id ?: nextId().also { newId ->
                mutableFolders += sourceFolder.copy(
                    id = newId,
                    parentId = targetParentId,
                    createdAt = System.currentTimeMillis(),
                    order = nextOrder(targetParentId),
                    secret = targetSecret
                )
            }
        }
        return MirroredFolderPath(folderId = targetParentId, folders = mutableFolders)
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
                    put("order", folder.order)
                    put("secret", folder.secret)
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
                    put("order", bookmark.order)
                    put("secret", bookmark.secret)
                }
            )
        }
        sharedPreferences.edit().putString(KEY_BOOKMARKS, array.toString()).apply()
    }

    private fun nextFolderOrder(parentId: Long?, secret: Boolean): Long {
        return (getFolders()
            .filter { it.parentId == parentId && it.secret == secret }
            .maxOfOrNull { it.order } ?: -1L) + 1L
    }

    private fun nextBookmarkOrder(folderId: Long?, secret: Boolean): Long {
        return (getBookmarks()
            .filter { it.folderId == folderId && it.secret == secret }
            .maxOfOrNull { it.order } ?: -1L) + 1L
    }

    private fun nextBookmarkOrder(
        bookmarks: List<BrowserBookmarkItem>,
        folderId: Long?,
        secret: Boolean
    ): Long {
        return (bookmarks
            .filter { it.folderId == folderId && it.secret == secret }
            .maxOfOrNull { it.order } ?: -1L) + 1L
    }

    private fun nextStorageId(): Long {
        val usedIds = (getFolders().map { it.id } + getBookmarks().map { it.id }).toHashSet()
        var candidate = System.currentTimeMillis()
        while (usedIds.contains(candidate)) {
            candidate++
        }
        return candidate
    }

    private fun foldersToJson(folders: List<BrowserBookmarkFolder>): JSONArray {
        val array = JSONArray()
        folders.forEach { folder ->
            array.put(
                JSONObject().apply {
                    put("id", folder.id)
                    put("title", folder.title)
                    if (folder.parentId == null) put("parentId", JSONObject.NULL) else put("parentId", folder.parentId)
                    put("createdAt", folder.createdAt)
                    put("order", folder.order)
                    put("secret", folder.secret)
                }
            )
        }
        return array
    }

    private fun bookmarksToJson(bookmarks: List<BrowserBookmarkItem>): JSONArray {
        val array = JSONArray()
        bookmarks.forEach { bookmark ->
            array.put(
                JSONObject().apply {
                    put("id", bookmark.id)
                    put("title", bookmark.title)
                    put("url", bookmark.url)
                    put("iconUrl", bookmark.iconUrl)
                    if (bookmark.folderId == null) put("folderId", JSONObject.NULL) else put("folderId", bookmark.folderId)
                    put("createdAt", bookmark.createdAt)
                    put("order", bookmark.order)
                    put("secret", bookmark.secret)
                }
            )
        }
        return array
    }

    private fun readFolders(array: JSONArray?): List<BrowserBookmarkFolder> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    BrowserBookmarkFolder(
                        id = item.optLong("id", System.currentTimeMillis() + index),
                        title = item.optString("title"),
                        parentId = item.optLong("parentId").takeIf { !item.isNull("parentId") },
                        createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        order = item.optLong("order", item.optLong("createdAt", index.toLong())),
                        secret = item.optBoolean("secret", false)
                    )
                )
            }
        }
    }

    private fun readBookmarks(array: JSONArray?): List<BrowserBookmarkItem> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    BrowserBookmarkItem(
                        id = item.optLong("id", System.currentTimeMillis() + index),
                        title = item.optString("title"),
                        url = item.optString("url"),
                        iconUrl = item.optString("iconUrl"),
                        folderId = item.optLong("folderId").takeIf { !item.isNull("folderId") },
                        createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        order = item.optLong("order", item.optLong("createdAt", index.toLong())),
                        secret = item.optBoolean("secret", false)
                    )
                )
            }
        }
    }

    private fun looksLikeHikerBookmarkArray(array: JSONArray): Boolean {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            if (item.has("group") || (item.has("title") && item.has("url") && !item.has("folderId"))) {
                return true
            }
        }
        return false
    }

    private fun readHikerBookmarks(
        array: JSONArray
    ): Pair<List<BrowserBookmarkFolder>, List<BrowserBookmarkItem>> {
        val folders = mutableListOf<BrowserBookmarkFolder>()
        val bookmarks = mutableListOf<BrowserBookmarkItem>()
        val groupIdByPath = mutableMapOf<String, Long>()
        var idSeed = System.currentTimeMillis()
        fun nextId(): Long = idSeed++
        fun ensureFolder(group: String): Long? {
            val normalizedGroup = group.trim()
            if (normalizedGroup.isBlank()) {
                return null
            }

            var parentId: Long? = null
            var currentPath = ""
            normalizedGroup
                .split("/")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .forEach { title ->
                    currentPath = if (currentPath.isBlank()) title else "$currentPath/$title"
                    parentId = groupIdByPath.getOrPut(currentPath) {
                        val id = nextId()
                        folders += BrowserBookmarkFolder(
                            id = id,
                            title = title,
                            parentId = parentId,
                            createdAt = System.currentTimeMillis(),
                            order = folders.count { it.parentId == parentId }.toLong(),
                            secret = false
                        )
                        id
                    }
                }
            if (parentId == null) {
                val id = nextId()
                folders += BrowserBookmarkFolder(
                    id = id,
                    title = normalizedGroup,
                    parentId = null,
                    createdAt = System.currentTimeMillis(),
                    order = folders.size.toLong(),
                    secret = false
                )
                id
            }
            return parentId
        }

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val title = item.optString("title").trim()
            val url = item.optString("url").trim()
            val group = item.optString("group").trim()
            if (url.isBlank()) {
                ensureFolder(title.ifBlank { group })
            } else {
                bookmarks += BrowserBookmarkItem(
                    id = nextId(),
                    title = title.ifBlank {
                        url.substringAfter("://").substringBefore("/").ifBlank { url }
                    },
                    url = url,
                    iconUrl = item.optString("iconUrl"),
                    folderId = ensureFolder(group),
                    createdAt = System.currentTimeMillis(),
                    order = item.optLong("order", index.toLong())
                )
            }
        }
        return folders to bookmarks
    }

    private fun unwrapHikerShareCommand(raw: String): String {
        if (!raw.contains("￥bookmark￥")) {
            return raw
        }
        return raw.substringAfter("￥bookmark￥").trim()
    }

    companion object {
        private const val PREF_NAME = "browser_bookmarks"
        private const val KEY_FOLDERS = "browser_bookmark_folders"
        private const val KEY_BOOKMARKS = "browser_bookmark_items"
        private const val HOME_NAVIGATION_FOLDER_TITLE = "主页导航"

        private val LEGACY_ROOT_FOLDERS = listOf(
            HOME_NAVIGATION_FOLDER_TITLE,
            "人工智能",
            "动漫影视",
            "小说漫画",
            "成人动漫",
            "成人视频",
            "服务器"
        )
    }
}
