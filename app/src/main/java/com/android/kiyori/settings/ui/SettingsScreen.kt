package com.android.kiyori.settings.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.kiyori.R
import com.android.kiyori.browser.data.BrowserPreferencesRepository
import com.android.kiyori.browser.security.BrowserSecurityPolicy
import com.android.kiyori.bilibili.auth.BiliBiliAuthManager
import com.android.kiyori.ui.compose.ImmersiveTopAppBar
import com.android.kiyori.danmaku.ui.BiliBiliDanmakuComposeActivity
import com.android.kiyori.download.ui.DownloadActivity
import com.android.kiyori.history.ui.PlaybackHistoryComposeActivity
import com.android.kiyori.settings.AboutComposeActivity
import com.android.kiyori.settings.PlaybackSettingsComposeActivity
import com.android.kiyori.subtitle.ui.SubtitleSearchActivity
import com.android.kiyori.utils.ThemeManager
import com.android.kiyori.ui.compose.SettingsColors as SettingsPalette

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    showBackButton: Boolean = true
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val authManager = remember { BiliBiliAuthManager.getInstance(context) }
    val browserPreferencesRepository = remember { BrowserPreferencesRepository(context) }
    val currentTheme = remember { mutableStateOf(ThemeManager.getCurrentTheme(context)) }
    var browserHomeUrl by remember { mutableStateOf(browserPreferencesRepository.getHomeUrl()) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(SettingsPage.Root) }
    var browserSubPage by remember { mutableStateOf<BrowserSubPage?>(null) }

    fun launchActivity(intent: Intent) {
        context.startActivity(intent)
        activity?.overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }

    fun withBiliLogin(action: () -> Unit) {
        if (authManager.isLoggedIn()) {
            action()
        } else {
            Toast.makeText(
                context,
                "请先在文件管理页完成 B 站登录",
                Toast.LENGTH_SHORT
            ).show()
        }
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

    Scaffold(
        containerColor = Color(0xFFF5F5F2),
        topBar = {
            SettingsHeaderBar(
                title = browserSubPage?.title ?: if (currentPage == SettingsPage.Browser) "浏览器功能设置" else currentPage.title,
                showBackButton = browserSubPage != null || currentPage != SettingsPage.Root || showBackButton,
                showRootActions = currentPage == SettingsPage.Root && browserSubPage == null,
                onBackClick = ::handleBack
            )
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
                                        "主页地址格式无效",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@BrowserHomepageCustomizationPage false
                                }

                                browserPreferencesRepository.setHomeUrl(resolvedUrl)
                                browserHomeUrl = resolvedUrl
                                Toast.makeText(
                                    context,
                                    "主页地址已保存",
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
                        SettingsDetailPage(
                            groups = listOf(
                                listOf(
                                    SettingsDetailEntry(
                                        title = "播放设置",
                                        value = null,
                                        onClick = {
                                            launchActivity(
                                                Intent(context, PlaybackSettingsComposeActivity::class.java)
                                            )
                                        }
                                    ),
                                    SettingsDetailEntry(
                                        title = "播放历史记录",
                                        value = null,
                                        onClick = {
                                            launchActivity(
                                                Intent(context, PlaybackHistoryComposeActivity::class.java)
                                            )
                                        }
                                    )
                                )
                            )
                        )
                    }

                    SettingsPage.Download -> {
                        SettingsDetailPage(
                            groups = listOf(
                                listOf(
                                    SettingsDetailEntry(
                                        title = "B 站视频下载",
                                        value = null,
                                        onClick = {
                                            withBiliLogin {
                                                launchActivity(Intent(context, DownloadActivity::class.java))
                                            }
                                        }
                                    ),
                                    SettingsDetailEntry(
                                        title = "B 站弹幕下载",
                                        value = null,
                                        onClick = {
                                            withBiliLogin {
                                                launchActivity(
                                                    Intent(context, BiliBiliDanmakuComposeActivity::class.java)
                                                )
                                            }
                                        }
                                    ),
                                    SettingsDetailEntry(
                                        title = "字幕搜索下载",
                                        value = null,
                                        onClick = {
                                            launchActivity(
                                                Intent(context, SubtitleSearchActivity::class.java)
                                            )
                                        }
                                    )
                                )
                            )
                        )
                    }

                    SettingsPage.Developer -> {
                        SettingsDetailPage(
                            groups = listOf(
                                listOf(
                                    SettingsDetailEntry(
                                        title = "使用说明",
                                        value = null,
                                        onClick = {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://www.kdocs.cn/l/cjEzoxiyxaHT")
                                                )
                                            )
                                        }
                                    ),
                                    SettingsDetailEntry(
                                        title = "开发者模式",
                                        value = null,
                                        onClick = {
                                            currentPage = SettingsPage.DeveloperMode
                                        }
                                    )
                                )
                            )
                        )
                    }

                    SettingsPage.Interface -> {
                        SettingsDetailPage(
                            groups = listOf(
                                listOf(
                                    SettingsDetailEntry(
                                        title = "主题设置",
                                        value = currentTheme.value.themeName,
                                        onClick = { showThemeDialog = true }
                                    )
                                )
                            )
                        )
                    }

                    SettingsPage.More -> {
                        SettingsDetailPage(
                            groups = listOf(
                                listOf(
                                    SettingsDetailEntry(
                                        title = "关于",
                                        value = null,
                                        onClick = {
                                            launchActivity(Intent(context, AboutComposeActivity::class.java))
                                        }
                                    )
                                )
                            )
                        )
                    }

                    SettingsPage.ClipboardCode,
                    SettingsPage.MiniProgramManager,
                    SettingsPage.MiniProgramSubscription,
                    SettingsPage.AdBlock,
                    SettingsPage.BackupSync,
                    SettingsPage.DeveloperMode -> {
                        SettingsPlaceholderPage()
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentTheme.value,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { theme ->
                ThemeManager.setTheme(context, theme)
                currentTheme.value = theme
                showThemeDialog = false
                activity?.recreate()
            }
        )
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
            .size(30.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF72726E),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsRootPage(
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
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
private fun SettingsRootGroupCard(
    entries: List<SettingsRootEntry>,
    onEntryClick: (SettingsRootEntry) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp),
        shape = RoundedCornerShape(16.dp),
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsDetailRow(
                        entry = SettingsDetailEntry(
                            title = "主页地址",
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
        title = {
            Text(
                text = "网页主页自定义",
                color = SettingsPalette.PrimaryText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    label = { Text("主页地址") },
                    placeholder = { Text("输入网址或 about:blank") }
                )
                Text(
                    text = "保存后，浏览器点击主页时会直接打开这里设置的地址。",
                    fontSize = 12.sp,
                    color = SettingsPalette.SecondaryText
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(inputValue.trim()) }) {
                Text("保存", color = SettingsPalette.AccentText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = SettingsPalette.SecondaryText)
            }
        },
        shape = RoundedCornerShape(16.dp)
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
private fun ThemeSelectionDialog(
    currentTheme: ThemeManager.Theme,
    onDismiss: () -> Unit,
    onThemeSelected: (ThemeManager.Theme) -> Unit
) {
    var selectedTheme by remember { mutableStateOf(currentTheme) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择主题",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SettingsPalette.PrimaryText
            )
        },
        text = {
            Column {
                ThemeManager.Theme.values().forEach { theme ->
                    ThemeOption(
                        theme = theme,
                        isSelected = selectedTheme == theme,
                        onSelect = { selectedTheme = theme }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onThemeSelected(selectedTheme) }) {
                Text("确定", color = SettingsPalette.AccentText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = SettingsPalette.SecondaryText)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun ThemeOption(
    theme: ThemeManager.Theme,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = theme.themeName,
            fontSize = 16.sp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                SettingsPalette.PrimaryText
            }
        )
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
    DeveloperMode("开发者模式"),
    Interface("界面定制"),
    More("更多功能")
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
