package com.android.kiyori.browser.domain

data class BrowserBookmarkFolder(
    val id: Long,
    val title: String,
    val parentId: Long?,
    val createdAt: Long,
    val order: Long = createdAt,
    val secret: Boolean = false
)

data class BrowserBookmarkItem(
    val id: Long,
    val title: String,
    val url: String,
    val iconUrl: String,
    val folderId: Long?,
    val createdAt: Long,
    val order: Long = createdAt,
    val secret: Boolean = false
)

data class BrowserBookmarkFolderOption(
    val id: Long,
    val title: String
)
