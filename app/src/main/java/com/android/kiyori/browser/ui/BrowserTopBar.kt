package com.android.kiyori.browser.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.kiyori.browser.domain.BrowserPageState
import com.android.kiyori.browser.domain.BrowserSearchEngine
import com.android.kiyori.browser.domain.BrowserSearchRecord
import com.android.kiyori.browser.security.BrowserSecurityPolicy

private val BrowserTopBarHorizontalPadding = 8.dp
private val BrowserTopBarVerticalPadding = 8.dp
private val BrowserTopBarItemSpacing = 5.dp
private val BrowserTopBarSideButtonWidth = 26.dp
private val BrowserTopBarSideButtonHeight = 30.dp
private val BrowserTopBarSideIconSize = 22.dp

@Composable
fun BrowserTopBar(
    state: BrowserPageState,
    detectedVideoCount: Int,
    onBackPressed: () -> Unit,
    onToggleUrlBar: () -> Unit,
    onReload: () -> Unit,
    onShowDetectedVideos: () -> Unit,
    onSelectQuickSearchEngine: (BrowserSearchEngine) -> Unit,
    onDismissQuickSearchEngineBar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = BrowserTopBarHorizontalPadding,
                    vertical = BrowserTopBarVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BrowserTopBarItemSpacing)
        ) {
            BrowserSearchBackButton(onClick = onBackPressed)
            BrowserCollapsedAddressCard(
                modifier = Modifier
                    .weight(1f),
                title = if (state.isBlankPage) "空白首页" else state.title,
                detectedVideoCount = detectedVideoCount,
                onClick = onToggleUrlBar,
                onShowDetectedVideos = onShowDetectedVideos
            )
            BrowserTopBarActionButton(
                icon = Icons.Default.Refresh,
                contentDescription = "刷新",
                onClick = onReload,
                iconSize = BrowserTopBarSideIconSize
            )
        }

        if (
            state.showSearchEngineQuickSwitchBar &&
            state.lastSearchQuery.isNotBlank()
        ) {
            BrowserSearchEngineQuickSwitchBar(
                currentEngine = state.searchEngine,
                onSelectEngine = onSelectQuickSearchEngine,
                onClose = onDismissQuickSearchEngineBar
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
    onClearSearchRecords: () -> Unit,
    onToggleIncognitoMode: () -> Unit
) {
    val focusManager = LocalFocusManager.current
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
                .fillMaxSize()
                .align(Alignment.TopCenter),
            color = Color.White,
            shadowElevation = 0.dp,
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(
                        horizontal = BrowserTopBarHorizontalPadding,
                        vertical = BrowserTopBarVerticalPadding
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BrowserTopBarItemSpacing)
                ) {
                    BrowserSearchBackButton(onClick = onDismiss)
                    BrowserAddressInputCard(
                        modifier = Modifier.weight(1f),
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
                    Spacer(modifier = Modifier.width(BrowserTopBarSideButtonWidth))
                }

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
                            isIncognitoMode = state.isIncognitoMode,
                            records = state.searchRecords,
                            onOpenRecord = onOpenSearchRecord,
                            onDeleteRecord = onDeleteSearchRecord,
                            onClearAll = onClearSearchRecords,
                            onToggleIncognitoMode = onToggleIncognitoMode
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
private fun BrowserSearchBackButton(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(
                width = BrowserTopBarSideButtonWidth,
                height = BrowserTopBarSideButtonHeight
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "返回上一页",
            tint = Color(0xFF111827),
            modifier = Modifier.size(BrowserTopBarSideIconSize)
        )
    }
}

@Composable
private fun BrowserTopBarActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    iconSize: Dp = 18.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(
                width = BrowserTopBarSideButtonWidth,
                height = BrowserTopBarSideButtonHeight
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFF111827),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun BrowserCollapsedAddressCard(
    modifier: Modifier = Modifier,
    title: String,
    detectedVideoCount: Int,
    onClick: () -> Unit,
    onShowDetectedVideos: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.Black, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color(0xFF111827),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        if (detectedVideoCount > 0) {
            val badgeText = if (detectedVideoCount > 99) "99+" else detectedVideoCount.toString()
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF8A00))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onShowDetectedVideos
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = if (badgeText.length > 2) 9.sp else 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF111827),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BrowserAddressInputCard(
    modifier: Modifier = Modifier,
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
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
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
                    .focusRequester(focusRequester)
                    .weight(1f)
                    .heightIn(min = 22.dp, max = 22.dp)
                    .padding(vertical = 2.dp),
                maxLines = 1,
                textStyle = textStyle,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        onSubmit()
                    }
                ),
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
                                maxLines = 1
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
        Image(
            painter = painterResource(id = engine.iconResId),
            contentDescription = engine.displayName,
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
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
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFEFF3F7),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
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
    isIncognitoMode: Boolean,
    records: List<BrowserSearchRecord>,
    onOpenRecord: (String) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onClearAll: () -> Unit,
    onToggleIncognitoMode: () -> Unit
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
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp)
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isIncognitoMode) Color(0xFF111827) else Color(0xFFF3F4F6)
                    )
                    .clickable(onClick = onToggleIncognitoMode)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = "无痕模式",
                    tint = if (isIncognitoMode) Color.White else Color(0xFF374151),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "无痕",
                    color = if (isIncognitoMode) Color.White else Color(0xFF374151),
                    fontSize = 11.sp
                )
            }
            if (records.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
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
                Spacer(modifier = Modifier.height(1.dp))
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
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (selected) Color(0xFFB8C5F3) else Color(0xFFE6ECF2)
    val backgroundColor = if (selected) {
        Color(0xFFE6EBFA)
    } else {
        Color.White
    }

    Row(
        modifier = modifier
            .height(50.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(id = engine.iconResId),
            contentDescription = engine.displayName,
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(5.dp))
        )
        Text(
            text = engine.displayName,
            color = Color(0xFF111827),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BrowserSearchEngineQuickSwitchBar(
    currentEngine: BrowserSearchEngine,
    onSelectEngine: (BrowserSearchEngine) -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BrowserSearchEngine.entries.forEach { engine ->
                QuickSearchEngineChip(
                    engine = engine,
                    selected = engine == currentEngine,
                    onClick = {
                        if (engine != currentEngine) {
                            onSelectEngine(engine)
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭搜索引擎切换条",
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun QuickSearchEngineChip(
    engine: BrowserSearchEngine,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(shape)
            .background(if (selected) Color(0xFFE8F0FF) else Color(0xFFF5F5F5))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Image(
            painter = painterResource(id = engine.iconResId),
            contentDescription = engine.displayName,
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Text(
            text = engine.displayName,
            color = if (selected) Color(0xFF2B4E9C) else Color(0xFF202124),
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

