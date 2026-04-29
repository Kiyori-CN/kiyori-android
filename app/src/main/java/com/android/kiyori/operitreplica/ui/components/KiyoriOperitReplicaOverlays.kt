package com.android.kiyori.operitreplica.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.kiyori.operitreplica.model.OperitReplicaCharacterOption
import com.android.kiyori.operitreplica.model.OperitReplicaCharacterSortOption
import com.android.kiyori.operitreplica.model.OperitReplicaConversation
import com.android.kiyori.operitreplica.model.OperitReplicaConversationSection
import com.android.kiyori.operitreplica.model.OperitReplicaHistoryDisplayMode
import com.android.kiyori.operitreplica.model.OperitReplicaMessage

@Composable
internal fun BoxScope.KiyoriOperitReplicaOverlayHost(
    bottomDockPadding: Dp,
    showHistoryPanel: Boolean,
    historySearchQuery: String,
    showHistorySearchBox: Boolean,
    activeConversation: OperitReplicaConversation?,
    groupedConversations: List<OperitReplicaConversationSection>,
    showFeaturePanel: Boolean,
    showAttachmentPanel: Boolean,
    showSwipeHint: Boolean,
    showFullscreenEditor: Boolean,
    showHistorySettingsDialog: Boolean,
    historyDisplayMode: OperitReplicaHistoryDisplayMode,
    autoSwitchCharacterCard: Boolean,
    autoSwitchChatOnCharacterSelect: Boolean,
    showCharacterSelector: Boolean,
    selectedCharacterId: String,
    characterSortOption: OperitReplicaCharacterSortOption,
    characterOptions: List<OperitReplicaCharacterOption>,
    showWorkspacePanel: Boolean,
    hasEverOpenedWorkspace: Boolean,
    isWorkspacePreparing: Boolean,
    workspaceReloadVersion: Int,
    showComputerPanel: Boolean,
    currentModelLabel: String,
    enableThinking: Boolean,
    enableTools: Boolean,
    enableMemory: Boolean,
    enableStream: Boolean,
    enableVoice: Boolean,
    enableWorkspace: Boolean,
    enableNotification: Boolean,
    inputText: String,
    activeMessages: List<OperitReplicaMessage>,
    onHistoryPanelDismiss: () -> Unit,
    onToggleHistorySearchBox: () -> Unit,
    onHistoryQueryChange: (String) -> Unit,
    onCreateConversation: () -> Unit,
    onCreateGroup: (String) -> Unit,
    onSelectConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onMoveConversationUp: (String) -> Unit,
    onMoveConversationDown: (String) -> Unit,
    onRenameGroup: (String, String, String?) -> Unit,
    onClearGroup: (String, String?) -> Unit,
    onDeleteGroup: (String, String?) -> Unit,
    onOpenHistorySettings: () -> Unit,
    onThinkingChange: (Boolean) -> Unit,
    onToolsChange: (Boolean) -> Unit,
    onMemoryChange: (Boolean) -> Unit,
    onStreamChange: (Boolean) -> Unit,
    onVoiceChange: (Boolean) -> Unit,
    onWorkspaceChange: (Boolean) -> Unit,
    onNotificationChange: (Boolean) -> Unit,
    onFeaturePanelDismiss: () -> Unit,
    onAttachmentPanelDismiss: () -> Unit,
    onAddAttachment: (String) -> Unit,
    onDismissSwipeHint: () -> Unit,
    onFullscreenInputChange: (String) -> Unit,
    onDismissFullscreenEditor: () -> Unit,
    onSubmitFullscreenEditor: () -> Unit,
    onDismissHistorySettings: () -> Unit,
    onHistoryDisplayModeChange: (OperitReplicaHistoryDisplayMode) -> Unit,
    onAutoSwitchCharacterCardChange: (Boolean) -> Unit,
    onAutoSwitchChatOnCharacterSelectChange: (Boolean) -> Unit,
    onDismissCharacterSelector: () -> Unit,
    onSelectCharacter: (String) -> Unit,
    onCharacterSortOptionChange: (OperitReplicaCharacterSortOption) -> Unit,
    onOpenCharacterSettings: () -> Unit,
    onCloseWorkspace: () -> Unit,
    onReloadWorkspace: () -> Unit,
    onOpenComputer: () -> Unit,
    onCloseComputer: () -> Unit,
    onModelClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = showHistoryPanel,
        enter = fadeIn(animationSpec = tween(240)),
        exit = fadeOut(animationSpec = tween(200)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.22f))
                    .clickable(onClick = onHistoryPanelDismiss),
        )
    }

    AnimatedVisibility(
        visible = showHistoryPanel,
        enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(260)),
        exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(220)),
    ) {
        KiyoriOperitReplicaHistoryPanel(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .width(286.dp)
                    .safeDrawingPadding()
                    .background(Color.White),
            activeConversation = activeConversation,
            groupedConversations = groupedConversations,
            historySearchQuery = historySearchQuery,
            showHistorySearchBox = showHistorySearchBox,
            onBackClick = onHistoryPanelDismiss,
            onToggleSearchBox = onToggleHistorySearchBox,
            onSearchQueryChange = onHistoryQueryChange,
            onCreateConversation = onCreateConversation,
            onCreateGroup = onCreateGroup,
            onSelectConversation = onSelectConversation,
            onRenameConversation = onRenameConversation,
            onDeleteConversation = onDeleteConversation,
            onMoveConversationUp = onMoveConversationUp,
            onMoveConversationDown = onMoveConversationDown,
            onRenameGroup = onRenameGroup,
            onClearGroup = onClearGroup,
            onDeleteGroup = onDeleteGroup,
            onOpenHistorySettings = onOpenHistorySettings,
        )
    }

    if (showFeaturePanel) {
        KiyoriOperitReplicaFeaturePanel(
            modifier =
                Modifier
                    .padding(start = 14.dp, bottom = 132.dp + bottomDockPadding)
                    .align(Alignment.BottomStart),
            enableThinking = enableThinking,
            enableTools = enableTools,
            enableMemory = enableMemory,
            enableStream = enableStream,
            enableVoice = enableVoice,
            enableWorkspace = enableWorkspace,
            enableNotification = enableNotification,
            currentModelLabel = currentModelLabel,
            onThinkingChange = onThinkingChange,
            onToolsChange = onToolsChange,
            onMemoryChange = onMemoryChange,
            onStreamChange = onStreamChange,
            onVoiceChange = onVoiceChange,
            onWorkspaceChange = onWorkspaceChange,
            onNotificationChange = onNotificationChange,
            onModelClick = onModelClick,
            onDismiss = onFeaturePanelDismiss,
        )
    }

    if (showAttachmentPanel) {
        KiyoriOperitReplicaAttachmentPanel(
            modifier =
                Modifier
                    .padding(start = 14.dp, bottom = 132.dp + bottomDockPadding)
                    .align(Alignment.BottomStart),
            onDismiss = onAttachmentPanelDismiss,
            onAddAttachment = onAddAttachment,
        )
    }

    if (showSwipeHint) {
        KiyoriOperitReplicaSwipeHint(
            modifier = Modifier.align(Alignment.TopCenter),
            onDismiss = onDismissSwipeHint,
        )
    }

    if (showFullscreenEditor) {
        KiyoriOperitReplicaFullscreenEditorDialog(
            inputText = inputText,
            onInputTextChange = onFullscreenInputChange,
            onDismiss = onDismissFullscreenEditor,
            onSubmit = onSubmitFullscreenEditor,
        )
    }

    if (showHistorySettingsDialog) {
        KiyoriOperitReplicaHistorySettingsDialog(
            historyDisplayMode = historyDisplayMode,
            autoSwitchCharacterCard = autoSwitchCharacterCard,
            autoSwitchChatOnCharacterSelect = autoSwitchChatOnCharacterSelect,
            onDismiss = onDismissHistorySettings,
            onHistoryDisplayModeChange = onHistoryDisplayModeChange,
            onAutoSwitchCharacterCardChange = onAutoSwitchCharacterCardChange,
            onAutoSwitchChatOnCharacterSelectChange = onAutoSwitchChatOnCharacterSelectChange,
        )
    }

    KiyoriOperitReplicaCharacterSelectorPanel(
        isVisible = showCharacterSelector,
        selectedCharacterId = selectedCharacterId,
        sortOption = characterSortOption,
        characterOptions = characterOptions,
        onDismiss = onDismissCharacterSelector,
        onSelectCharacter = onSelectCharacter,
        onSortOptionChange = onCharacterSortOptionChange,
        onOpenCharacterSettings = onOpenCharacterSettings,
    )

    KiyoriOperitReplicaWorkspaceOverlayHost(
        hasEverOpenedWorkspace = hasEverOpenedWorkspace,
        isVisible = showWorkspacePanel,
        reloadVersion = workspaceReloadVersion,
        activeConversation = activeConversation,
        messageCount = activeMessages.size,
        onClose = onCloseWorkspace,
        onOpenComputer = onOpenComputer,
        onReloadWorkspace = onReloadWorkspace,
    )

    if (showComputerPanel) {
        KiyoriOperitReplicaComputerOverlay(onClose = onCloseComputer)
    }

    AnimatedVisibility(
        visible = isWorkspacePreparing,
        enter = fadeIn(animationSpec = tween(180)),
        exit = fadeOut(animationSpec = tween(120)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.74f))
                    .clickable(enabled = false) {},
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 14.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF6177B2),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = "正在准备工作区",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111111),
                    )
                    Text(
                        text = "保持 Operit 的全屏工作区层级与进入节奏。",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF667085),
                    )
                }
            }
        }
    }
}

