package com.android.kiyori.remote.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.kiyori.R
import com.android.kiyori.app.BaseActivity
import com.android.kiyori.ui.compose.ImmersiveTopAppBar
import com.android.kiyori.manager.PreferencesManager
import com.android.kiyori.remote.RemotePlaybackHeaders
import com.android.kiyori.remote.RemotePlaybackHistoryRepository
import com.android.kiyori.remote.RemotePlaybackLauncher
import com.android.kiyori.remote.RemotePlaybackRequest
import com.android.kiyori.remote.RemoteUrlParser
import com.android.kiyori.ui.theme.getThemeColors
import com.android.kiyori.utils.ThemeManager

class RemotePlaybackInputActivity : BaseActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, RemotePlaybackInputActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                    surfaceVariant = Color(0xFFF5F7FA),
                    onSurface = Color(0xFF111827)
                )
            ) {
                RemotePlaybackInputScreen(
                    onBack = {
                        finish()
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    },
                    onPlayRequest = { request ->
                        RemotePlaybackLauncher.start(this, request)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemotePlaybackInputScreen(
    onBack: () -> Unit,
    onPlayRequest: (RemotePlaybackRequest) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferencesManager = remember(context) { PreferencesManager.getInstance(context) }
    val historyRepository = remember(context) { RemotePlaybackHistoryRepository(context) }
    val initialSearchHistoryEnabled = remember { historyRepository.isSearchHistoryEnabled() }
    val initialDebugHistoryEnabled = remember { historyRepository.isDebugHistoryEnabled() }

    var searchHistoryEnabled by remember { mutableStateOf(initialSearchHistoryEnabled) }
    var debugHistoryEnabled by remember { mutableStateOf(initialDebugHistoryEnabled) }
    var url by remember {
        mutableStateOf(
            if (initialSearchHistoryEnabled) "" else preferencesManager.getLastRemoteInputUrl()
        )
    }
    var title by remember {
        mutableStateOf(
            if (initialSearchHistoryEnabled) "" else preferencesManager.getLastRemoteInputTitle()
        )
    }
    var sourcePageUrl by remember {
        mutableStateOf(
            if (initialSearchHistoryEnabled) "" else preferencesManager.getLastRemoteInputSourcePageUrl()
        )
    }
    var referer by remember {
        mutableStateOf(
            if (initialSearchHistoryEnabled) "" else preferencesManager.getLastRemoteInputReferer()
        )
    }
    var origin by remember {
        mutableStateOf(
            if (initialSearchHistoryEnabled) "" else preferencesManager.getLastRemoteInputOrigin()
        )
    }
    var cookie by remember {
        mutableStateOf(
            if (initialSearchHistoryEnabled) "" else preferencesManager.getLastRemoteInputCookie()
        )
    }
    var authorization by remember {
        mutableStateOf(
            if (initialSearchHistoryEnabled) "" else preferencesManager.getLastRemoteInputAuthorization()
        )
    }
    var userAgent by remember {
        mutableStateOf(
            if (initialSearchHistoryEnabled) "" else preferencesManager.getLastRemoteInputUserAgent()
        )
    }
    val hasInputContent = remember(
        url,
        title,
        sourcePageUrl,
        referer,
        origin,
        cookie,
        authorization,
        userAgent
    ) {
        listOf(url, title, sourcePageUrl, referer, origin, cookie, authorization, userAgent)
            .any { it.isNotBlank() }
    }
    var showAdvanced by remember {
        mutableStateOf(
            listOf(sourcePageUrl, referer, origin, cookie, authorization, userAgent)
                .any { it.isNotBlank() }
        )
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color.White,
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        disabledTextColor = Color(0xFF9CA3AF),
        focusedBorderColor = Color(0xFF1F2937),
        unfocusedBorderColor = Color(0xFF9CA3AF),
        disabledBorderColor = Color(0xFFD1D5DB),
        focusedLabelColor = Color(0xFF111827),
        unfocusedLabelColor = Color(0xFF4B5563),
        focusedPlaceholderColor = Color(0xFF6B7280),
        unfocusedPlaceholderColor = Color(0xFF6B7280),
        cursorColor = Color.Black
    )

    Scaffold(
        topBar = {
            ImmersiveTopAppBar(
                title = {
                    Text(
                        text = "播放网络视频",
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
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                        modifier = Modifier.weight(1f),
                        enabled = hasInputContent
                    ) {
                        Text("清空")
                    }
                    Button(
                        onClick = {
                            val request = buildRemotePlaybackRequest(
                                preferencesManager = preferencesManager,
                                historyRepository = historyRepository,
                                searchHistoryEnabled = searchHistoryEnabled,
                                rawUrl = url,
                                title = title,
                                sourcePageUrl = sourcePageUrl,
                                referer = referer,
                                origin = origin,
                                cookie = cookie,
                                authorization = authorization,
                                userAgent = userAgent
                            )
                            onPlayRequest(request)
                        },
                        modifier = Modifier.weight(1.45f),
                        enabled = url.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("立即播放")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RemoteInputSection(
                icon = Icons.Default.Link,
                title = "播放信息"
            ) {
                Text(
                    text = "将地址粘贴到下方即可。标题可选，仅用于播放器内展示。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("视频地址或播放串") },
                    placeholder = { Text("https://example.com/video.mp4 或 curl 请求") },
                    minLines = 4,
                    maxLines = 8,
                    colors = fieldColors,
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("标题（可选）") },
                    placeholder = { Text("不填写则按链接或站点标题显示") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(18.dp)
                )
            }

            RemoteInputSection(
                icon = Icons.Default.Tune,
                title = "记录与保存"
            ) {
                Text(
                    text = "搜索记录用于保存你手动输入过的播放地址；调试记录用于保存远程解析时生成的调试信息，两者互不影响。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
                Spacer(modifier = Modifier.height(12.dp))
                HistoryToggleItem(
                    title = "搜索记录",
                    description = if (searchHistoryEnabled) {
                        "开启后，新的输入会单独保存到搜索记录页，并可直接再次播放。"
                    } else {
                        "关闭后，输入内容仅作为临时草稿保留在当前页面。"
                    },
                    checked = searchHistoryEnabled,
                    onCheckedChange = { enabled ->
                        searchHistoryEnabled = enabled
                        historyRepository.setSearchHistoryEnabled(enabled)
                        if (enabled) {
                            preferencesManager.clearLastRemoteInputDraft()
                        }
                    },
                    onOpenHistory = {
                        RemotePlaybackHistoryActivity.startSearch(context)
                    }
                )
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color(0xFFE5E7EB)
                )
                HistoryToggleItem(
                    title = "调试记录",
                    description = if (debugHistoryEnabled) {
                        "开启后，播放器解析网络视频时会自动保存调试摘要，便于排查站点问题。"
                    } else {
                        "关闭后，不再额外保存新的调试记录。"
                    },
                    checked = debugHistoryEnabled,
                    onCheckedChange = { enabled ->
                        debugHistoryEnabled = enabled
                        historyRepository.setDebugHistoryEnabled(enabled)
                    },
                    onOpenHistory = {
                        RemotePlaybackHistoryActivity.startDebug(context)
                    }
                )
            }

            FilledTonalButton(
                onClick = { showAdvanced = !showAdvanced },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.Tune,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (showAdvanced) "收起高级请求头" else "展开高级请求头")
            }

            if (showAdvanced) {
                RemoteInputSection(
                    icon = Icons.Default.Security,
                    title = "高级请求头"
                ) {
                    Text(
                        text = "仅在站点存在防盗链、登录校验或 UA 限制时填写。不确定时建议先留空测试，出现打不开再逐项补充。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = sourcePageUrl,
                        onValueChange = { sourcePageUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("来源页 URL（可选）") },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(18.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = referer,
                        onValueChange = { referer = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Referer（可选）") },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(18.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = origin,
                        onValueChange = { origin = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Origin（可选）") },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(18.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cookie,
                        onValueChange = { cookie = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Cookie（可选）") },
                        minLines = 2,
                        maxLines = 5,
                        colors = fieldColors,
                        shape = RoundedCornerShape(18.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = authorization,
                        onValueChange = { authorization = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Authorization（可选）") },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(18.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = userAgent,
                        onValueChange = { userAgent = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("User-Agent（可选）") },
                        minLines = 2,
                        maxLines = 4,
                        colors = fieldColors,
                        shape = RoundedCornerShape(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RemoteInputSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }
            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFFE5E7EB)
            )
            content()
        }
    }
}

@Composable
private fun HistoryToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpenHistory: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color(0xFF111827),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color(0xFF6B7280),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = onOpenHistory) {
            Text("查看记录")
        }
    }
}

private fun buildRemotePlaybackRequest(
    preferencesManager: PreferencesManager,
    historyRepository: RemotePlaybackHistoryRepository,
    searchHistoryEnabled: Boolean,
    rawUrl: String,
    title: String,
    sourcePageUrl: String,
    referer: String,
    origin: String,
    cookie: String,
    authorization: String,
    userAgent: String
): RemotePlaybackRequest {
    val normalizedRawUrl = rawUrl.trim()
    val parsedInput = RemoteUrlParser.parsePlaybackInput(normalizedRawUrl)
    val normalizedTitle = title.trim()
    val normalizedReferer = referer.trim()
    val normalizedSourcePageUrl = sourcePageUrl.trim().ifBlank { normalizedReferer }
    val headers = linkedMapOf<String, String>().apply {
        putAll(parsedInput?.headers.orEmpty())
    }

    if (normalizedReferer.isNotBlank()) {
        headers["Referer"] = normalizedReferer
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

    if (searchHistoryEnabled) {
        historyRepository.addSearchHistory(
            url = normalizedRawUrl,
            title = normalizedTitle,
            sourcePageUrl = normalizedSourcePageUrl
        )
        preferencesManager.clearLastRemoteInputDraft()
    } else {
        preferencesManager.setLastRemoteInputUrl(normalizedRawUrl)
        preferencesManager.setLastRemoteInputTitle(normalizedTitle)
        preferencesManager.setLastRemoteInputSourcePageUrl(normalizedSourcePageUrl)
        preferencesManager.setLastRemoteInputReferer(normalizedReferer)
        preferencesManager.setLastRemoteInputOrigin(origin.trim())
        preferencesManager.setLastRemoteInputCookie(cookie.trim())
        preferencesManager.setLastRemoteInputAuthorization(authorization.trim())
        preferencesManager.setLastRemoteInputUserAgent(userAgent.trim())
    }

    return RemotePlaybackRequest(
        url = parsedInput?.url ?: normalizedRawUrl,
        title = normalizedTitle,
        sourcePageUrl = normalizedSourcePageUrl,
        headers = RemotePlaybackHeaders.normalize(headers),
        source = RemotePlaybackRequest.Source.DIRECT_INPUT
    )
}

