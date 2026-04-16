package com.fam4k007.videoplayer.browser.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fam4k007.videoplayer.browser.domain.BrowserPageState
import com.fam4k007.videoplayer.browser.domain.BrowserSearchEngine
import com.fam4k007.videoplayer.browser.domain.BrowserSearchRecord
import com.fam4k007.videoplayer.browser.security.BrowserSecurityPolicy

@Composable
fun BrowserTopBar(
    state: BrowserPageState,
    detectedVideoCount: Int,
    onBackPressed: () -> Unit,
    onToggleUrlBar: () -> Unit,
    onReload: () -> Unit,
    onShowDetectedVideos: () -> Unit,
    onPlayBestVideo: () -> Unit
) {
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradientBrush)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrowserBarIcon(
                icon = Icons.Default.ArrowBack,
                contentDescription = "返回",
                onClick = onBackPressed
            )
            Text(
                text = if (state.isBlankPage) "空白首页" else state.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleUrlBar)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
            BrowserBarIcon(
                icon = if (state.showUrlBar) Icons.Default.Close else Icons.Default.Search,
                contentDescription = "切换地址栏",
                onClick = onToggleUrlBar
            )
            BrowserBarIcon(
                icon = Icons.Default.Refresh,
                contentDescription = "刷新",
                onClick = onReload
            )
            Box {
                BrowserBarIcon(
                    icon = Icons.Default.VideoLibrary,
                    contentDescription = "嗅探结果",
                    onClick = onShowDetectedVideos
                )
                if (detectedVideoCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 3.dp)
                            .background(Color.Red, CircleShape)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = detectedVideoCount.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            BrowserBarIcon(
                icon = Icons.Default.PlayCircle,
                contentDescription = "播放最佳结果",
                onClick = onPlayBestVideo,
                enabled = detectedVideoCount > 0
            )
        }
    }
}