@Composable
private fun KiyoriOperitReplicaCharacterSelectorPanel(
    isVisible: Boolean,
    selectedCharacterId: String,
    sortOption: OperitReplicaCharacterSortOption,
    characterOptions: List<OperitReplicaCharacterOption>,
    onDismiss: () -> Unit,
    onSelectCharacter: (String) -> Unit,
    onSortOptionChange: (OperitReplicaCharacterSortOption) -> Unit,
    onOpenCharacterSettings: () -> Unit,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(180)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable(onClick = onDismiss),
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { -it / 2 }, animationSpec = tween(260)),
                exit = slideOutVertically(targetOffsetY = { -it / 2 }, animationSpec = tween(220)),
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 58.dp, start = 18.dp, end = 18.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 16.dp,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = false) {}
                                .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "选择角色",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111111),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${characterOptions.size} 个角色",
                                fontSize = 11.sp,
                                color = Color(0xFF667085),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box {
                                OperitSecondaryIconButton(
                                    icon = Icons.Default.Sort,
                                    contentDescription = "character sort",
                                    onClick = { sortMenuExpanded = true },
                                )
                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false },
                                    modifier = Modifier.background(Color.White),
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("默认顺序") },
                                        onClick = {
                                            onSortOptionChange(OperitReplicaCharacterSortOption.DEFAULT)
                                            sortMenuExpanded = false
                                        },
                                        trailingIcon = {
                                            if (sortOption == OperitReplicaCharacterSortOption.DEFAULT) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF4568B2),
                                                )
                                            }
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("按名称排序") },
                                        onClick = {
                                            onSortOptionChange(OperitReplicaCharacterSortOption.NAME_ASC)
                                            sortMenuExpanded = false
                                        },
                                        trailingIcon = {
                                            if (sortOption == OperitReplicaCharacterSortOption.NAME_ASC) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF4568B2),
                                                )
                                            }
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("按最近创建") },
                                        onClick = {
                                            onSortOptionChange(OperitReplicaCharacterSortOption.CREATED_DESC)
                                            sortMenuExpanded = false
                                        },
                                        trailingIcon = {
                                            if (sortOption == OperitReplicaCharacterSortOption.CREATED_DESC) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF4568B2),
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            OperitSecondaryIconButton(
                                icon = Icons.Default.Edit,
                                contentDescription = "character settings",
                                onClick = {
                                    onOpenCharacterSettings()
                                    onDismiss()
                                },
                            )
                        }
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            characterOptions.forEach { option ->
                                KiyoriOperitReplicaCharacterRow(
                                    option = option,
                                    selected = option.id == selectedCharacterId,
                                    onClick = { onSelectCharacter(option.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KiyoriOperitReplicaCharacterRow(
    option: OperitReplicaCharacterOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) option.accent.copy(alpha = 0.10f) else Color(0xFFF7F8FB),
        border =
            BorderStroke(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) option.accent.copy(alpha = 0.36f) else Color.Transparent,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(option.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = option.accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111111),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = option.subtitle,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF667085),
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = option.accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun KiyoriOperitReplicaWorkspaceOverlayHost(
    hasEverOpenedWorkspace: Boolean,
    isVisible: Boolean,
    reloadVersion: Int,
    activeConversation: OperitReplicaConversation?,
    messageCount: Int,
    onClose: () -> Unit,
    onOpenComputer: () -> Unit,
    onReloadWorkspace: () -> Unit,
) {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            if (hasEverOpenedWorkspace) {
                key(reloadVersion) {
                    KiyoriOperitReplicaWorkspaceInteractivePanel(
                        activeConversation = activeConversation,
                        messageCount = messageCount,
                        onClose = onClose,
                        onOpenComputer = onOpenComputer,
                        onReloadWorkspace = onReloadWorkspace,
                    )
                }
            }
        },
    ) { measurables, constraints ->
        if (measurables.isEmpty()) {
            layout(0, 0) {}
        } else if (isVisible) {
            val placeable = measurables.first().measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.placeRelative(0, 0)
            }
        } else {
            layout(0, 0) {}
        }
    }
}

