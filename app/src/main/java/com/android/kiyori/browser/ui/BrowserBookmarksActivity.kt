package com.android.kiyori.browser.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.android.kiyori.utils.ThemeManager

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
        enableEdgeToEdge()
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
                        overridePendingTransition(R.anim.no_anim, R.anim.no_anim)
                    },
                    onCreateFolder = { title, parentId ->
                        repository.addFolder(title, parentId)
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
                    onDeleteFolder = { folderId ->
                        repository.deleteFolder(folderId)
                        refreshState()
                    },
                    onSaveBookmark = { draft ->
                        repository.upsertBookmark(
                            title = draft.title,
                            url = draft.url,
                            iconUrl = draft.iconUrl,
                            folderId = draft.folderId
                        )
                        refreshState()
                    },
                    onDeleteBookmark = { bookmarkId ->
                        repository.deleteBookmark(bookmarkId)
                        refreshState()
                    },
                    onOpenBookmark = { url ->
                        BrowserActivity.start(this, url = url)
                        overridePendingTransition(R.anim.no_anim, R.anim.no_anim)
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
