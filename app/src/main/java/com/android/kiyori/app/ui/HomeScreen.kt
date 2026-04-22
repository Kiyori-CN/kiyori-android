package com.android.kiyori.app.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.android.kiyori.R
import com.android.kiyori.bilibili.auth.BiliBiliAuthManager
import com.android.kiyori.browser.data.BrowserBookmarkRepository
import com.android.kiyori.browser.data.BrowserHistoryRepository
import com.android.kiyori.browser.ui.BrowserBookmarksActivity
import com.android.kiyori.history.PlaybackHistoryManager
import com.android.kiyori.history.ui.HistoryComposeActivity
import com.android.kiyori.history.ui.HistorySection
import com.android.kiyori.manager.PreferencesManager
import com.android.kiyori.manager.compose.BiliBiliLoginActivity
import com.android.kiyori.media.ui.LocalMediaBrowserActivity
import com.android.kiyori.media.VideoFileParcelable
import com.android.kiyori.bilibili.ui.BiliBiliPlayActivity
import com.android.kiyori.danmaku.ui.BiliBiliDanmakuComposeActivity
import com.android.kiyori.player.ui.VideoPlayerActivity
import com.android.kiyori.download.ui.DownloadActivity
import com.android.kiyori.remote.RemotePlaybackHeaders
import com.android.kiyori.remote.RemotePlaybackLauncher
import com.android.kiyori.remote.RemotePlaybackRequest
import com.android.kiyori.remote.RemoteUrlParser
import com.android.kiyori.browser.ui.BrowserActivity
import com.android.kiyori.settings.ui.SettingsScreen
import com.android.kiyori.subtitle.ui.SubtitleSearchActivity
import com.android.kiyori.webdav.WebDavComposeActivity
import kotlinx.coroutines.launch