@Composable
private fun KiyoriOperitReplicaWorkspacePanel(
    activeConversation: OperitReplicaConversation?,
    messageCount: Int,
    onClose: () -> Unit,
    onOpenComputer: () -> Unit,
    onReloadWorkspace: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Workspace",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111111),
                    modifier = Modifier.weight(1f),
                )
                OperitSecondaryIconButton(
                    icon = Icons.Default.DesktopWindows,
                    contentDescription = "computer",
                    onClick = onOpenComputer,
                )
                Spacer(modifier = Modifier.width(8.dp))
                OperitSecondaryIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "close workspace",
                    onClick = onClose,
                )
            }
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFFF7F8FB),
                border = BorderStroke(1.dp, Color(0xFFE6EAF2)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = activeConversation?.title ?: "当前未选中会话",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111111),
                    )
                    Text(
                        text = activeConversation?.preview ?: "工作区会跟随当前聊天容器保活，隐藏时不销毁。",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF667085),
                    )
                    Text(
                        text = "已接入 $messageCount 条消息上下文，结构对齐 Operit 的全屏工作区层。",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF475467),
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF111827),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "工作区宿主层",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Text(
                        text = "这一层现在先保证和 Operit 一致的生命周期、全屏覆盖关系、以及可重建入口，后面再直接替换成真实 WorkspaceScreen 宿主。",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color.White.copy(alpha = 0.78f),
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.08f),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onReloadWorkspace)
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "重建当前工作区宿主",
                                fontSize = 12.sp,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFFF7F8FB),
                border = BorderStroke(1.dp, Color(0xFFE6EAF2)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "后续复刻落点",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111111),
                    )
                    Text(
                        text = "1. 按 Operit 的 WorkspaceScreen 接入真实工作目录。 2. 保持隐藏时仍保活。 3. 让输入区和工作区共享当前会话上下文。",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF667085),
                    )
                }
            }
        }
    }
}