@Composable
fun BrowserUrlDropdownOverlay(
    state: BrowserPageState,
    onDismiss: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSubmitInput: () -> Unit,
    onSelectSearchEngine: (BrowserSearchEngine) -> Unit,
    onOpenCurrentUrl: () -> Unit,
    onCopyCurrentUrl: () -> Unit,
    onEditCurrentUrl: () -> Unit,
    onOpenSearchRecord: (String) -> Unit,
    onDeleteSearchRecord: (Long) -> Unit,
    onClearSearchRecords: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val configuration = LocalConfiguration.current
    val panelHeight = maxOf(configuration.screenHeightDp.dp * 0.75f, 420.dp)
    var showSearchEnginePanel by remember { mutableStateOf(false) }

    LaunchedEffect(state.showUrlBar) {
        if (!state.showUrlBar) {
            showSearchEnginePanel = false
        }
    }

    if (!state.showUrlBar) {
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(panelHeight)
                .align(Alignment.TopCenter),
            color = Color.White,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BrowserAddressInputCard(
                    value = state.inputUrl,
                    currentEngine = state.searchEngine,
                    isEnginePanelExpanded = showSearchEnginePanel,
                    onValueChange = onInputChanged,
                    onToggleEnginePanel = { showSearchEnginePanel = !showSearchEnginePanel },
                    onSubmit = {
                        showSearchEnginePanel = false
                        focusManager.clearFocus(force = true)
                        onSubmitInput()
                    }
                )

                if (state.currentUrl != BrowserSecurityPolicy.BLANK_HOME_URL) {
                    CurrentUrlRow(
                        url = state.currentUrl,
                        onOpenCurrentUrl = onOpenCurrentUrl,
                        onCopyCurrentUrl = onCopyCurrentUrl,
                        onEditCurrentUrl = onEditCurrentUrl
                    )
                }

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        SearchRecordSection(
                            records = state.searchRecords,
                            onOpenRecord = onOpenSearchRecord,
                            onDeleteRecord = onDeleteSearchRecord,
                            onClearAll = onClearSearchRecords
                        )
                    }

                    if (showSearchEnginePanel) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { showSearchEnginePanel = false }
                        )
                    }

                    Box(
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showSearchEnginePanel,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            BrowserSearchEnginePanel(
                                currentEngine = state.searchEngine,
                                onSelectEngine = { engine ->
                                    showSearchEnginePanel = false
                                    onSelectSearchEngine(engine)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowserBarIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.32f),
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun BrowserAddressInputCard(
    value: String,
    currentEngine: BrowserSearchEngine,
    isEnginePanelExpanded: Boolean,
    onValueChange: (String) -> Unit,
    onToggleEnginePanel: () -> Unit,
    onSubmit: () -> Unit
) {
    val textStyle = TextStyle(
        color = Color.Black,
        fontSize = 12.sp,
        lineHeight = 17.sp
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Black, RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchEngineToggle(
                engine = currentEngine,
                expanded = isEnginePanelExpanded,
                onClick = onToggleEnginePanel
            )
            Spacer(modifier = Modifier.width(6.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 22.dp, max = 64.dp)
                    .padding(vertical = 2.dp),
                maxLines = 3,
                textStyle = textStyle,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 22.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = "搜索或输入网址",
                                color = Color(0xFF6B7280),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                maxLines = 3
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (value.isNotBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { onValueChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "清空输入框",
                        tint = Color(0xFF4B5563),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSubmit),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索或前往",
                    tint = Color(0xFF111827),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CurrentUrlRow(
    url: String,
    onOpenCurrentUrl: () -> Unit,
    onCopyCurrentUrl: () -> Unit,
    onEditCurrentUrl: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenCurrentUrl),
                shape = RoundedCornerShape(12.dp),
                color = Color.White
            ) {
                Text(
                    text = url,
                    color = Color(0xFF111827),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            UrlActionButton(
                icon = Icons.Default.ContentCopy,
                label = "复制链接",
                onClick = onCopyCurrentUrl
            )
            Spacer(modifier = Modifier.width(6.dp))
            UrlActionButton(
                icon = Icons.Default.Edit,
                label = "编辑链接",
                onClick = onEditCurrentUrl
            )
        }
    }
}

@Composable
private fun UrlActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            color = Color(0xFF374151),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun SearchEngineToggle(
    engine: BrowserSearchEngine,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        AsyncImage(
            model = engine.iconUrl,
            contentDescription = engine.displayName,
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Fit
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "收起搜索引擎列表" else "展开搜索引擎列表",
            modifier = Modifier.size(14.dp),
            tint = Color(0xFF374151)
        )
    }
}

@Composable
private fun BrowserSearchEnginePanel(
    currentEngine: BrowserSearchEngine,
    onSelectEngine: (BrowserSearchEngine) -> Unit
) {
    val searchEngineRows = remember { BrowserSearchEngine.entries.chunked(3) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "选择搜索引擎",
                color = Color(0xFF111827),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            searchEngineRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { engine ->
                        SearchEngineCard(
                            engine = engine,
                            selected = engine == currentEngine,
                            onClick = { onSelectEngine(engine) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchRecordSection(
    records: List<BrowserSearchRecord>,
    onOpenRecord: (String) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    val rows = remember(records) { records.chunked(2) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "搜索记录",
                color = Color(0xFF111827),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            if (records.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onClearAll)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "一键清空",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "一键清空",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (records.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC)
            ) {
                Text(
                    text = "新的搜索词或网址会保存在这里，点击即可再次跳转。",
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                )
            }
            return
        }

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { record ->
                    SearchRecordCard(
                        record = record,
                        onClick = { onOpenRecord(record.targetUrl) },
                        onDelete = { onDeleteRecord(record.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(2 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SearchRecordCard(
    record: BrowserSearchRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        tonalElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = record.query,
                    color = Color(0xFF111827),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 20.dp)
                )
                Text(
                    text = record.targetUrl,
                    color = Color(0xFF6B7280),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "删除搜索记录",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchEngineCard(
    engine: BrowserSearchEngine,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color(0xFFD1D5DB)
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        Color.White
    }

    Column(
        modifier = modifier
            .height(78.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AsyncImage(
            model = engine.iconUrl,
            contentDescription = engine.displayName,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Fit
        )
        Text(
            text = engine.displayName,
            color = Color(0xFF111827),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