/**
 * Compose 版本的主页
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    historyManager: PlaybackHistoryManager,
    initialTab: String? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val authManager = remember(context) { BiliBiliAuthManager.getInstance(context) }
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.fromRoute(initialTab) ?: HomeTab.Home) }
    var homePlaceholderTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var fileSubPage by rememberSaveable { mutableStateOf(FileSubPage.Home) }
    var filePlaceholderTitle by rememberSaveable { mutableStateOf<String?>(null) }
    val homePagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val bookmarkCount = remember(context) { BrowserBookmarkRepository(context).getBookmarks().size }
    val historyCount = remember(context) {
        BrowserHistoryRepository(context).getHistory().size + historyManager.getHistory().size
    }

    fun withBiliLogin(action: () -> Unit) {
        if (authManager.isLoggedIn()) {
            action()
        } else {
            android.widget.Toast.makeText(
                context,
                "请先在文件管理页完成 B 站登录",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(initialTab) {
        HomeTab.fromRoute(initialTab)?.let { selectedTab = it }
    }

    BackHandler(enabled = filePlaceholderTitle != null || fileSubPage != FileSubPage.Home) {
        when {
            filePlaceholderTitle != null -> filePlaceholderTitle = null
            fileSubPage != FileSubPage.Home -> fileSubPage = FileSubPage.Home
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        bottomBar = {
            if (!(selectedTab == HomeTab.Home && homePlaceholderTitle != null)) {
                HomeBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = {
                        when (it) {
                            HomeTab.Browser -> {
                                fileSubPage = FileSubPage.Home
                                filePlaceholderTitle = null
                                BrowserActivity.start(context)
                                activity?.overridePendingTransition(
                                    R.anim.no_anim,
                                    R.anim.no_anim
                                )
                            }
                            HomeTab.Home -> {
                                selectedTab = HomeTab.Home
                                homePlaceholderTitle = null
                                fileSubPage = FileSubPage.Home
                                filePlaceholderTitle = null
                                coroutineScope.launch {
                                    homePagerState.animateScrollToPage(1)
                                }
                            }
                            else -> {
                                fileSubPage = FileSubPage.Home
                                filePlaceholderTitle = null
                                selectedTab = it
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            when (selectedTab) {
                HomeTab.Home -> {
                    if (homePlaceholderTitle != null) {
                        HomeBlankSubPage(
                            title = homePlaceholderTitle.orEmpty(),
                            onBackClick = { homePlaceholderTitle = null }
                        )
                    } else {
                        HomeMinusOnePager(
                            pagerState = homePagerState,
                            bookmarkCount = bookmarkCount,
                            historyCount = historyCount,
                            onSearchClick = {
                                BrowserActivity.start(context, openSearch = true)
                                activity?.overridePendingTransition(
                                    R.anim.no_anim,
                                    R.anim.no_anim
                                )
                            },
                            onAiClick = { selectedTab = HomeTab.AI },
                            onCloseMinusOne = {
                                coroutineScope.launch {
                                    homePagerState.animateScrollToPage(1)
                                }
                            },
                            onOpenHistoryPage = {
                                HistoryComposeActivity.start(
                                    context,
                                    initialSection = HistorySection.WEB
                                )
                                activity?.overridePendingTransition(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left
                                )
                            },
                            onOpenMinusOnePage = { title ->
                                if (title == "\u4e66\u7b7e") {
                                    BrowserBookmarksActivity.start(context)
                                    activity?.overridePendingTransition(
                                        R.anim.slide_in_right,
                                        R.anim.slide_out_left
                                    )
                                } else {
                                    homePlaceholderTitle = title
                                }
                            }
                        )
                    }
                }

                HomeTab.Files -> {
                    if (filePlaceholderTitle != null) {
                        HomeBlankSubPage(
                            title = filePlaceholderTitle.orEmpty(),
                            onBackClick = { filePlaceholderTitle = null }
                        )
                    } else if (fileSubPage == FileSubPage.BiliBili) {
                        FileBiliBiliPage(
                            onBackClick = { fileSubPage = FileSubPage.Home },
                            onLoginClick = {
                                context.startActivity(Intent(context, BiliBiliLoginActivity::class.java))
                                activity?.overridePendingTransition(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left
                                )
                            },
                            onBiliBiliClick = {
                                context.startActivity(Intent(context, BiliBiliPlayActivity::class.java))
                                activity?.overridePendingTransition(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left
                                )
                            },
                            onVideoDownloadClick = {
                                withBiliLogin {
                                    context.startActivity(Intent(context, DownloadActivity::class.java))
                                    activity?.overridePendingTransition(
                                        R.anim.slide_in_right,
                                        R.anim.slide_out_left
                                    )
                                }
                            },
                            onDanmakuDownloadClick = {
                                withBiliLogin {
                                    context.startActivity(Intent(context, BiliBiliDanmakuComposeActivity::class.java))
                                    activity?.overridePendingTransition(
                                        R.anim.slide_in_right,
                                        R.anim.slide_out_left
                                    )
                                }
                            },
                            onSubtitleSearchClick = {
                                context.startActivity(Intent(context, SubtitleSearchActivity::class.java))
                                activity?.overridePendingTransition(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left
                                )
                            }
                        )
                    } else {
                        FileManagementPage(
                            onOpenBiliBili = { fileSubPage = FileSubPage.BiliBili },
                            onOpenPlaceholder = { title ->
                                filePlaceholderTitle = title
                            },
                            onLocalVideoClick = {
                                context.startActivity(Intent(context, LocalMediaBrowserActivity::class.java))
                                activity?.overridePendingTransition(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left
                                )
                            },
                            onLoginClick = {
                                context.startActivity(Intent(context, BiliBiliLoginActivity::class.java))
                                activity?.overridePendingTransition(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left
                                )
                            },
                            onBiliBiliClick = {
                                context.startActivity(Intent(context, BiliBiliPlayActivity::class.java))
                                activity?.overridePendingTransition(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left
                                )
                            },
                            onWebDavClick = {
                                context.startActivity(Intent(context, WebDavComposeActivity::class.java))
                                activity?.overridePendingTransition(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left
                                )
                            },
                            onVideoDownloadClick = {
                                withBiliLogin {
                                    context.startActivity(Intent(context, DownloadActivity::class.java))
                                    activity?.overridePendingTransition(
                                        R.anim.slide_in_right,
                                        R.anim.slide_out_left
                                    )
                                }
                            },
                            onDanmakuDownloadClick = {
                                withBiliLogin {
                                    context.startActivity(Intent(context, BiliBiliDanmakuComposeActivity::class.java))
                                    activity?.overridePendingTransition(
                                        R.anim.slide_in_right,
                                        R.anim.slide_out_left
                                    )
                                }
                            },
                            onSubtitleSearchClick = {
                                context.startActivity(Intent(context, SubtitleSearchActivity::class.java))
                                activity?.overridePendingTransition(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left
                                )
                            }
                        )
                    }
                }

                HomeTab.AI -> PlaceholderTabPage(title = selectedTab.title)
                HomeTab.Browser,
                HomeTab.Settings -> SettingsScreen(
                    onNavigateBack = {},
                    showBackButton = false
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeMinusOnePager(
    pagerState: androidx.compose.foundation.pager.PagerState,
    bookmarkCount: Int,
    historyCount: Int,
    onSearchClick: () -> Unit,
    onAiClick: () -> Unit,
    onCloseMinusOne: () -> Unit,
    onOpenHistoryPage: () -> Unit,
    onOpenMinusOnePage: (String) -> Unit
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> MinusOneScreen(
                bookmarkCount = bookmarkCount,
                historyCount = historyCount,
                onCloseClick = onCloseMinusOne,
                onHistoryClick = onOpenHistoryPage,
                onItemClick = onOpenMinusOnePage
            )
            else -> HomeLandingPage(
                onSearchClick = onSearchClick,
                onAiClick = onAiClick
            )
        }
    }
}

private enum class HomeTab(val title: String, val iconRes: Int) {
    Home("软件首页", R.drawable.ic_kiyori_nav_home),
    Browser("浏览器", R.drawable.ic_kiyori_nav_globe),
    AI("AI对话", R.drawable.ic_kiyori_nav_robot),
    Files("文件管理", R.drawable.ic_kiyori_nav_folder),
    Settings("设置页", R.drawable.ic_kiyori_nav_settings)
    ;
    companion object {
        fun fromRoute(route: String?): HomeTab? = when (route) {
            "home" -> Home
            "files" -> Files
            "ai" -> AI
            "settings" -> Settings
            else -> null
        }
    }
}

private enum class FileSubPage {
    Home,
    BiliBili
}

@Composable
private fun MinusOneScreen(
    bookmarkCount: Int,
    historyCount: Int,
    onCloseClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onItemClick: (String) -> Unit
) {
    val dataItems = remember(bookmarkCount, historyCount) {
        listOf(
            MinusOneDataItem("收藏", 0, Color(0xFF39A95F), listOf(Color(0xFFF0FAF2), Color(0xFFFFFFFF))),
            MinusOneDataItem("书签", bookmarkCount, Color(0xFFE45D65), listOf(Color(0xFFFEF0F1), Color(0xFFFFFFFF))),
            MinusOneDataItem("历史", historyCount, Color(0xFF6E48E6), listOf(Color(0xFFF4F0FE), Color(0xFFFFFFFF))),
            MinusOneDataItem("下载", 0, Color(0xFFE1BE4E), listOf(Color(0xFFFFF8E9), Color(0xFFFFFFFF)))
        )
    }
    val quickTools = remember {
        listOf(
            MinusOneQuickTool("新版", R.drawable.ic_kiyori_minus_one_new),
            MinusOneQuickTool("手册", R.drawable.ic_kiyori_minus_one_manual),
            MinusOneQuickTool("版本", R.drawable.ic_kiyori_minus_one_version),
            MinusOneQuickTool("搜索", R.drawable.ic_kiyori_minus_one_search),
            MinusOneQuickTool("工具箱", R.drawable.ic_kiyori_minus_one_toolbox),
            MinusOneQuickTool("清理", R.drawable.ic_kiyori_minus_one_clean),
            MinusOneQuickTool("备份", R.drawable.ic_kiyori_minus_one_backup),
            MinusOneQuickTool("退出", R.drawable.ic_kiyori_minus_one_exit)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(start = 20.dp, top = 12.dp, end = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "负一屏",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111)
            )
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭负一屏",
                    tint = Color(0xFF6B6B6B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "我的数据",
                fontSize = 13.sp,
                color = Color(0xFF8B8B8B),
                modifier = Modifier.padding(start = 4.dp)
            )

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dataItems.forEach { item ->
                        MinusOneDataCard(
                            item = item,
                            onClick = {
                                if (item.title == "历史") {
                                    onHistoryClick()
                                } else {
                                    onItemClick(item.title)
                                }
                            }
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "快捷工具",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF131313)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF222222),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        quickTools.chunked(4).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowItems.forEach { tool ->
                                    MinusOneQuickToolItem(
                                        tool = tool,
                                        onClick = { onItemClick(tool.title) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinusOneDataCard(
    item: MinusOneDataItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(brush = Brush.verticalGradient(item.backgroundColors))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = item.accentColor
            )
            Text(
                text = item.count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = item.accentColor
            )
        }
    }
}

@Composable
private fun MinusOneQuickToolItem(
    tool: MinusOneQuickTool,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = tool.iconRes),
                contentDescription = tool.title,
                tint = Color.Unspecified,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = tool.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF111111),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun HomeBlankSubPage(
    title: String,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color(0xFF111111)
                )
            }
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111)
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "空白页",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD1D5DB)
            )
        }
    }
}

private data class MinusOneDataItem(
    val title: String,
    val count: Int,
    val accentColor: Color,
    val backgroundColors: List<Color>
)

private data class MinusOneQuickTool(
    val title: String,
    val iconRes: Int
)

@Composable
private fun HomeLandingPage(
    onSearchClick: () -> Unit,
    onAiClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.36f))

        Text(
            text = "Kiyori",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF222222)
        )

        Spacer(modifier = Modifier.height(26.dp))

        HomeSearchCard(
            onSearchClick = onSearchClick,
            onAiClick = onAiClick
        )

        Spacer(modifier = Modifier.weight(0.64f))
    }
}

@Composable
private fun HomeSearchCard(
    onSearchClick: () -> Unit,
    onAiClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 314.dp)
            .fillMaxWidth()
            .height(108.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0x0D000000),
                spotColor = Color(0x0D000000)
            )
            .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .clickable(onClick = onSearchClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "把问题和任务告诉我",
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFFD9DBE0),
            modifier = Modifier.padding(start = 2.dp, top = 1.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFFF8F9FC))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeSearchModeChip(
                    icon = Icons.Default.Search,
                    label = "搜索",
                    active = true,
                    onClick = onSearchClick
                )
                HomeSearchModeChip(
                    icon = Icons.Default.AutoAwesome,
                    label = "AI",
                    active = false,
                    onClick = onAiClick
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeSearchLibraryIconButton(
                    iconRes = R.drawable.ic_kiyori_search_add_circle,
                    circled = false,
                    iconSize = 21.dp
                )
                HomeSearchLibraryIconButton(
                    iconRes = R.drawable.ic_kiyori_search_voice,
                    circled = false,
                    iconSize = 21.dp
                )
                HomeSearchLibraryIconButton(
                    iconRes = R.drawable.ic_kiyori_search_camera,
                    circled = false,
                    iconSize = 21.dp
                )
            }
        }
    }
}

@Composable
private fun HomeSearchModeChip(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) Color(0xFF2A66F5) else Color(0xFF9B9EA7),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = if (active) Color(0xFF2A66F5) else Color(0xFF9B9EA7)
        )
    }
}

@Composable
private fun HomeSearchLibraryIconButton(
    iconRes: Int,
    circled: Boolean,
    iconSize: Dp
) {
    val baseModifier = Modifier.size(if (circled) 30.dp else 28.dp)

    Box(
        modifier = if (circled) {
            baseModifier
                .clip(CircleShape)
                .border(1.4.dp, Color(0xFF000000), CircleShape)
        } else {
            baseModifier
        },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color(0xFF000000),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun FileManagementPage(
    onOpenBiliBili: () -> Unit,
    onOpenPlaceholder: (String) -> Unit,
    onLocalVideoClick: () -> Unit,
    onLoginClick: () -> Unit,
    onBiliBiliClick: () -> Unit,
    onWebDavClick: () -> Unit,
    onVideoDownloadClick: () -> Unit,
    onDanmakuDownloadClick: () -> Unit,
    onSubtitleSearchClick: () -> Unit
) {
    val categoryItems = remember {
        listOf(
            FileEntryItem(
                title = "图片",
                count = "0项",
                backgroundColors = listOf(Color(0xFF74AFFF), Color(0xFF4F86E6)),
                icon = Icons.Default.Image
            ),
            FileEntryItem(
                title = "视频",
                count = "0项",
                backgroundColors = listOf(Color(0xFFFF7888), Color(0xFFE65567)),
                icon = Icons.Default.PlayCircle
            ),
            FileEntryItem(
                title = "音频",
                count = "0项",
                backgroundColors = listOf(Color(0xFF4B4B57), Color(0xFF2F3138)),
                icon = Icons.Default.MusicNote
            ),
            FileEntryItem(
                title = "文档",
                count = "0项",
                backgroundColors = listOf(Color(0xFFFFE06A), Color(0xFFF4C53A)),
                icon = Icons.Default.Description
            ),
            FileEntryItem(
                title = "安装包",
                count = "0项",
                backgroundColors = listOf(Color(0xFF69D690), Color(0xFF4BC16C)),
                icon = Icons.Default.Android
            ),
            FileEntryItem(
                title = "压缩包",
                count = "0项",
                backgroundColors = listOf(Color(0xFFFFE06A), Color(0xFFF4C53A)),
                icon = Icons.Default.Folder
            ),
            FileEntryItem(
                title = "标签",
                count = "0项",
                backgroundColors = listOf(Color(0xFF7DB3FF), Color(0xFF4A86E8)),
                icon = Icons.Default.LocalOffer
            ),
            FileEntryItem(
                title = "下载",
                count = "0项",
                backgroundColors = listOf(Color(0xFFFFE7A6), Color(0xFFF5C14A)),
                icon = Icons.Default.Download
            )
        )
    }
    val quickAccessItems = remember {
        listOf(
            FileEntryItem(
                title = "哔哩哔哩",
                count = "0项",
                backgroundColors = listOf(Color(0xFFFFD8E6), Color(0xFFFFA9C9)),
                icon = Icons.Default.VideoLibrary
            )
        )
    }
    val storageItems = remember {
        listOf(
            FileStorageItem(
                title = "手机存储",
                value = "可用244.0 GB/512 GB",
                icon = Icons.Default.History
            ),
            FileStorageItem(
                title = "WebDAV",
                value = null,
                icon = Icons.Default.Cloud
            ),
            FileStorageItem(
                title = "最近删除",
                value = "0项",
                icon = Icons.Default.Delete
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF7F7F7))
                    .clickable { onOpenPlaceholder("搜索") }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF8C8C8C),
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "搜索",
                        fontSize = 14.sp,
                        color = Color(0xFF8C8C8C)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.KeyboardVoice,
                        contentDescription = null,
                        tint = Color(0xFF8C8C8C),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = { onOpenPlaceholder("更多") },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "更多",
                    tint = Color(0xFF111111),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        FileEntryGrid(
            items = categoryItems,
            onItemClick = { item ->
                if (item.title == "视频") {
                    onLocalVideoClick()
                } else {
                    onOpenPlaceholder(item.title)
                }
            }
        )

        Spacer(modifier = Modifier.height(34.dp))

        FileSectionHeader(
            title = "快捷访问",
            actionText = "全部",
            onClick = { onOpenPlaceholder("快捷访问全部") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        FileEntryGrid(
            items = quickAccessItems,
            onItemClick = { item ->
                if (item.title == "哔哩哔哩") {
                    onOpenBiliBili()
                } else {
                    onOpenPlaceholder(item.title)
                }
            }
        )

        Spacer(modifier = Modifier.height(36.dp))

        FileSectionHeader(
            title = "存储位置",
            actionText = "全部",
            onClick = { onOpenPlaceholder("存储位置全部") }
        )

        Spacer(modifier = Modifier.height(14.dp))

        storageItems.forEachIndexed { index, item ->
            FileStorageRow(
                item = item,
                onClick = {
                    if (item.title == "WebDAV") {
                        onWebDavClick()
                    } else {
                        onOpenPlaceholder(item.title)
                    }
                }
            )

            if (index != storageItems.lastIndex) {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun FileBiliBiliPage(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onBiliBiliClick: () -> Unit,
    onVideoDownloadClick: () -> Unit,
    onDanmakuDownloadClick: () -> Unit,
    onSubtitleSearchClick: () -> Unit
) {
    val biliItems = remember {
        listOf(
            FileEntryItem(
                title = "B站登录",
                count = "0项",
                backgroundColors = listOf(Color(0xFF7ED7A5), Color(0xFF47BE6F)),
                icon = Icons.Default.Person
            ),
            FileEntryItem(
                title = "B站番剧",
                count = "0项",
                backgroundColors = listOf(Color(0xFFFFE06A), Color(0xFFF4C53A)),
                icon = Icons.Default.VideoLibrary
            ),
            FileEntryItem(
                title = "视频下载",
                count = "0项",
                backgroundColors = listOf(Color(0xFFFFE7A6), Color(0xFFF5C14A)),
                icon = Icons.Default.Download
            ),
            FileEntryItem(
                title = "弹幕下载",
                count = "0项",
                backgroundColors = listOf(Color(0xFF4B4B57), Color(0xFF2F3138)),
                icon = Icons.Default.Comment
            ),
            FileEntryItem(
                title = "字幕搜索",
                count = "0项",
                backgroundColors = listOf(Color(0xFF7EA9FF), Color(0xFF4C73E6)),
                icon = Icons.Default.Search
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color(0xFF111111),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "哔哩哔哩",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111111)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        FileEntryGrid(
            items = biliItems,
            onItemClick = { item ->
                when (item.title) {
                    "B站登录" -> onLoginClick()
                    "B站番剧" -> onBiliBiliClick()
                    "视频下载" -> onVideoDownloadClick()
                    "弹幕下载" -> onDanmakuDownloadClick()
                    "字幕搜索" -> onSubtitleSearchClick()
                }
            }
        )

        Spacer(modifier = Modifier.height(18.dp))
    }
}

private data class FileEntryItem(
    val title: String,
    val count: String,
    val backgroundColors: List<Color>,
    val icon: ImageVector? = null,
    val iconRes: Int? = null,
    val iconTint: Color = Color.White
)

private data class FileStorageItem(
    val title: String,
    val value: String?,
    val icon: ImageVector
)

@Composable
private fun FileSectionHeader(
    title: String,
    actionText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFB0B0B0)
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = actionText,
                fontSize = 15.sp,
                color = Color(0xFFC3C3C3)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFC3C3C3),
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun FileEntryGrid(
    items: List<FileEntryItem>,
    onItemClick: (FileEntryItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        items.chunked(4).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { item ->
                    FileEntryTile(
                        modifier = Modifier.weight(1f),
                        item = item,
                        onClick = { onItemClick(item) }
                    )
                }

                repeat(4 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FileEntryTile(
    modifier: Modifier = Modifier,
    item: FileEntryItem,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(brush = Brush.linearGradient(item.backgroundColors)),
            contentAlignment = Alignment.Center
        ) {
            when {
                item.icon != null -> {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = item.iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }

                item.iconRes != null -> {
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = item.title,
                        tint = item.iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF111111)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = item.count,
            fontSize = 8.sp,
            color = Color(0xFFC1C1C1)
        )
    }
}

@Composable
private fun FileStorageRow(
    item: FileStorageItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = Color(0xFF111111),
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = item.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF111111)
        )

        Spacer(modifier = Modifier.weight(1f))

        if (!item.value.isNullOrBlank()) {
            Text(
                text = item.value,
                fontSize = 13.sp,
                color = Color(0xFFC0C0C0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFC0C0C0),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun HomeBottomBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeTab.entries.forEach { tab ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (selectedTab == tab) Color(0xFFF1F4FF) else Color.Transparent
                            )
                            .clickable { onTabSelected(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = tab.iconRes),
                            contentDescription = tab.title,
                            tint = Color(0xFF000000),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderTabPage(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFBBBBBB)
        )
    }
}

/**
 * 顶部栏
 */