@Composable
private fun KiyoriOperitReplicaComputerOverlay(
    onClose: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFF05070A))
                .clickable(enabled = false) {},
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Computer",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                OperitSecondaryIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "close computer",
                    selectedTint = Color.White,
                    onClick = onClose,
                )
            }
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0E1525),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "全屏 Computer 宿主",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Text(
                        text = "这一层继续保持和 Operit 的 ComputerScreen 一样的覆盖策略：打开时完全接管，关闭时直接移出组合，避免底层残留。",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.05f),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "下一步真实对齐",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                            Text(
                                text = "1. 接独立终端或桌面容器。 2. 保证开启后拦截全部触摸。 3. 关闭时完整释放 SurfaceView / WebView 资源。",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Color.White.copy(alpha = 0.72f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun KiyoriOperitReplicaFeaturePanel(
    modifier: Modifier = Modifier,
    enableThinking: Boolean,
    enableTools: Boolean,
    enableMemory: Boolean,
    enableStream: Boolean,
    enableVoice: Boolean,
    enableWorkspace: Boolean,
    enableNotification: Boolean,
    currentModelLabel: String,
    onThinkingChange: (Boolean) -> Unit,
    onToolsChange: (Boolean) -> Unit,
    onMemoryChange: (Boolean) -> Unit,
    onStreamChange: (Boolean) -> Unit,
    onVoiceChange: (Boolean) -> Unit,
    onWorkspaceChange: (Boolean) -> Unit,
    onNotificationChange: (Boolean) -> Unit,
    onModelClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = modifier.width(286.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Agent 设置",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111111),
                    modifier = Modifier.weight(1f),
                )
                OperitSecondaryIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "dismiss features",
                    onClick = onDismiss,
                )
            }
            OperitReplicaSectionLabel(text = "模型", modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
            OperitReplicaPopupRow(
                title = "当前模型",
                active = true,
                value = currentModelLabel,
                onClick = onModelClick,
            )
            OperitReplicaSectionLabel(text = "能力开关", modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
            OperitReplicaPopupRow(title = "思考模式", active = enableThinking, onClick = { onThinkingChange(!enableThinking) })
            OperitReplicaPopupRow(title = "工具调用", active = enableTools, onClick = { onToolsChange(!enableTools) })
            OperitReplicaPopupRow(title = "记忆更新", active = enableMemory, onClick = { onMemoryChange(!enableMemory) })
            OperitReplicaPopupRow(title = "流式输出", active = enableStream, onClick = { onStreamChange(!enableStream) })
            OperitReplicaPopupRow(title = "语音播报", active = enableVoice, onClick = { onVoiceChange(!enableVoice) })
            OperitReplicaPopupRow(title = "工作区", active = enableWorkspace, onClick = { onWorkspaceChange(!enableWorkspace) })
            OperitReplicaPopupRow(title = "通知采集", active = enableNotification, onClick = { onNotificationChange(!enableNotification) })
        }
    }
}

