package com.android.kiyori.remote.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.kiyori.R
import com.android.kiyori.app.BaseActivity
import com.android.kiyori.ui.compose.ImmersiveTopAppBar
import com.android.kiyori.remote.RemotePlaybackHistoryRepository
import com.android.kiyori.remote.RemotePlaybackLauncher
import com.android.kiyori.remote.RemotePlaybackRequest
import com.android.kiyori.ui.theme.getThemeColors
import com.android.kiyori.utils.applyCloseActivityTransitionCompat
import com.android.kiyori.utils.ThemeManager
import com.android.kiyori.utils.enableTransparentSystemBars
import java.util.Date

class RemotePlaybackHistoryActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableTransparentSystemBars()

        val historyType = intent.getStringExtra(EXTRA_HISTORY_TYPE) ?: HISTORY_TYPE_SEARCH

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
                    surfaceVariant = Color(0xFFF8FAFC),
                    onSurface = Color(0xFF111827)
                )
            ) {
                RemotePlaybackHistoryScreen(
                    historyType = historyType,
                    onBack = {
                        finish()
                        applyCloseActivityTransitionCompat(R.anim.slide_in_left, R.anim.slide_out_right)
                    }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_HISTORY_TYPE = "history_type"
        const val HISTORY_TYPE_SEARCH = "search"
        const val HISTORY_TYPE_DEBUG = "debug"

        fun startSearch(context: Context) {
            context.startActivity(
                Intent(context, RemotePlaybackHistoryActivity::class.java).apply {
                    putExtra(EXTRA_HISTORY_TYPE, HISTORY_TYPE_SEARCH)
                }
            )
        }

        fun startDebug(context: Context) {
            context.startActivity(
                Intent(context, RemotePlaybackHistoryActivity::class.java).apply {
                    putExtra(EXTRA_HISTORY_TYPE, HISTORY_TYPE_DEBUG)
                }
            )
        }
    }
}

@Composable
private fun RemotePlaybackHistoryScreen(
    historyType: String,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember(context) { RemotePlaybackHistoryRepository(context) }

    if (historyType == RemotePlaybackHistoryActivity.HISTORY_TYPE_DEBUG) {
        var items by remember { mutableStateOf(repository.getDebugHistory()) }
        HistoryScaffold(
            title = "调试记录",
            emptyText = "暂时还没有保存的调试记录",
            tip = "这里会保存网络视频解析时生成的调试摘要，适合排查站点兼容问题。",
            onBack = onBack,
            hasItems = items.isNotEmpty()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    HistoryCard(
                        title = formatHistoryTime(item.createdAt),
                        subtitle = item.summary,
                        mono = true,
                        onPlay = null,
                        onCopy = {
                            copyText(context, item.summary, "已复制调试记录")
                        },
                        onDelete = {
                            repository.deleteDebugHistory(item.id)
                            items = repository.getDebugHistory()
                        }
                    )
                }
            }
        }
    } else {
        var items by remember { mutableStateOf(repository.getSearchHistory()) }
        HistoryScaffold(
            title = "搜索记录",
            emptyText = "暂时还没有保存的搜索记录",
            tip = "这里保存你手动输入过的网络播放地址。每条记录都可以复制、删除或直接再次播放。",
            onBack = onBack,
            hasItems = items.isNotEmpty()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    val headline = item.title.ifBlank { item.url }
                    val subline = buildString {
                        append(item.url)
                        if (item.sourcePageUrl.isNotBlank()) {
                            append("\n来源页: ")
                            append(item.sourcePageUrl)
                        }
                    }
                    HistoryCard(
                        title = headline,
                        subtitle = subline,
                        footnote = formatHistoryTime(item.createdAt),
                        onPlay = {
                            RemotePlaybackLauncher.start(
                                context,
                                RemotePlaybackRequest(
                                    url = item.url,
                                    title = item.title,
                                    sourcePageUrl = item.sourcePageUrl,
                                    source = RemotePlaybackRequest.Source.DIRECT_INPUT
                                )
                            )
                        },
                        onCopy = {
                            copyText(context, item.url, "已复制搜索记录链接")
                        },
                        onDelete = {
                            repository.deleteSearchHistory(item.id)
                            items = repository.getSearchHistory()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScaffold(
    title: String,
    emptyText: String,
    tip: String,
    onBack: () -> Unit,
    hasItems: Boolean,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            ImmersiveTopAppBar(
                title = {
                    Text(
                        text = title,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (hasItems) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = tip,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            color = Color(0xFF4B5563),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        content()
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF111827)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = emptyText,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    title: String,
    subtitle: String,
    footnote: String = "",
    mono: Boolean = false,
    onPlay: (() -> Unit)?,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = Color(0xFF111827),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White
            ) {
                Text(
                    text = subtitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    color = Color(0xFF374151),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default
                )
            }
            if (footnote.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = footnote,
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (onPlay != null) {
                    TextButton(onClick = onPlay) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("播放")
                    }
                }
                TextButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("复制")
                }
                TextButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除")
                }
            }
        }
    }
}

private fun copyText(context: Context, text: String, toast: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText("remote_history", text))
    Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
}

private fun formatHistoryTime(timestamp: Long): String {
    return DateFormat.format("yyyy-MM-dd HH:mm:ss", Date(timestamp)).toString()
}

