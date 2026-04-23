package com.android.kiyori.settings.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.kiyori.R
import com.android.kiyori.browser.data.BrowserPreferencesRepository
import com.android.kiyori.browser.security.BrowserSecurityPolicy
import com.android.kiyori.settings.PlaybackSettingsComposeActivity
import com.android.kiyori.utils.applyOpenActivityTransitionCompat
import com.android.kiyori.ui.compose.SettingsColors as SettingsPalette

private val SettingsCardBorderColor = Color(0xFFE8E8E2)

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    showBackButton: Boolean = true,
    initialPage: String? = null,
    navigationRequestId: Long = 0L,
    onRootPageStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val browserPreferencesRepository = remember { BrowserPreferencesRepository(context) }
    var browserHomeUrl by remember { mutableStateOf(browserPreferencesRepository.getHomeUrl()) }
    var currentPage by remember(initialPage) {
        mutableStateOf(settingsPageFromKey(initialPage) ?: SettingsPage.Root)
    }
    var browserSubPage by remember { mutableStateOf<BrowserSubPage?>(null) }

    LaunchedEffect(initialPage, navigationRequestId) {
        currentPage = settingsPageFromKey(initialPage) ?: SettingsPage.Root
        browserSubPage = null
    }

    fun launchActivity(intent: Intent) {
        context.startActivity(intent)
        activity?.applyOpenActivityTransitionCompat(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }

    fun handleBack() {
        when {
            browserSubPage != null -> browserSubPage = null
            currentPage != SettingsPage.Root -> currentPage = SettingsPage.Root
            showBackButton -> onNavigateBack()
        }
    }

    if (browserSubPage != null || currentPage != SettingsPage.Root || showBackButton) {
        BackHandler(onBack = ::handleBack)
    }

    LaunchedEffect(currentPage, browserSubPage) {
        onRootPageStateChanged(currentPage == SettingsPage.Root && browserSubPage == null)
    }

    val showPinnedHeaderBar = browserSubPage != null || currentPage != SettingsPage.Root

    Scaffold(
        containerColor = Color(0xFFF5F5F2),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showPinnedHeaderBar) {
            SettingsHeaderBar(
                title = browserSubPage?.title ?: when (currentPage) {
                    SettingsPage.Browser -> "浏览器功能设置"
                    SettingsPage.Player -> ""
                    SettingsPage.Download -> ""
                    SettingsPage.AdBlock -> ""
                    else -> currentPage.title
                },
                showBackButton = true,
                showRootActions = false,
                onBackClick = ::handleBack
            )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F2))
                .padding(paddingValues)
        ) {
            if (browserSubPage != null) {
                when (browserSubPage ?: BrowserSubPage.HomepageCustomization) {
                    is BrowserSubPage.HomepageCustomization -> {
                        BrowserHomepageCustomizationPage(
                            currentHomeUrl = browserHomeUrl,
                            onSave = { input ->
                                val resolvedUrl = if (input.isBlank()) {
                                    BrowserSecurityPolicy.BLANK_HOME_URL
                                } else {
                                    BrowserSecurityPolicy.normalizeUserInput(
                                        input = input,
                                        searchEngine = browserPreferencesRepository.getSearchEngine()
                                    )
                                }

                                if (resolvedUrl == null) {
                                    Toast.makeText(
                                        context,
                                        "自定义主页入口格式无效",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@BrowserHomepageCustomizationPage false
                                }

                                browserPreferencesRepository.setHomeUrl(resolvedUrl)
                                browserHomeUrl = resolvedUrl
                                Toast.makeText(
                                    context,
                                    "自定义主页入口已保存",
                                    Toast.LENGTH_SHORT
                                ).show()
                                true
                            },
                            onReset = {
                                browserPreferencesRepository.setHomeUrl(BrowserSecurityPolicy.BLANK_HOME_URL)
                                browserHomeUrl = BrowserSecurityPolicy.BLANK_HOME_URL
                                Toast.makeText(
                                    context,
                                    "已恢复为空白页",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    is BrowserSubPage.X5Debug -> {
                        BrowserX5DebugPage()
                    }

                    is BrowserSubPage.Placeholder -> {
                        SettingsPlaceholderPage()
                    }
                }
            } else {
                when (currentPage) {
                    SettingsPage.Root -> {
                        SettingsRootPage(
                            showBackButton = showBackButton,
                            onBackClick = ::handleBack,
                            onOpenPage = { currentPage = it }
                        )
                    }

                    SettingsPage.Browser -> {
                        BrowserSettingsDetailPage(
                            onOpenNavigation = { destination ->
                                browserSubPage = when (destination) {
                                    BrowserSettingsDestination.HomepageCustomization ->
                                        BrowserSubPage.HomepageCustomization

                                    BrowserSettingsDestination.X5Debug ->
                                        BrowserSubPage.X5Debug

                                    is BrowserSettingsDestination.Placeholder ->
                                        BrowserSubPage.Placeholder(destination.title)
                                }
                            }
                        )
                    }

                    SettingsPage.Player -> {
                        PlayerSettingsPage(
                            onOpenPlaybackSettings = {
                                launchActivity(
                                    Intent(context, PlaybackSettingsComposeActivity::class.java)
                                )
                            }
                        )
                    }

                    SettingsPage.Download -> {
                        DownloadSettingsContent()
                    }

                    SettingsPage.AdBlock -> {
                        AdBlockSettingsPage()
                    }

                    SettingsPage.ClipboardCode,
                    SettingsPage.MiniProgramManager,
                    SettingsPage.MiniProgramSubscription,
                    SettingsPage.BackupSync,
                    SettingsPage.Developer,
                    SettingsPage.Interface,
                    SettingsPage.More -> {
                        SettingsPlaceholderPage()
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowserSettingsDetailPage(
    onOpenNavigation: (BrowserSettingsDestination) -> Unit
) {
    val groups = remember {
        listOf(
            listOf(
                BrowserSettingsEntry.Navigation(
                    title = "网页插件管理",
                    destination = BrowserSettingsDestination.Placeholder("网页插件管理")
                ),
                BrowserSettingsEntry.Toggle(title = "悬浮嗅探播放", enabled = true),
                BrowserSettingsEntry.Navigation(
                    title = "悬浮嗅探模式",
                    destination = BrowserSettingsDestination.Placeholder("悬浮嗅探模式")
                ),
                BrowserSettingsEntry.Toggle(title = "返回不重载", enabled = false),
                BrowserSettingsEntry.Navigation(
                    title = "启动时恢复标签",
                    value = "不恢复",
                    destination = BrowserSettingsDestination.Placeholder("启动时恢复标签")
                )
            ),
            listOf(
                BrowserSettingsEntry.Navigation(
                    title = "网页主页自定义",
                    destination = BrowserSettingsDestination.HomepageCustomization
                ),
                BrowserSettingsEntry.Navigation(
                    title = "标签栏样式设置",
                    value = "图文卡片",
                    destination = BrowserSettingsDestination.Placeholder("标签栏样式设置")
                ),
                BrowserSettingsEntry.Toggle(title = "手势前进后退", enabled = true),
                BrowserSettingsEntry.Toggle(title = "底部上滑手势", enabled = true),
                BrowserSettingsEntry.Toggle(title = "搜索引擎切换条", enabled = true)
            ),
            listOf(
                BrowserSettingsEntry.Toggle(title = "音视频嗅探提示", enabled = true),
                BrowserSettingsEntry.Navigation(
                    title = "嗅探规则管理",
                    destination = BrowserSettingsDestination.Placeholder("嗅探规则管理")
                )
            ),
            listOf(
                BrowserSettingsEntry.Toggle(title = "允许网页打开应用", enabled = true),
                BrowserSettingsEntry.Toggle(title = "允许网页获取位置", enabled = true),
                BrowserSettingsEntry.Navigation(
                    title = "网页翻译接口",
                    value = "百度翻译",
                    destination = BrowserSettingsDestination.Placeholder("网页翻译接口")
                ),
                BrowserSettingsEntry.Navigation(
                    title = "网站配置管理",
                    destination = BrowserSettingsDestination.Placeholder("网站配置管理")
                ),
                BrowserSettingsEntry.Navigation(
                    title = "网站密码管理",
                    destination = BrowserSettingsDestination.Placeholder("网站密码管理")
                )
            ),
            listOf(
                BrowserSettingsEntry.Navigation(
                    title = "网页字体大小",
                    destination = BrowserSettingsDestination.Placeholder("网页字体大小")
                ),
                BrowserSettingsEntry.Toggle(title = "强制页面缩放", enabled = false),
                BrowserSettingsEntry.Navigation(
                    title = "腾讯X5调试",
                    destination = BrowserSettingsDestination.X5Debug
                ),
                BrowserSettingsEntry.Navigation(
                    title = "自定义UA设置",
                    destination = BrowserSettingsDestination.Placeholder("自定义UA设置")
                ),
                BrowserSettingsEntry.Navigation(
                    title = "浏览器代理替换",
                    destination = BrowserSettingsDestination.Placeholder("浏览器代理替换")
                ),
                BrowserSettingsEntry.Toggle(title = "强制新窗口打开", enabled = false)
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

        itemsIndexed(groups) { _, group ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SettingsCardBorderColor),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    group.forEachIndexed { index, entry ->
                        BrowserSettingsRow(
                            entry = entry,
                            onOpenNavigation = onOpenNavigation
                        )
                        if (index != group.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.6.dp)
                                    .background(Color(0xFFF2F2EE))
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun BrowserSettingsRow(
    entry: BrowserSettingsEntry,
    onOpenNavigation: (BrowserSettingsDestination) -> Unit
) {
    val clickableModifier = when (entry) {
        is BrowserSettingsEntry.Navigation -> Modifier.clickable {
            onOpenNavigation(entry.destination)
        }

        is BrowserSettingsEntry.Toggle -> Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(start = 18.dp, end = 14.dp, top = 18.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2B2B2B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        when (entry) {
            is BrowserSettingsEntry.Navigation -> {
                if (!entry.value.isNullOrBlank()) {
                    Text(
                        text = entry.value,
                        fontSize = 13.sp,
                        color = Color(0xFF9A9895),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(7.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFFBDBDB8),
                    modifier = Modifier.size(18.dp)
                )
            }

            is BrowserSettingsEntry.Toggle -> {
                BrowserSettingsIndicator(enabled = entry.enabled)
            }
        }
    }
}

@Composable
private fun SettingsHeaderBar(
    title: String,
    showBackButton: Boolean,
    showRootActions: Boolean,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F2))
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 5.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color(0xFF2B2B2B),
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
        } else {
            Spacer(modifier = Modifier.width(10.dp))
        }

        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF202020),
            modifier = Modifier.weight(1f)
        )

        if (showRootActions) {
            SettingsHeaderAction(icon = Icons.Default.Search)
            SettingsHeaderAction(icon = Icons.Default.GridView)
            SettingsHeaderAction(icon = Icons.Default.Refresh)
            SettingsHeaderAction(icon = Icons.Default.DarkMode)
        }
    }
}

@Composable
private fun SettingsHeaderAction(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF72726E),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SettingsHeaderAction(iconRes: Int) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color(0xFF72726E),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SettingsRootPage(
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    onOpenPage: (SettingsPage) -> Unit
) {
    val groups = remember {
        listOf(
            listOf(
                SettingsRootEntry(
                    title = "剪贴板口令",
                    icon = Icons.Default.ContentPaste,
                    iconTint = Color(0xFF46C785),
                    destination = SettingsPage.ClipboardCode
                ),
                SettingsRootEntry(
                    title = "小程序管理",
                    icon = Icons.Default.Apps,
                    iconTint = Color(0xFF59BCE8),
                    destination = SettingsPage.MiniProgramManager
                ),
                SettingsRootEntry(
                    title = "小程序订阅",
                    icon = Icons.Default.GridView,
                    iconTint = Color(0xFF5AA9EA),
                    destination = SettingsPage.MiniProgramSubscription
                )
            ),
            listOf(
                SettingsRootEntry(
                    title = "浏览器",
                    icon = Icons.Default.Language,
                    iconTint = Color(0xFF6A96F2),
                    destination = SettingsPage.Browser
                ),
                SettingsRootEntry(
                    title = "播放器",
                    icon = Icons.Default.PlayCircle,
                    iconTint = Color(0xFFF06E71),
                    destination = SettingsPage.Player
                ),
                SettingsRootEntry(
                    title = "下载",
                    icon = Icons.Default.Download,
                    iconTint = Color(0xFFF27D84),
                    destination = SettingsPage.Download
                ),
                SettingsRootEntry(
                    title = "广告拦截",
                    icon = Icons.Default.Block,
                    iconTint = Color(0xFF58C68E),
                    destination = SettingsPage.AdBlock
                )
            ),
            listOf(
                SettingsRootEntry(
                    title = "数据备份与同步",
                    icon = Icons.Default.Backup,
                    iconTint = Color(0xFF4FAEE9),
                    destination = SettingsPage.BackupSync
                ),
                SettingsRootEntry(
                    title = "开发手册与模式",
                    icon = Icons.Default.MenuBook,
                    iconTint = Color(0xFF5DBDCC),
                    destination = SettingsPage.Developer
                )
            ),
            listOf(
                SettingsRootEntry(
                    title = "界面定制",
                    icon = Icons.Default.Palette,
                    iconTint = Color(0xFFE575A5),
                    destination = SettingsPage.Interface
                ),
                SettingsRootEntry(
                    title = "更多功能",
                    icon = Icons.Default.Widgets,
                    iconTint = Color(0xFF5E9CEE),
                    destination = SettingsPage.More
                )
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SettingsRootScrollableHeader(
                title = SettingsPage.Root.title,
                showBackButton = showBackButton,
                onBackClick = onBackClick
            )
        }

        itemsIndexed(groups) { _, group ->
            SettingsRootGroupCard(
                entries = group,
                onEntryClick = { onOpenPage(it.destination) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SettingsRootScrollableHeader(
    title: String,
    showBackButton: Boolean,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F2))
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color(0xFF2B2B2B),
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
        } else {
            Spacer(modifier = Modifier.width(10.dp))
        }

        Text(
            text = title,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF202020),
            modifier = Modifier.weight(1f)
        )

        SettingsHeaderAction(iconRes = R.drawable.ic_kiyori_settings_header_top_search)
        SettingsHeaderAction(iconRes = R.drawable.ic_kiyori_settings_header_scan)
        SettingsHeaderAction(iconRes = R.drawable.ic_kiyori_settings_header_top_refresh)
        SettingsHeaderAction(iconRes = R.drawable.ic_kiyori_settings_header_sun)
    }
}

@Composable
private fun SettingsRootGroupCard(
    entries: List<SettingsRootEntry>,
    onEntryClick: (SettingsRootEntry) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SettingsCardBorderColor),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            entries.forEachIndexed { index, entry ->
                SettingsRootRow(
                    entry = entry,
                    onClick = { onEntryClick(entry) }
                )
                if (index != entries.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.6.dp)
                            .background(Color(0xFFF2F2EE))
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRootRow(
    entry: SettingsRootEntry,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = entry.title,
                tint = entry.iconTint,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = entry.title,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2B2B2B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFBDBDB8),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsDetailPage(
    groups: List<List<SettingsDetailEntry>>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        itemsIndexed(groups) { _, group ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SettingsCardBorderColor),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    group.forEachIndexed { index, entry ->
                        SettingsDetailRow(entry = entry)
                        if (index != group.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.6.dp)
                                    .background(Color(0xFFF2F2EE))
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SettingsDetailRow(
    entry: SettingsDetailEntry
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = entry.onClick)
            .padding(start = 18.dp, end = 15.dp, top = 17.dp, bottom = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.title,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2B2B2B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (!entry.value.isNullOrBlank()) {
            Text(
                text = entry.value,
                fontSize = 12.5.sp,
                color = Color(0xFF94948F),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFBDBDB8),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsPlaceholderPage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2))
    )
}

@Composable
private fun BrowserHomepageCustomizationPage(
    currentHomeUrl: String,
    onSave: (String) -> Boolean,
    onReset: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SettingsCardBorderColor),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsDetailRow(
                        entry = SettingsDetailEntry(
                            title = "自定义主页入口",
                            value = currentHomeUrl,
                            onClick = { showEditDialog = true }
                        )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.6.dp)
                            .background(Color(0xFFF2F2EE))
                    )
                    SettingsDetailRow(
                        entry = SettingsDetailEntry(
                            title = "恢复为空白页",
                            value = "about:blank",
                            onClick = onReset
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showEditDialog) {
        BrowserHomepageEditDialog(
            initialValue = currentHomeUrl,
            onDismiss = { showEditDialog = false },
            onConfirm = { value ->
                if (onSave(value)) {
                    showEditDialog = false
                }
            }
        )
    }
}

@Composable
private fun BrowserHomepageEditDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inputValue by remember(initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "自定义主页入口",
                    color = Color(0xFF111111),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "设置后，浏览器点击主页会直接打开这里配置的地址。",
                    color = Color(0xFF666666),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "入口地址",
                    color = Color(0xFF111111),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    label = { Text("自定义主页入口") },
                    placeholder = { Text("输入网址或 about:blank") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF111111),
                        unfocusedBorderColor = Color(0xFFD6D6D6),
                        focusedLabelColor = Color(0xFF111111),
                        unfocusedLabelColor = Color(0xFF666666),
                        cursorColor = Color(0xFF111111),
                        focusedTextColor = Color(0xFF111111),
                        unfocusedTextColor = Color(0xFF111111),
                        focusedPlaceholderColor = Color(0xFF9B9B9B),
                        unfocusedPlaceholderColor = Color(0xFF9B9B9B)
                    )
                )
                Text(
                    text = "支持完整网址，也可以直接填写 about:blank 作为空白页。",
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(inputValue.trim()) }) {
                Text(
                    "保存",
                    color = Color(0xFF111111),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "取消",
                    color = Color(0xFF777777),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun BrowserSettingsIndicator(
    enabled: Boolean
) {
    Box(
        modifier = Modifier
            .size(17.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (enabled) Color(0xFFCBBEFF) else Color.White)
            .border(
                width = if (enabled) 0.dp else 1.dp,
                color = if (enabled) Color.Transparent else Color(0xFFBAB4AE),
                shape = RoundedCornerShape(3.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (enabled) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun DownloadSettingsPage() {
    val groups = remember {
        listOf(
            listOf(
                DownloadSettingsEntry.Navigation(title = "自定义下载器", value = "IDM+"),
                DownloadSettingsEntry.Navigation(title = "同时下载任务数", value = "6"),
                DownloadSettingsEntry.Navigation(title = "普通格式下载线程数", value = "12"),
                DownloadSettingsEntry.Navigation(title = "M3U8下载线程数", value = "16")
            ),
            listOf(
                DownloadSettingsEntry.Toggle(title = "M3U8自动合并", enabled = true),
                DownloadSettingsEntry.Toggle(title = "自动转存公开目录", enabled = true),
                DownloadSettingsEntry.Navigation(title = "自定义下载分块大小")
            ),
            listOf(
                DownloadSettingsEntry.Toggle(title = "安装包自动清理", enabled = false),
                DownloadSettingsEntry.Toggle(title = "下载无需弹窗确认", enabled = false),
                DownloadSettingsEntry.Navigation(title = "下载完成强提示")
            ),
            listOf(
                DownloadSettingsEntry.Navigation(title = "切换下载协议")
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(2.dp))
        }

        item {
            Text(
                text = "下载器及自定义",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF202020),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp)
            )
        }

        itemsIndexed(groups) { _, group ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SettingsCardBorderColor),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    group.forEachIndexed { index, entry ->
                        DownloadSettingsRow(entry = entry)
                        if (index != group.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.6.dp)
                                    .background(Color(0xFFF2F2EE))
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DownloadSettingsRow(
    entry: DownloadSettingsEntry
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 14.dp, top = 18.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2B2B2B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        when (entry) {
            is DownloadSettingsEntry.Navigation -> {
                if (!entry.value.isNullOrBlank()) {
                    Text(
                        text = entry.value,
                        fontSize = 13.sp,
                        color = Color(0xFF9A9895),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(7.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFFBDBDB8),
                    modifier = Modifier.size(18.dp)
                )
            }

            is DownloadSettingsEntry.Toggle -> {
                BrowserSettingsIndicator(enabled = entry.enabled)
            }
        }
    }
}

@Composable
private fun PlayerSettingsPage(
    onOpenPlaybackSettings: () -> Unit
) {
    val groups = remember {
        listOf(
            listOf(
                PlayerSettingsEntry.Navigation(title = "播放设置", onClick = onOpenPlaybackSettings),
                PlayerSettingsEntry.Navigation(title = "极速播放模式2.0"),
                PlayerSettingsEntry.Navigation(title = "自定义播放器"),
                PlayerSettingsEntry.Navigation(title = "备用播放器"),
                PlayerSettingsEntry.Navigation(title = "跳过片头片尾")
            ),
            listOf(
                PlayerSettingsEntry.Navigation(title = "小窗模式"),
                PlayerSettingsEntry.Toggle(title = "AI全屏显示", enabled = true),
                PlayerSettingsEntry.Navigation(title = "直接全屏播放/返回"),
                PlayerSettingsEntry.Toggle(title = "重力感应自动横屏", enabled = true),
                PlayerSettingsEntry.Toggle(title = "双指捏合缩放", enabled = true)
            ),
            listOf(
                PlayerSettingsEntry.Toggle(title = "与其他应用同时播放", enabled = true),
                PlayerSettingsEntry.Toggle(title = "流量网络下自动播放", enabled = false),
                PlayerSettingsEntry.Navigation(title = "蓝牙断开自动暂停", value = "仅音乐"),
                PlayerSettingsEntry.Toggle(title = "非Wifi网络提示", enabled = true),
                PlayerSettingsEntry.Toggle(title = "音乐失败自动下一曲", enabled = false),
                PlayerSettingsEntry.Navigation(title = "视频播放跳转"),
                PlayerSettingsEntry.Toggle(title = "视频播放完自动返回", enabled = true),
                PlayerSettingsEntry.Navigation(title = "投屏复制链接")
            ),
            listOf(
                PlayerSettingsEntry.Navigation(title = "倍速记忆设置"),
                PlayerSettingsEntry.Navigation(title = "长按倍速设置"),
                PlayerSettingsEntry.Navigation(title = "双击快进快退", value = "10s"),
                PlayerSettingsEntry.Toggle(title = "全局底部进度条", enabled = false)
            ),
            listOf(
                PlayerSettingsEntry.Toggle(title = "隧道播放模式", enabled = true),
                PlayerSettingsEntry.Navigation(title = "清除播放进度"),
                PlayerSettingsEntry.Navigation(title = "自定义投屏")
            ),
            listOf(
                PlayerSettingsEntry.Navigation(title = "M3U8广告清除")
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(2.dp))
        }

        item {
            Text(
                text = "播放器相关设置",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF202020),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp)
            )
        }

        itemsIndexed(groups) { _, group ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SettingsCardBorderColor),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    group.forEachIndexed { index, entry ->
                        PlayerSettingsRow(entry = entry)
                        if (index != group.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.6.dp)
                                    .background(Color(0xFFF2F2EE))
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PlayerSettingsRow(
    entry: PlayerSettingsEntry
) {
    val clickableModifier = when (entry) {
        is PlayerSettingsEntry.Navigation -> Modifier.clickable(onClick = entry.onClick)
        is PlayerSettingsEntry.Toggle -> Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(start = 18.dp, end = 14.dp, top = 18.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2B2B2B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        when (entry) {
            is PlayerSettingsEntry.Navigation -> {
                if (!entry.value.isNullOrBlank()) {
                    Text(
                        text = entry.value,
                        fontSize = 13.sp,
                        color = Color(0xFF9A9895),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(7.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFFBDBDB8),
                    modifier = Modifier.size(18.dp)
                )
            }

            is PlayerSettingsEntry.Toggle -> {
                BrowserSettingsIndicator(enabled = entry.enabled)
            }
        }
    }
}

@Composable
private fun AdBlockSettingsPage() {
    val groups = remember {
        listOf(
            listOf(
                AdBlockSettingsEntry.Toggle(title = "广告拦截开关", enabled = true),
                AdBlockSettingsEntry.Navigation(title = "网址过滤拦截"),
                AdBlockSettingsEntry.Navigation(title = "网页元素拦截"),
                AdBlockSettingsEntry.Navigation(title = "广告拦截订阅"),
                AdBlockSettingsEntry.Navigation(title = "广告拦截器 Pro"),
                AdBlockSettingsEntry.Navigation(title = "Adblock Plus订阅")
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(2.dp))
        }

        item {
            Text(
                text = "广告拦截与订阅",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF202020),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp)
            )
        }

        itemsIndexed(groups) { _, group ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SettingsCardBorderColor),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    group.forEachIndexed { index, entry ->
                        AdBlockSettingsRow(entry = entry)
                        if (index != group.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.6.dp)
                                    .background(Color(0xFFF2F2EE))
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AdBlockSettingsRow(
    entry: AdBlockSettingsEntry
) {
    val clickableModifier = when (entry) {
        is AdBlockSettingsEntry.Navigation -> Modifier.clickable { }
        is AdBlockSettingsEntry.Toggle -> Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(start = 18.dp, end = 14.dp, top = 18.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2B2B2B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        when (entry) {
            is AdBlockSettingsEntry.Navigation -> {
                Spacer(modifier = Modifier.width(7.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFFBDBDB8),
                    modifier = Modifier.size(18.dp)
                )
            }

            is AdBlockSettingsEntry.Toggle -> {
                BrowserSettingsIndicator(enabled = entry.enabled)
            }
        }
    }
}

private enum class SettingsPage(val title: String) {
    Root("设置"),
    ClipboardCode("剪贴板口令"),
    MiniProgramManager("小程序管理"),
    MiniProgramSubscription("小程序订阅"),
    Browser("浏览器"),
    Player("播放器"),
    Download("下载"),
    AdBlock("广告拦截"),
    BackupSync("数据备份与同步"),
    Developer("开发手册与模式"),
    Interface("界面定制"),
    More("更多功能")
}

private fun settingsPageFromKey(key: String?): SettingsPage? = when (key) {
    "browser" -> SettingsPage.Browser
    "player" -> SettingsPage.Player
    "download" -> SettingsPage.Download
    "ad_block" -> SettingsPage.AdBlock
    else -> null
}

private data class SettingsRootEntry(
    val title: String,
    val icon: ImageVector,
    val iconTint: Color,
    val destination: SettingsPage
)

private data class SettingsDetailEntry(
    val title: String,
    val value: String?,
    val onClick: () -> Unit
)

private sealed interface BrowserSettingsEntry {
    val title: String

    data class Navigation(
        override val title: String,
        val value: String? = null,
        val destination: BrowserSettingsDestination
    ) : BrowserSettingsEntry

    data class Toggle(
        override val title: String,
        val enabled: Boolean
    ) : BrowserSettingsEntry
}

private sealed interface DownloadSettingsEntry {
    val title: String

    data class Navigation(
        override val title: String,
        val value: String? = null
    ) : DownloadSettingsEntry

    data class Toggle(
        override val title: String,
        val enabled: Boolean
    ) : DownloadSettingsEntry
}

private sealed interface PlayerSettingsEntry {
    val title: String

    data class Navigation(
        override val title: String,
        val value: String? = null,
        val onClick: () -> Unit = {}
    ) : PlayerSettingsEntry

    data class Toggle(
        override val title: String,
        val enabled: Boolean
    ) : PlayerSettingsEntry
}

private sealed interface AdBlockSettingsEntry {
    val title: String

    data class Navigation(
        override val title: String
    ) : AdBlockSettingsEntry

    data class Toggle(
        override val title: String,
        val enabled: Boolean
    ) : AdBlockSettingsEntry
}

private sealed interface BrowserSettingsDestination {
    data object HomepageCustomization : BrowserSettingsDestination
    data object X5Debug : BrowserSettingsDestination

    data class Placeholder(
        val title: String
    ) : BrowserSettingsDestination
}

private sealed interface BrowserSubPage {
    val title: String

    data object HomepageCustomization : BrowserSubPage {
        override val title: String = "网页主页自定义"
    }

    data object X5Debug : BrowserSubPage {
        override val title: String = "腾讯X5调试"
    }

    data class Placeholder(
        override val title: String
    ) : BrowserSubPage
}
