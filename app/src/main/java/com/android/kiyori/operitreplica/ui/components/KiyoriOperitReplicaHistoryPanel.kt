package com.android.kiyori.operitreplica.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.kiyori.operitreplica.model.OperitReplicaConversationBindingKind
import com.android.kiyori.operitreplica.model.OperitReplicaConversation
import com.android.kiyori.operitreplica.model.OperitReplicaConversationSection
import com.android.kiyori.operitreplica.model.OperitReplicaConversationSectionType

@Composable
internal fun KiyoriOperitReplicaHistoryPanel(
    modifier: Modifier = Modifier,
    activeConversation: OperitReplicaConversation?,
    groupedConversations: List<OperitReplicaConversationSection>,
    historySearchQuery: String,
    showHistorySearchBox: Boolean,
    onBackClick: () -> Unit,
    onToggleSearchBox: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
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
) {
    var collapsedCharacterKeys by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var collapsedGroupKeys by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var conversationActionTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var groupActionTargetKey by rememberSaveable { mutableStateOf<String?>(null) }
    var renameConversationTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameGroupTargetKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreateGroupDialog by rememberSaveable { mutableStateOf(false) }

    fun toggleCharacter(key: String) {
        collapsedCharacterKeys =
            if (key in collapsedCharacterKeys) {
                collapsedCharacterKeys - key
            } else {
                collapsedCharacterKeys + key
            }
    }

    fun toggleGroup(key: String) {
        collapsedGroupKeys =
            if (key in collapsedGroupKeys) {
                collapsedGroupKeys - key
            } else {
                collapsedGroupKeys + key
            }
    }

    val conversationActionTarget =
        conversationActionTargetId?.let { targetId ->
            findConversationById(groupedConversations, targetId)
        }
    val groupActionTarget =
        groupActionTargetKey?.let { targetKey ->
            findGroupByKey(groupedConversations, targetKey)
        }
    val renameConversationTarget =
        renameConversationTargetId?.let { targetId ->
            findConversationById(groupedConversations, targetId)
        }
    val renameGroupTarget =
        renameGroupTargetKey?.let { targetKey ->
            findGroupByKey(groupedConversations, targetKey)
        }

    Column(modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OperitSecondaryIconButton(
                icon = Icons.Default.Close,
                contentDescription = "close history",
                onClick = onBackClick,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "聊天历史",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111111),
                modifier = Modifier.weight(1f),
            )
            OperitSecondaryIconButton(
                icon = Icons.Default.Search,
                contentDescription = "search history",
                selected = showHistorySearchBox,
                selectedTint = Color(0xFF4568B2),
                onClick = onToggleSearchBox,
            )
            Spacer(modifier = Modifier.width(6.dp))
            OperitSecondaryIconButton(
                icon = Icons.Default.SettingsSuggest,
                contentDescription = "history settings",
                onClick = onOpenHistorySettings,
            )
        }

        if (showHistorySearchBox) {
            OutlinedTextField(
                value = historySearchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                placeholder = { Text("搜索对话或分组") },
                singleLine = true,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD8E2F8),
                        unfocusedBorderColor = Color(0xFFE5EAF2),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                    ),
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF111827),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCreateConversation)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "新建会话",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF2F5FE),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD8E2F8)),
            ) {
                Row(
                    modifier =
                        Modifier
                            .clickable(onClick = { showCreateGroupDialog = true })
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "create group",
                        tint = Color(0xFF4568B2),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "新建分组",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4568B2),
                    )
                }
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (groupedConversations.isEmpty()) {
                OperitReplicaHistoryEmptyState(searching = historySearchQuery.isNotBlank())
            } else {
                groupedConversations.forEach { section ->
                    OperitReplicaHistorySectionBlock(
                        section = section,
                        depth = 0,
                        activeConversationId = activeConversation?.id,
                        collapsedCharacterKeys = collapsedCharacterKeys,
                        collapsedGroupKeys = collapsedGroupKeys,
                        onToggleCharacter = ::toggleCharacter,
                        onToggleGroup = ::toggleGroup,
                        onSelectConversation = onSelectConversation,
                        onOpenConversationActions = { conversationActionTargetId = it },
                        onOpenGroupActions = { groupActionTargetKey = it },
                    )
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        var newGroupName by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("新建分组") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入分组名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCreateGroup(newGroupName)
                        showCreateGroupDialog = false
                    },
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    if (conversationActionTarget != null) {
        AlertDialog(
            onDismissRequest = { conversationActionTargetId = null },
            title = { Text("会话操作") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = conversationActionTarget.title,
                        fontSize = 13.sp,
                        color = Color(0xFF667085),
                    )
                    OperitReplicaActionRow(
                        icon = Icons.Default.Edit,
                        title = "重命名",
                        onClick = {
                            renameConversationTargetId = conversationActionTarget.id
                            conversationActionTargetId = null
                        },
                    )
                    OperitReplicaActionRow(
                        icon = Icons.Default.KeyboardArrowUp,
                        title = "上移",
                        onClick = {
                            onMoveConversationUp(conversationActionTarget.id)
                            conversationActionTargetId = null
                        },
                    )
                    OperitReplicaActionRow(
                        icon = Icons.Default.KeyboardArrowDown,
                        title = "下移",
                        onClick = {
                            onMoveConversationDown(conversationActionTarget.id)
                            conversationActionTargetId = null
                        },
                    )
                    OperitReplicaActionRow(
                        icon = Icons.Default.Delete,
                        title = "删除",
                        destructive = true,
                        onClick = {
                            onDeleteConversation(conversationActionTarget.id)
                            conversationActionTargetId = null
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { conversationActionTargetId = null }) {
                    Text("关闭")
                }
            },
        )
    }

    if (renameConversationTarget != null) {
        var newTitle by rememberSaveable(renameConversationTarget.id) { mutableStateOf(renameConversationTarget.title) }
        AlertDialog(
            onDismissRequest = { renameConversationTargetId = null },
            title = { Text("重命名会话") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameConversation(renameConversationTarget.id, newTitle)
                        renameConversationTargetId = null
                    },
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameConversationTargetId = null }) {
                    Text("取消")
                }
            },
        )
    }

    if (groupActionTarget != null) {
        AlertDialog(
            onDismissRequest = { groupActionTargetKey = null },
            title = { Text("分组管理") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = groupActionTarget.title,
                        fontSize = 13.sp,
                        color = Color(0xFF667085),
                    )
                    OperitReplicaActionRow(
                        icon = Icons.Default.Edit,
                        title = "重命名分组",
                        onClick = {
                            renameGroupTargetKey = groupActionTarget.key
                            groupActionTargetKey = null
                        },
                    )
                    OperitReplicaActionRow(
                        icon = Icons.Default.Remove,
                        title = "移出分组",
                        onClick = {
                            groupActionTarget.groupName?.let { groupName ->
                                onClearGroup(groupName, groupActionTarget.characterId)
                            }
                            groupActionTargetKey = null
                        },
                    )
                    OperitReplicaActionRow(
                        icon = Icons.Default.Delete,
                        title = "删除组内会话",
                        destructive = true,
                        onClick = {
                            groupActionTarget.groupName?.let { groupName ->
                                onDeleteGroup(groupName, groupActionTarget.characterId)
                            }
                            groupActionTargetKey = null
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { groupActionTargetKey = null }) {
                    Text("关闭")
                }
            },
        )
    }

    if (renameGroupTarget != null) {
        var newGroupName by rememberSaveable(renameGroupTarget.key) { mutableStateOf(renameGroupTarget.title) }
        AlertDialog(
            onDismissRequest = { renameGroupTargetKey = null },
            title = { Text("重命名分组") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        renameGroupTarget.groupName?.let { groupName ->
                            onRenameGroup(groupName, newGroupName, renameGroupTarget.characterId)
                        }
                        renameGroupTargetKey = null
                    },
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameGroupTargetKey = null }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
internal fun OperitReplicaSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF5B6475),
    )
}