@Composable
fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Kiyori",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF222222)
        )
    }
}

/**
 * Logo 区域（可点击继续播放）
 */
@Composable
fun LogoSection(
    historyManager: PlaybackHistoryManager,
    onContinuePlay: (PlaybackHistoryManager.HistoryItem) -> Unit
) {
    val context = LocalContext.current
    val lastVideo = historyManager.getLastPlayedLocalVideo()
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Logo 图片（直接显示图标，不带背景框）
        Icon(
            painter = painterResource(id = R.drawable.ic_continue_play),
            contentDescription = "继续播放",
            modifier = Modifier
                .size(120.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable {
                    val video = historyManager.getLastPlayedLocalVideo()
                    if (video != null) {
                        onContinuePlay(video)
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "暂无播放记录",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
            tint = Color.Unspecified
        )
        
        // 提示文字（仅在有播放记录时显示）
        if (lastVideo != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "继续播放: ${lastVideo.fileName}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF666666),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .padding(horizontal = 16.dp),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 渐变按钮
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(280.dp)
            .height(60.dp),
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp,
            hoveredElevation = 10.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun RemoteUrlDialog(
    onDismiss: () -> Unit,
    onConfirm: (RemotePlaybackRequest) -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember(context) { PreferencesManager.getInstance(context) }
    var lastRemoteDebugSummary by remember { mutableStateOf(preferencesManager.getLastRemoteDebugSummary()) }
    var url by remember { mutableStateOf(preferencesManager.getLastRemoteInputUrl()) }
    var title by remember { mutableStateOf(preferencesManager.getLastRemoteInputTitle()) }
    var sourcePageUrl by remember { mutableStateOf(preferencesManager.getLastRemoteInputSourcePageUrl()) }
    var referer by remember { mutableStateOf(preferencesManager.getLastRemoteInputReferer()) }
    var origin by remember { mutableStateOf(preferencesManager.getLastRemoteInputOrigin()) }
    var cookie by remember { mutableStateOf(preferencesManager.getLastRemoteInputCookie()) }
    var authorization by remember { mutableStateOf(preferencesManager.getLastRemoteInputAuthorization()) }
    var userAgent by remember { mutableStateOf(preferencesManager.getLastRemoteInputUserAgent()) }
    val hasSavedAdvancedInput =
        listOf(sourcePageUrl, referer, origin, cookie, authorization, userAgent).any { it.isNotBlank() }
    var showAdvanced by remember { mutableStateOf(hasSavedAdvancedInput) }
    val dialogContainerColor = MaterialTheme.colorScheme.primary
    val dialogFieldColor = MaterialTheme.colorScheme.surfaceVariant
    val dialogPrimaryColor = MaterialTheme.colorScheme.primary
    val dialogSecondaryColor = MaterialTheme.colorScheme.onPrimary
    val dialogTextColor = MaterialTheme.colorScheme.onPrimary
    val dialogMutedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = dialogFieldColor,
        unfocusedContainerColor = dialogFieldColor,
        disabledContainerColor = dialogFieldColor,
        focusedTextColor = dialogTextColor,
        unfocusedTextColor = dialogTextColor,
        disabledTextColor = dialogMutedTextColor,
        focusedLabelColor = dialogSecondaryColor,
        unfocusedLabelColor = dialogMutedTextColor,
        disabledLabelColor = dialogMutedTextColor,
        focusedPlaceholderColor = dialogMutedTextColor,
        unfocusedPlaceholderColor = dialogMutedTextColor,
        disabledPlaceholderColor = dialogMutedTextColor,
        focusedBorderColor = dialogPrimaryColor,
        unfocusedBorderColor = dialogPrimaryColor.copy(alpha = 0.35f),
        disabledBorderColor = dialogPrimaryColor.copy(alpha = 0.2f),
        cursorColor = dialogPrimaryColor
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogContainerColor,
        titleContentColor = dialogTextColor,
        textContentColor = dialogTextColor,
        title = {
            Text(
                text = "播放网络视频",
                color = dialogTextColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = dialogContainerColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = dialogSecondaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "基础信息",
                                color = dialogTextColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("视频链接") },
                            placeholder = { Text("https://example.com/video.mp4 或直接粘贴 curl / 请求头") },
                            minLines = 3,
                            maxLines = 6,
                            colors = textFieldColors
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("标题（可选）") },
                            singleLine = true,
                            colors = textFieldColors
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { showAdvanced = !showAdvanced },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = dialogPrimaryColor.copy(alpha = 0.12f),
                        contentColor = dialogSecondaryColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showAdvanced) "收起高级设置" else "展开高级设置")
                }

                if (lastRemoteDebugSummary.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = dialogFieldColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = dialogSecondaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "上次远程调试信息",
                                        color = dialogTextColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboardManager.setPrimaryClip(
                                            ClipData.newPlainText("remote_debug_summary", lastRemoteDebugSummary)
                                        )
                                        android.widget.Toast.makeText(
                                            context,
                                            "已复制上次远程调试信息",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = dialogSecondaryColor)
                                ) {
                                    Text("复制")
                                }
                            }

                            SelectionContainer {
                                Text(
                                    text = lastRemoteDebugSummary,
                                    color = dialogMutedTextColor,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            TextButton(
                                onClick = {
                                    preferencesManager.clearLastRemoteDebugSummary()
                                    lastRemoteDebugSummary = ""
                                },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.textButtonColors(contentColor = dialogMutedTextColor)
                            ) {
                                Text("清空调试信息")
                            }
                        }
                    }
                }

                if (url.isNotBlank() || title.isNotBlank() || hasSavedAdvancedInput) {
                    TextButton(
                        onClick = {
                            url = ""
                            title = ""
                            sourcePageUrl = ""
                            referer = ""
                            origin = ""
                            cookie = ""
                            authorization = ""
                            userAgent = ""
                            showAdvanced = false
                            preferencesManager.clearLastRemoteInputDraft()
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.textButtonColors(contentColor = dialogMutedTextColor)
                    ) {
                        Text("清空已保存输入")
                    }
                }

                if (showAdvanced) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = dialogContainerColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = dialogSecondaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "高级请求头",
                                    color = dialogTextColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedTextField(
                                value = referer,
                                onValueChange = { referer = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Referer（可选）") },
                                singleLine = true,
                                colors = textFieldColors
                            )

                            OutlinedTextField(
                                value = origin,
                                onValueChange = { origin = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Origin（可选）") },
                                singleLine = true,
                                colors = textFieldColors
                            )

                            OutlinedTextField(
                                value = cookie,
                                onValueChange = { cookie = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Cookie（可选）") },
                                singleLine = true,
                                colors = textFieldColors
                            )

                            OutlinedTextField(
                                value = authorization,
                                onValueChange = { authorization = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Authorization（可选）") },
                                singleLine = true,
                                colors = textFieldColors
                            )

                            OutlinedTextField(
                                value = userAgent,
                                onValueChange = { userAgent = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("User-Agent（可选）") },
                                singleLine = true,
                                colors = textFieldColors
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedInput = RemoteUrlParser.parsePlaybackInput(url)
                    val normalizedSourcePageUrl = sourcePageUrl.trim().ifBlank { referer.trim() }
                    val headers = linkedMapOf<String, String>().apply {
                        putAll(parsedInput?.headers.orEmpty())
                    }
                    if (referer.isNotBlank()) {
                        headers["Referer"] = referer.trim()
                    }
                    if (origin.isNotBlank()) {
                        headers["Origin"] = origin.trim()
                    }
                    if (cookie.isNotBlank()) {
                        headers["Cookie"] = cookie.trim()
                    }
                    if (authorization.isNotBlank()) {
                        headers["Authorization"] = authorization.trim()
                    }
                    if (userAgent.isNotBlank()) {
                        headers["User-Agent"] = userAgent.trim()
                    }

                    preferencesManager.setLastRemoteInputUrl(url)
                    preferencesManager.setLastRemoteInputTitle(title.trim())
                    preferencesManager.setLastRemoteInputSourcePageUrl(normalizedSourcePageUrl)
                    preferencesManager.setLastRemoteInputReferer(referer.trim())
                    preferencesManager.setLastRemoteInputOrigin(origin.trim())
                    preferencesManager.setLastRemoteInputCookie(cookie.trim())
                    preferencesManager.setLastRemoteInputAuthorization(authorization.trim())
                    preferencesManager.setLastRemoteInputUserAgent(userAgent.trim())

                    onConfirm(
                        RemotePlaybackRequest(
                            url = parsedInput?.url ?: url.trim(),
                            title = title.trim(),
                            sourcePageUrl = normalizedSourcePageUrl,
                            headers = RemotePlaybackHeaders.normalize(headers),
                            source = RemotePlaybackRequest.Source.DIRECT_INPUT
                        )
                    )
                },
                enabled = url.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = dialogPrimaryColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("播放")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = dialogMutedTextColor)
            ) {
                Text("取消")
            }
        }
    )
}

/**
 * 可展开的操作按钮
 */
@Composable
fun HomeQuickActions(
    onLocalVideoClick: () -> Unit,
    onLoginClick: () -> Unit,
    onBiliBiliClick: () -> Unit,
    onWebDavClick: () -> Unit,
    onVideoDownloadClick: () -> Unit,
    onDanmakuDownloadClick: () -> Unit,
    onSubtitleSearchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeQuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Folder,
                label = "本地视频",
                onClick = onLocalVideoClick
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeQuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Person,
                label = "B站登录",
                onClick = onLoginClick
            )
            HomeQuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.VideoLibrary,
                label = "B站番剧",
                onClick = onBiliBiliClick
            )
            HomeQuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Cloud,
                label = "WebDAV",
                onClick = onWebDavClick
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeQuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Download,
                label = "B站视频下载",
                onClick = onVideoDownloadClick
            )
            HomeQuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Comment,
                label = "B站弹幕下载",
                onClick = onDanmakuDownloadClick
            )
            HomeQuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Search,
                label = "字幕搜索下载",
                onClick = onSubtitleSearchClick
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun HomeQuickActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE3F2FD),
                            Color(0xFFBBDEFB)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun HomeQuickActionButton(
    modifier: Modifier = Modifier,
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE3F2FD),
                            Color(0xFFBBDEFB)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = Color(0xFF111111),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun ExpandableActionButton(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onBrowserClick: () -> Unit,
    onBiliBiliClick: () -> Unit,
    onWebDavClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom
        ) {
            // 展开的功能区
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(300)) + 
                        expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)) + 
                       shrinkVertically(animationSpec = tween(300))
            ) {
                Card(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .widthIn(min = 240.dp, max = 312.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ActionItem(
                            icon = Icons.Default.Language,
                            label = "浏览器",
                            onClick = onBrowserClick
                        )

                        // TV浏览器（视频嗅探）
                        
                        // 哔哩哔哩番剧
                        ActionItem(
                            icon = Icons.Default.VideoLibrary,
                            label = "哔哩哔哩番剧",
                            onClick = onBiliBiliClick
                        )
                        
                        // WebDAV
                        ActionItem(
                            icon = Icons.Default.Cloud,
                            label = "WebDAV",
                            onClick = onWebDavClick
                        )
                    }
                }
            }
            
            // 展开/收起按钮
            FloatingActionButton(
                onClick = onToggle,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.size(60.dp)
            ) {
                // 旋转动画
                val rotation by animateFloatAsState(
                    targetValue = if (isExpanded) 45f else 0f,
                    animationSpec = tween(300),
                    label = "rotation"
                )
                
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}

