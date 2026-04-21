package com.android.kiyori.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
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
import com.android.kiyori.history.PlaybackHistoryManager
import com.android.kiyori.history.ui.PlaybackHistoryComposeActivity
import com.android.kiyori.manager.PreferencesManager
import com.android.kiyori.media.VideoFileParcelable
import com.android.kiyori.media.ui.LocalMediaBrowserActivity
import com.android.kiyori.bilibili.ui.BiliBiliPlayActivity
import com.android.kiyori.player.ui.VideoPlayerActivity
import com.android.kiyori.remote.RemotePlaybackHeaders
import com.android.kiyori.remote.RemotePlaybackLauncher
import com.android.kiyori.remote.RemotePlaybackRequest
import com.android.kiyori.remote.RemoteUrlParser
import com.android.kiyori.remote.ui.RemotePlaybackInputActivity
import com.android.kiyori.browser.ui.BrowserActivity
import com.android.kiyori.webdav.WebDavComposeActivity
import com.android.kiyori.manager.compose.BiliBiliLoginActivity
import com.android.kiyori.settings.ui.SettingsScreen

/**
 * Compose 版本的主页
 */
@Composable
fun HomeScreen(
    historyManager: PlaybackHistoryManager,
    initialTab: String? = null
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.fromRoute(initialTab) ?: HomeTab.Home) }

    LaunchedEffect(initialTab) {
        HomeTab.fromRoute(initialTab)?.let { selectedTab = it }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        bottomBar = {
            HomeBottomBar(
                selectedTab = selectedTab,
                onTabSelected = {
                    when (it) {
                        HomeTab.Browser -> {
                            BrowserActivity.start(context)
                            (context as? android.app.Activity)?.overridePendingTransition(
                                R.anim.no_anim,
                                R.anim.no_anim
                            )
                        }
                        else -> {
                            selectedTab = it
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            when (selectedTab) {
                HomeTab.Home -> HomeLandingPage(
                    onSearchClick = {
                        BrowserActivity.start(context, openSearch = true)
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.no_anim,
                            R.anim.no_anim
                        )
                    },
                    onAiClick = { selectedTab = HomeTab.AI }
                )

                HomeTab.Files -> FileManagementPage(
                    onLocalVideoClick = {
                        context.startActivity(Intent(context, LocalMediaBrowserActivity::class.java))
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                    },
                    onPlaybackHistoryClick = {
                        context.startActivity(Intent(context, PlaybackHistoryComposeActivity::class.java))
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                    },
                    onLoginClick = {
                        context.startActivity(Intent(context, BiliBiliLoginActivity::class.java))
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                    },
                    onRemoteVideoClick = {
                        RemotePlaybackInputActivity.start(context)
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                    },
                    onBiliBiliClick = {
                        context.startActivity(Intent(context, BiliBiliPlayActivity::class.java))
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                    },
                    onWebDavClick = {
                        context.startActivity(Intent(context, WebDavComposeActivity::class.java))
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                    }
                )

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
    onLocalVideoClick: () -> Unit,
    onPlaybackHistoryClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRemoteVideoClick: () -> Unit,
    onBiliBiliClick: () -> Unit,
    onWebDavClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HomeQuickActions(
            onLocalVideoClick = onLocalVideoClick,
            onPlaybackHistoryClick = onPlaybackHistoryClick,
            onLoginClick = onLoginClick,
            onRemoteVideoClick = onRemoteVideoClick,
            onBiliBiliClick = onBiliBiliClick,
            onWebDavClick = onWebDavClick
        )

        Spacer(modifier = Modifier.weight(1f))
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
    onPlaybackHistoryClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRemoteVideoClick: () -> Unit,
    onBiliBiliClick: () -> Unit,
    onWebDavClick: () -> Unit
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
            HomeQuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.History,
                label = "播放记录",
                onClick = onPlaybackHistoryClick,
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeQuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.PlayCircle,
                label = "网络视频",
                onClick = onRemoteVideoClick
            )
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