@Composable
private fun OperitReplicaHistorySectionBlock(
    section: OperitReplicaConversationSection,
    depth: Int,
    activeConversationId: String?,
    collapsedCharacterKeys: List<String>,
    collapsedGroupKeys: List<String>,
    onToggleCharacter: (String) -> Unit,
    onToggleGroup: (String) -> Unit,
    onSelectConversation: (String) -> Unit,
    onOpenConversationActions: (String) -> Unit,
    onOpenGroupActions: (String) -> Unit,
) {
    when (section.type) {
        OperitReplicaConversationSectionType.CHARACTER -> {
            val expanded = section.key !in collapsedCharacterKeys
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OperitReplicaCharacterHeader(
                    title = section.title,
                    subtitle = section.subtitle,
                    bindingKind = section.bindingKind,
                    accent = section.accent,
                    count = sectionConversationCount(section),
                    expanded = expanded,
                    onClick = { onToggleCharacter(section.key) },
                )
                if (expanded) {
                    Column(
                        modifier = Modifier.padding(start = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        section.childSections.forEach { childSection ->
                            OperitReplicaHistorySectionBlock(
                                section = childSection,
                                depth = depth + 1,
                                activeConversationId = activeConversationId,
                                collapsedCharacterKeys = collapsedCharacterKeys,
                                collapsedGroupKeys = collapsedGroupKeys,
                                onToggleCharacter = onToggleCharacter,
                                onToggleGroup = onToggleGroup,
                                onSelectConversation = onSelectConversation,
                                onOpenConversationActions = onOpenConversationActions,
                                onOpenGroupActions = onOpenGroupActions,
                            )
                        }
                    }
                }
            }
        }
        OperitReplicaConversationSectionType.GROUP -> {
            val expanded = section.key !in collapsedGroupKeys
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OperitReplicaGroupHeader(
                    title = section.title,
                    subtitle = section.subtitle,
                    accent = section.accent,
                    count = section.conversations.size,
                    expanded = expanded,
                    nested = depth > 0,
                    showManageHint = section.groupName != null,
                    onClick = { onToggleGroup(section.key) },
                    onLongClick = if (section.groupName != null) {
                        { onOpenGroupActions(section.key) }
                    } else {
                        null
                    },
                )
                if (expanded) {
                    Column(
                        modifier = if (depth > 0) Modifier.padding(start = 14.dp) else Modifier,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        section.conversations.forEach { conversation ->
                            OperitReplicaHistoryRow(
                                conversation = conversation,
                                selected = activeConversationId == conversation.id,
                                nested = depth > 0,
                                onClick = { onSelectConversation(conversation.id) },
                                onLongClick = { onOpenConversationActions(conversation.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperitReplicaCharacterHeader(
    title: String,
    subtitle: String?,
    bindingKind: OperitReplicaConversationBindingKind,
    accent: Color,
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = characterBindingIcon(bindingKind),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF24314A),
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        color = Color(0xFF667085),
                    )
                }
            }
        }
        if (bindingKind == OperitReplicaConversationBindingKind.CHARACTER_GROUP ||
            bindingKind == OperitReplicaConversationBindingKind.UNBOUND
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = accent.copy(alpha = 0.12f),
            ) {
                Text(
                    text =
                        when (bindingKind) {
                            OperitReplicaConversationBindingKind.CHARACTER_GROUP -> "组绑定"
                            OperitReplicaConversationBindingKind.UNBOUND -> "未绑定"
                            else -> ""
                        },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(accent.copy(alpha = 0.18f)),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = count.toString(),
            fontSize = 11.sp,
            color = Color(0xFF98A2B3),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = Color(0xFF98A2B3),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun OperitReplicaGroupHeader(
    title: String,
    subtitle: String?,
    accent: Color,
    count: Int,
    expanded: Boolean,
    nested: Boolean,
    showManageHint: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (nested) {
            Box(
                modifier =
                    Modifier
                        .width(18.dp)
                        .height(36.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .width(2.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(accent.copy(alpha = 0.22f)),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        Surface(
            modifier =
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    ),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF7F8FC),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE7ECF4)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(15.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111111),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            color = Color(0xFF667085),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (showManageHint) {
                        Text(
                            text = "长按管理",
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            color = Color(0xFF98A2B3),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    text = count.toString(),
                    fontSize = 11.sp,
                    color = Color(0xFF98A2B3),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF98A2B3),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun OperitReplicaHistoryEmptyState(searching: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF7F8FC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE7ECF4)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (searching) "没有匹配的历史记录" else "还没有历史会话",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111111),
            )
            Text(
                text = if (searching) "尝试更换关键词，或从上方直接创建新会话。" else "创建第一条会话后，这里会按 Operit 的层级显示角色卡和分组。",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFF667085),
            )
        }
    }
}

@Composable
private fun OperitReplicaActionRow(
    icon: ImageVector,
    title: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (destructive) Color(0xFFFDECEC) else Color(0xFFF7F8FC))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (destructive) Color(0xFFCE3A3A) else Color(0xFF4568B2),
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (destructive) Color(0xFF8F1D1D) else Color(0xFF111111),
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun OperitReplicaHistoryRow(
    conversation: OperitReplicaConversation,
    selected: Boolean,
    nested: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (nested) {
            Box(
                modifier =
                    Modifier
                        .width(18.dp)
                        .height(72.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .width(2.dp)
                            .height(72.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(conversation.accent.copy(alpha = 0.18f)),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) Color(0xFFF2F5FE) else Color.Transparent)
                    .border(
                        1.dp,
                        if (selected) Color(0xFFD8E2F8) else Color.Transparent,
                        RoundedCornerShape(16.dp),
                    )
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                    .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                tint = if (selected) Color(0xFF4568B2) else Color(0xFF98A2B3),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(conversation.accent.copy(alpha = if (selected) 0.18f else 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = conversation.accent.copy(alpha = if (selected) 1f else 0.88f),
                    modifier = Modifier.size(17.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.title,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = Color(0xFF111111),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = conversation.preview,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatOperitConversationRowTime(conversation.sortKey),
                    fontSize = 11.sp,
                    color = Color(0xFF98A2B3),
                )
                if (selected) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier =
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4568B2)),
                    )
                }
            }
        }
    }
}

private fun findConversationById(
    sections: List<OperitReplicaConversationSection>,
    conversationId: String,
): OperitReplicaConversation? {
    sections.forEach { section ->
        section.conversations.firstOrNull { it.id == conversationId }?.let { return it }
        findConversationById(section.childSections, conversationId)?.let { return it }
    }
    return null
}

private fun findGroupByKey(
    sections: List<OperitReplicaConversationSection>,
    targetKey: String,
): OperitReplicaConversationSection? {
    sections.forEach { section ->
        if (section.type == OperitReplicaConversationSectionType.GROUP && section.key == targetKey) {
            return section
        }
        findGroupByKey(section.childSections, targetKey)?.let { return it }
    }
    return null
}

private fun sectionConversationCount(section: OperitReplicaConversationSection): Int {
    return when (section.type) {
        OperitReplicaConversationSectionType.CHARACTER -> section.childSections.sumOf(::sectionConversationCount)
        OperitReplicaConversationSectionType.GROUP -> section.conversations.size
    }
}

private fun characterBindingIcon(bindingKind: OperitReplicaConversationBindingKind): ImageVector {
    return when (bindingKind) {
        OperitReplicaConversationBindingKind.CHARACTER_GROUP -> Icons.Default.Groups
        OperitReplicaConversationBindingKind.UNBOUND -> Icons.Default.Remove
        else -> Icons.Default.Person
    }
}

private fun formatOperitConversationRowTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val dayMs = 24 * 60 * 60 * 1000L
    return when {
        diff < dayMs -> android.text.format.DateFormat.format("HH:mm", timestamp).toString()
        diff < dayMs * 2 -> "昨天"
        else -> android.text.format.DateFormat.format("MM-dd", timestamp).toString()
    }
}