/**
 * 功能项（图标 + 文字）
 */
@Composable
fun ActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(70.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 1.dp, vertical = 6.dp)
    ) {
        // 图标背景（参考设置页样式）
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE3F2FD),
                            Color(0xFFBBDEFB)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 带背景的图标按钮（参考设置页样式）
 */
@Composable
fun IconWithBackground(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD),
                        Color(0xFFBBDEFB)
                    )
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * 继续播放功能
 */
private fun continueLastPlay(
    context: android.content.Context,
    lastVideo: PlaybackHistoryManager.HistoryItem
) {
    try {
        val videoUri = Uri.parse(lastVideo.uri)
        
        com.android.kiyori.utils.Logger.d("HomeScreen", "=== Continue Last Play ===")
        com.android.kiyori.utils.Logger.d("HomeScreen", "Video URI: $videoUri")
        com.android.kiyori.utils.Logger.d("HomeScreen", "Folder name: ${lastVideo.folderName}")
        
        // 【修复】从 URI 获取完整的文件夹路径来扫描视频
        val folderPath = getFullFolderPath(context, videoUri)
        com.android.kiyori.utils.Logger.d("HomeScreen", "Full folder path: $folderPath")
        
        val videoList = if (folderPath != null) {
            scanVideosInFolder(context, folderPath)
        } else {
            emptyList()
        }
        
        com.android.kiyori.utils.Logger.d("HomeScreen", "Scanned ${videoList.size} videos from folder")
        
        val intent = Intent(context, VideoPlayerActivity::class.java).apply {
            data = videoUri
            action = Intent.ACTION_VIEW
            putExtra("folder_path", lastVideo.folderName)
            putExtra("last_position", lastVideo.position)
            
            if (videoList.isNotEmpty()) {
                putParcelableArrayListExtra("video_list", ArrayList(videoList))
                com.android.kiyori.utils.Logger.d("HomeScreen", "Put ${videoList.size} videos into intent")
            } else {
                com.android.kiyori.utils.Logger.w("HomeScreen", "No videos found, will use identifySeries fallback")
            }
        }
        
        context.startActivity(intent)
        (context as? android.app.Activity)?.overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    } catch (e: Exception) {
        com.android.kiyori.utils.Logger.e("HomeScreen", "Failed to continue last play", e)
        android.widget.Toast.makeText(
            context,
            "无法播放该视频: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * 从 URI 获取完整的文件夹路径
 */
private fun getFullFolderPath(context: android.content.Context, uri: Uri): String? {
    val projection = arrayOf(android.provider.MediaStore.Video.Media.DATA)
    
    try {
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val dataColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA)
                val fullPath = cursor.getString(dataColumn)
                // 从完整路径中提取文件夹路径
                return fullPath.substringBeforeLast("/")
            }
        }
    } catch (e: Exception) {
        com.android.kiyori.utils.Logger.e("HomeScreen", "Failed to get folder path from URI", e)
    }
    
    return null
}