@Composable
internal fun KiyoriOperitReplicaAttachmentPanel(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onAddAttachment: (String) -> Unit,
) {
    Surface(
        modifier = modifier.width(210.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "添加附件",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111111),
                    modifier = Modifier.weight(1f),
                )
                OperitSecondaryIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "dismiss attachments",
                    onClick = onDismiss,
                )
            }
            listOf(
                "屏幕内容" to Icons.Default.ContentCopy,
                "工作区" to Icons.Default.Code,
                "文件" to Icons.Default.DataObject,
                "语音" to Icons.Default.MicNone,
                "通知" to Icons.Default.Notifications,
                "位置" to Icons.Default.LocationOn,
            ).forEach { (item, icon) ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clickable { onAddAttachment(item) }
                            .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF667085),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = item, fontSize = 13.sp, color = Color(0xFF111111))
                }
            }
        }
    }
}

@Composable
internal fun KiyoriOperitReplicaSwipeHint(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier =
            modifier
                .statusBarsPadding()
                .padding(top = 8.dp),
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFF111827).copy(alpha = 0.82f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "向右滑可返回主页，当前正一屏保持 Operit 结构",
                fontSize = 11.sp,
                color = Color.White,
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
internal fun KiyoriOperitReplicaFullscreenEditorDialog(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onSubmit) {
                Text("发送")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        text = {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                modifier = Modifier.fillMaxWidth().height(240.dp),
                textStyle = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
            )
        },
    )
}

