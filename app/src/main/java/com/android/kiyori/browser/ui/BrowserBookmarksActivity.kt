package com.android.kiyori.browser.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.android.kiyori.R
import com.android.kiyori.browser.data.BrowserBookmarkRepository
import com.android.kiyori.browser.domain.BrowserBookmarkFolder
import com.android.kiyori.browser.domain.BrowserBookmarkItem
import com.android.kiyori.ui.theme.getThemeColors
import com.android.kiyori.utils.applyCloseActivityTransitionCompat
import com.android.kiyori.utils.applyOpenActivityTransitionCompat
import com.android.kiyori.utils.ThemeManager
import com.android.kiyori.utils.enableTransparentSystemBars

class BrowserBookmarksActivity : ComponentActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, BrowserBookmarksActivity::class.java))
        }
    }

    private lateinit var repository: BrowserBookmarkRepository
    private var folders by mutableStateOf<List<BrowserBookmarkFolder>>(emptyList())
    private var bookmarks by mutableStateOf<List<BrowserBookmarkItem>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableTransparentSystemBars()
        repository = BrowserBookmarkRepository(this)
        refreshState()

        setContent {
            val themeColors = getThemeColors(ThemeManager.getCurrentTheme(this).themeName)

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = themeColors.primary,
                    onPrimary = themeColors.onPrimary,
                    primaryContainer = themeColors.primaryVariant,
                    secondary = themeColors.secondary,
                    background = Color.White,
                    onBackground = Color(0xFF111827),
                    surface = Color.White,
                    surfaceVariant = Color(0xFFF5F5F5),
                    onSurface = Color(0xFF111827)
                )
            ) {
                BrowserBookmarksScreen(
                    folders = folders,
                    bookmarks = bookmarks,
                    onNavigateBack = {
                        finish()
                        applyCloseActivityTransitionCompat(R.anim.no_anim, R.anim.no_anim)
                    },
                    onCreateFolder = { title, parentId, secret ->
                        repository.addFolder(title, parentId, secret)
                        refreshState()
                    },
                    onRenameFolder = { folderId, title ->
                        repository.renameFolder(folderId, title)
                        refreshState()
                    },
                    onMoveFolder = { folderId, parentId ->
                        repository.moveFolder(folderId, parentId).also {
                            refreshState()
                        }
                    },
                    onDeleteFolder = { folderId, secret ->
                        repository.deleteFolder(folderId, secret)
                        refreshState()
                    },
                    onSaveBookmark = { draft, secret ->
                        val targetFolderId = draft.newFolderTitle
                            .trim()
                            .takeIf { it.isNotBlank() }
                            ?.let { repository.addFolder(it, parentId = null, secret = secret).id }
                            ?: draft.folderId
                        repository.upsertBookmark(
                            title = draft.title,
                            url = draft.url,
                            iconUrl = draft.iconUrl,
                            folderId = targetFolderId,
                            secret = secret
                        )
                        refreshState()
                    },
                    onUpdateBookmark = { bookmarkId, draft ->
                        val currentSecret = repository.getBookmarks()
                            .firstOrNull { it.id == bookmarkId }
                            ?.secret
                            ?: false
                        val targetFolderId = draft.newFolderTitle
                            .trim()
                            .takeIf { it.isNotBlank() }
                            ?.let { repository.addFolder(it, parentId = null, secret = currentSecret).id }
                            ?: draft.folderId
                        repository.updateBookmark(bookmarkId, draft.title, draft.url, draft.iconUrl, targetFolderId)
                        refreshState()
                    },
                    onMoveBookmark = { bookmarkId, folderId ->
                        repository.moveBookmark(bookmarkId, folderId).also { refreshState() }
                    },
                    onSetBookmarkSecret = { bookmarkId, secret ->
                        repository.setBookmarkSecret(bookmarkId, secret)
                        refreshState()
                    },
                    onDeleteBookmark = { bookmarkId ->
                        repository.deleteBookmark(bookmarkId)
                        refreshState()
                    },
                    onDeleteAllBookmarks = { secret ->
                        repository.deleteAllInSecretSpace(secret)
                        refreshState()
                    },
                    onDeleteBookmarksInFolder = { folderId, secret ->
                        repository.deleteBookmarksInFolder(folderId, secret)
                        refreshState()
                    },
                    onReorderFolder = { folderId, direction ->
                        repository.reorderFolder(folderId, direction).also { refreshState() }
                    },
                    onReorderBookmark = { bookmarkId, direction ->
                        repository.reorderBookmark(bookmarkId, direction).also { refreshState() }
                    },
                    onExportBookmarks = { folderId, secret -> repository.exportJson(folderId, secret) },
                    onShareBookmarks = { folderId, secret -> repository.exportHikerShareCommand(folderId, secret) },
                    onImportBookmarks = { raw, replace, secret ->
                        repository.importJson(raw, replace, secret).also { refreshState() }
                    },
                    onAddBookmarkToHomeNavigation = { bookmark ->
                        repository.addBookmarkToHomeNavigation(bookmark.id).also { refreshState() }
                    },
                    onAddFolderToHomeNavigation = { folderId ->
                        repository.addFolderBookmarksToHomeNavigation(folderId).also { refreshState() }
                    },
                    onOpenBookmark = { url ->
                        BrowserActivity.start(this, url = url)
                        applyOpenActivityTransitionCompat(R.anim.no_anim, R.anim.no_anim)
                    }
                )
            }
        }
    }

    private fun refreshState() {
        folders = repository.getFolders()
        bookmarks = repository.getBookmarks()
    }
}