/**
 * 扫描指定文件夹的所有视频文件
 */
private fun scanVideosInFolder(context: android.content.Context, folderPath: String): List<VideoFileParcelable> {
    val videos = mutableListOf<VideoFileParcelable>()
    val projection = arrayOf(
        android.provider.MediaStore.Video.Media._ID,
        android.provider.MediaStore.Video.Media.DISPLAY_NAME,
        android.provider.MediaStore.Video.Media.DATA,
        android.provider.MediaStore.Video.Media.DURATION,
        android.provider.MediaStore.Video.Media.SIZE,
        android.provider.MediaStore.Video.Media.DATE_ADDED
    )
    
    val selection = "${android.provider.MediaStore.Video.Media.DATA} LIKE ?"
    val selectionArgs = arrayOf("$folderPath%")
    val sortOrder = "${android.provider.MediaStore.Video.Media.DISPLAY_NAME} ASC"
    
    try {
        context.contentResolver.query(
            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA)
            val durationColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATE_ADDED)
            
            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn)
                // 只获取直接在该文件夹下的视频（不包括子文件夹）
                if (path.substringBeforeLast("/") == folderPath) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateColumn)
                    val uri = Uri.withAppendedPath(
                        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    ).toString()
                    
                    videos.add(
                        VideoFileParcelable(
                            uri = uri,
                            name = name,
                            path = path,
                            size = size,
                            duration = duration,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        com.android.kiyori.utils.Logger.e("HomeScreen", "Failed to scan videos in folder: $folderPath", e)
    }
    
    return videos
}