@Composable
internal fun KiyoriOperitReplicaHistorySettingsDialog(
    historyDisplayMode: OperitReplicaHistoryDisplayMode,
    autoSwitchCharacterCard: Boolean,
    autoSwitchChatOnCharacterSelect: Boolean,
    onDismiss: () -> Unit,
    onHistoryDisplayModeChange: (OperitReplicaHistoryDisplayMode) -> Unit,
    onAutoSwitchCharacterCardChange: (Boolean) -> Unit,
    onAutoSwitchChatOnCharacterSelectChange: (Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("历史设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "显示方式",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF667085),
                    )
                    OperitReplicaSelectableRow(
                        title = "按角色分组",
                        selected = historyDisplayMode == OperitReplicaHistoryDisplayMode.BY_CHARACTER_CARD,
                        onClick = { onHistoryDisplayModeChange(OperitReplicaHistoryDisplayMode.BY_CHARACTER_CARD) },
                    )
                    OperitReplicaSelectableRow(
                        title = "按文件夹分组",
                        selected = historyDisplayMode == OperitReplicaHistoryDisplayMode.BY_FOLDER,
                        onClick = { onHistoryDisplayModeChange(OperitReplicaHistoryDisplayMode.BY_FOLDER) },
                    )
                    OperitReplicaSelectableRow(
                        title = "仅当前角色",
                        selected = historyDisplayMode == OperitReplicaHistoryDisplayMode.CURRENT_CHARACTER_ONLY,
                        onClick = { onHistoryDisplayModeChange(OperitReplicaHistoryDisplayMode.CURRENT_CHARACTER_ONLY) },
                    )
                }
                OperitReplicaDialogToggle(
                    title = "自动切换角色卡",
                    checked = autoSwitchCharacterCard,
                    onCheckedChange = onAutoSwitchCharacterCardChange,
                )
                OperitReplicaDialogToggle(
                    title = "切换角色时自动切换会话",
                    checked = autoSwitchChatOnCharacterSelect,
                    onCheckedChange = onAutoSwitchChatOnCharacterSelectChange,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        },
    )
}

@Composable
private fun OperitReplicaSelectableRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) Color(0xFFF2F5FE) else Color(0xFFF8FAFD))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = Color(0xFF111111),
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF4568B2),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
internal fun OperitReplicaPopupRow(
    title: String,
    active: Boolean,
    onClick: () -> Unit,
    value: String? = null,
    subtitle: String? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (subtitle.isNullOrBlank() && value.isNullOrBlank()) 0.dp else 2.dp),
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = Color(0xFF111111),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF667085),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!value.isNullOrBlank()) {
                Text(
                    text = value,
                    fontSize = 11.sp,
                    color = Color(0xFF667085),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (value.isNullOrBlank()) {
            Icon(
                imageVector = if (active) Icons.Default.Check else Icons.Default.Remove,
                contentDescription = null,
                tint = if (active) Color(0xFF4568B2) else Color(0xFFD0D5DD),
                modifier = Modifier.size(18.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF98A2B3),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
internal fun OperitReplicaDialogToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = Color(0xFF111111),
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
