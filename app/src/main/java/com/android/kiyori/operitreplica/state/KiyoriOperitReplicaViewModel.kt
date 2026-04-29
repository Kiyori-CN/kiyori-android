package com.android.kiyori.operitreplica.state

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.kiyori.operitreplica.model.OperitReplicaCharacterOption
import com.android.kiyori.operitreplica.model.OperitReplicaCharacterSortOption
import com.android.kiyori.operitreplica.model.OperitReplicaConversationBindingKind
import com.android.kiyori.operitreplica.model.OperitReplicaConversation
import com.android.kiyori.operitreplica.model.OperitReplicaConversationGroup
import com.android.kiyori.operitreplica.model.OperitReplicaConversationSection
import com.android.kiyori.operitreplica.model.OperitReplicaConversationSectionType
import com.android.kiyori.operitreplica.model.OperitReplicaHistoryDisplayMode
import com.android.kiyori.operitreplica.model.OperitReplicaMessage
import com.android.kiyori.operitreplica.model.OperitReplicaMessageRole
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class KiyoriOperitReplicaViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(buildInitialState().withDerivedState())
    val uiState: StateFlow<KiyoriOperitReplicaUiState> = _uiState.asStateFlow()

    private var workspaceOpenJob: Job? = null

    fun updateInputText(inputText: String) {
        mutateState { it.copy(inputText = inputText) }
    }

    fun updateHistorySearchQuery(query: String) {
        mutateState { it.copy(historySearchQuery = query) }
    }

    fun setHistoryPanelVisible(visible: Boolean) {
        mutateState { it.copy(showHistoryPanel = visible) }
    }

    fun setStatsMenuVisible(visible: Boolean) {
        mutateState { it.copy(showStatsMenu = visible) }
    }

    fun toggleStatsMenu() {
        mutateState { it.copy(showStatsMenu = !it.showStatsMenu) }
    }

    fun toggleFeaturePanel() {
        mutateState { it.copy(showFeaturePanel = !it.showFeaturePanel) }
    }

    fun setFeaturePanelVisible(visible: Boolean) {
        mutateState { it.copy(showFeaturePanel = visible) }
    }

    fun toggleAttachmentPanel() {
        mutateState { it.copy(showAttachmentPanel = !it.showAttachmentPanel) }
    }

    fun setAttachmentPanelVisible(visible: Boolean) {
        mutateState { it.copy(showAttachmentPanel = visible) }
    }

    fun setFullscreenEditorVisible(visible: Boolean) {
        mutateState { it.copy(showFullscreenEditor = visible) }
    }

    fun setCharacterSelectorVisible(visible: Boolean) {
        mutateState { it.copy(showCharacterSelector = visible) }
    }

    fun selectCharacter(characterId: String) {
        mutateState { state ->
            if (state.characterOptions.none { it.id == characterId }) {
                state
            } else {
                val targetConversationId =
                    if (state.autoSwitchChatOnCharacterSelect) {
                        state.conversations
                            .sortedByDescending { it.sortKey }
                            .firstOrNull { it.characterId == characterId }
                            ?.id ?: state.activeConversationId
                    } else {
                        state.activeConversationId
                    }
                state.copy(
                    showCharacterSelector = false,
                    selectedCharacterId = characterId,
                    activeConversationId = targetConversationId,
                )
            }
        }
    }

    fun toggleHistorySearchBox() {
        mutateState { it.copy(showHistorySearchBox = !it.showHistorySearchBox) }
    }

    fun setHistorySettingsDialogVisible(visible: Boolean) {
        mutateState { it.copy(showHistorySettingsDialog = visible) }
    }

    fun dismissSwipeHint() {
        mutateState { it.copy(showSwipeHint = false) }
    }

    fun setReplyPreviewVisible(visible: Boolean) {
        mutateState { it.copy(showReplyPreview = visible) }
    }

    fun setHistoryDisplayMode(mode: OperitReplicaHistoryDisplayMode) {
        mutateState { it.copy(historyDisplayMode = mode) }
    }

    fun setAutoSwitchCharacterCard(enabled: Boolean) {
        mutateState { it.copy(autoSwitchCharacterCard = enabled) }
    }

    fun setAutoSwitchChatOnCharacterSelect(enabled: Boolean) {
        mutateState { it.copy(autoSwitchChatOnCharacterSelect = enabled) }
    }

    fun setThinkingEnabled(enabled: Boolean) {
        mutateState { it.copy(enableThinking = enabled) }
    }

    fun setToolsEnabled(enabled: Boolean) {
        mutateState { it.copy(enableTools = enabled) }
    }

    fun setMemoryEnabled(enabled: Boolean) {
        mutateState { it.copy(enableMemory = enabled) }
    }

    fun setStreamEnabled(enabled: Boolean) {
        mutateState { it.copy(enableStream = enabled) }
    }

    fun setVoiceEnabled(enabled: Boolean) {
        mutateState { it.copy(enableVoice = enabled) }
    }

    fun setWorkspaceEnabled(enabled: Boolean) {
        mutateState { it.copy(enableWorkspace = enabled) }
    }

    fun setNotificationEnabled(enabled: Boolean) {
        mutateState { it.copy(enableNotification = enabled) }
    }

    fun setCharacterSortOption(sortOption: OperitReplicaCharacterSortOption) {
        mutateState { it.copy(characterSortOption = sortOption) }
    }

    fun openWorkspacePanel() {
        workspaceOpenJob?.cancel()
        mutateState { state ->
            state.copy(
                showHistoryPanel = false,
                showFeaturePanel = false,
                showAttachmentPanel = false,
                showCharacterSelector = false,
                showComputerPanel = false,
                hasEverOpenedWorkspace = true,
                isWorkspacePreparing = true,
            )
        }
        workspaceOpenJob =
            viewModelScope.launch {
                delay(240)
                mutateState { it.copy(isWorkspacePreparing = false, showWorkspacePanel = true) }
            }
    }

    fun closeWorkspacePanel() {
        workspaceOpenJob?.cancel()
        mutateState { it.copy(isWorkspacePreparing = false, showWorkspacePanel = false) }
    }

    fun reloadWorkspacePanel() {
        workspaceOpenJob?.cancel()
        mutateState { state ->
            state.copy(
                showHistoryPanel = false,
                showFeaturePanel = false,
                showAttachmentPanel = false,
                showCharacterSelector = false,
                showComputerPanel = false,
                showWorkspacePanel = false,
                hasEverOpenedWorkspace = true,
                isWorkspacePreparing = true,
                workspaceReloadVersion = state.workspaceReloadVersion + 1,
            )
        }
        workspaceOpenJob =
            viewModelScope.launch {
                delay(180)
                mutateState { it.copy(isWorkspacePreparing = false, showWorkspacePanel = true) }
            }
    }

    fun setComputerPanelVisible(visible: Boolean) {
        workspaceOpenJob?.cancel()
        mutateState { state ->
            state.copy(
                isWorkspacePreparing = false,
                showWorkspacePanel = if (visible) false else state.showWorkspacePanel,
                showComputerPanel = visible,
            )
        }
    }

    fun removeAttachment(label: String) {
        mutateState { state ->
            state.copy(attachments = state.attachments.filterNot { it == label })
        }
    }

    fun addAttachment(label: String) {
        mutateState { state ->
            if (state.attachments.contains(label)) {
                state.copy(showAttachmentPanel = false)
            } else {
                state.copy(
                    attachments = state.attachments + label,
                    showAttachmentPanel = false,
                )
            }
        }
    }

    fun selectConversation(conversationId: String) {
        mutateState { state ->
            val selectedConversation = state.conversations.firstOrNull { it.id == conversationId }
            if (selectedConversation == null) {
                state
            } else {
                state.copy(
                    activeConversationId = conversationId,
                    selectedCharacterId =
                        if (state.autoSwitchCharacterCard) {
                            selectedConversation.characterId
                        } else {
                            state.selectedCharacterId
                        },
                    showHistoryPanel = false,
                    showReplyPreview = true,
                )
            }
        }
    }

    fun renameConversation(
        conversationId: String,
        newTitle: String,
    ) {
        val normalizedTitle = newTitle.trim()
        if (normalizedTitle.isBlank()) {
            return
        }
        mutateState { state ->
            state.copy(
                conversations =
                    state.conversations.map { conversation ->
                        if (conversation.id == conversationId) {
                            conversation.copy(title = normalizedTitle)
                        } else {
                            conversation
                        }
                    },
            )
        }
    }

    fun deleteConversation(conversationId: String) {
        mutateState { state ->
            val remainingConversations = state.conversations.filterNot { it.id == conversationId }
            val remainingMessages = state.conversationMessages - conversationId
            if (remainingConversations.isEmpty()) {
                createFallbackConversationState(
                    state = state.copy(conversations = emptyList(), conversationMessages = remainingMessages),
                )
            } else {
                val nextActiveConversationId =
                    if (state.activeConversationId == conversationId) {
                        remainingConversations.maxByOrNull { it.sortKey }?.id ?: remainingConversations.first().id
                    } else {
                        state.activeConversationId
                    }
                state.copy(
                    conversations = remainingConversations,
                    conversationMessages = remainingMessages,
                    activeConversationId = nextActiveConversationId,
                )
            }
        }
    }

    fun moveConversationUp(conversationId: String) {
        moveConversation(conversationId = conversationId, direction = -1)
    }

    fun moveConversationDown(conversationId: String) {
        moveConversation(conversationId = conversationId, direction = 1)
    }

    fun renameConversationGroup(
        groupName: String,
        newGroupName: String,
        characterId: String?,
    ) {
        val normalizedGroupName = newGroupName.trim()
        if (groupName.isBlank() || normalizedGroupName.isBlank()) {
            return
        }
        mutateState { state ->
            state.copy(
                conversationGroups =
                    state.conversationGroups.map { group ->
                        if (group.matchesGroup(groupName = groupName, characterId = characterId)) {
                            group.copy(name = normalizedGroupName)
                        } else {
                            group
                        }
                    },
                conversations =
                    state.conversations.map { conversation ->
                        if (conversation.matchesGroup(groupName = groupName, characterId = characterId)) {
                            conversation.copy(groupName = normalizedGroupName)
                        } else {
                            conversation
                        }
                    },
            )
        }
    }

    fun clearConversationGroup(
        groupName: String,
        characterId: String?,
    ) {
        if (groupName.isBlank()) {
            return
        }
        mutateState { state ->
            state.copy(
                conversations =
                    state.conversations.map { conversation ->
                        if (conversation.matchesGroup(groupName = groupName, characterId = characterId)) {
                            conversation.copy(groupName = null)
                        } else {
                            conversation
                        }
                    },
            )
        }
    }

    fun deleteConversationGroup(
        groupName: String,
        characterId: String?,
    ) {
        if (groupName.isBlank()) {
            return
        }
        mutateState { state ->
            val deletedConversationIds =
                state.conversations
                    .filter { it.matchesGroup(groupName = groupName, characterId = characterId) }
                    .map { it.id }
                    .toSet()
            val remainingConversations =
                state.conversations.filterNot { it.id in deletedConversationIds }
            val remainingMessages =
                state.conversationMessages.filterKeys { it !in deletedConversationIds }
            val remainingGroups =
                state.conversationGroups.filterNot { it.matchesGroup(groupName = groupName, characterId = characterId) }
            if (remainingConversations.isEmpty()) {
                createFallbackConversationState(
                    state =
                        state.copy(
                            conversationGroups = remainingGroups,
                            conversations = emptyList(),
                            conversationMessages = remainingMessages,
                        ),
                )
            } else {
                val nextActiveConversationId =
                    if (state.activeConversationId in deletedConversationIds) {
                        remainingConversations.maxByOrNull { it.sortKey }?.id ?: remainingConversations.first().id
                    } else {
                        state.activeConversationId
                    }
                state.copy(
                    conversationGroups = remainingGroups,
                    conversations = remainingConversations,
                    conversationMessages = remainingMessages,
                    activeConversationId = nextActiveConversationId,
                )
            }
        }
    }

    fun createConversationGroup(groupName: String) {
        val normalizedGroupName = groupName.trim()
        if (normalizedGroupName.isBlank()) {
            return
        }
        mutateState { state ->
            val characterId =
                when (state.historyDisplayMode) {
                    OperitReplicaHistoryDisplayMode.BY_CHARACTER_CARD,
                    OperitReplicaHistoryDisplayMode.CURRENT_CHARACTER_ONLY -> state.selectedCharacterId
                    OperitReplicaHistoryDisplayMode.BY_FOLDER -> null
                }
            val alreadyExists =
                state.conversationGroups.any { group ->
                    group.matchesGroup(groupName = normalizedGroupName, characterId = characterId)
                } ||
                    state.conversations.any { conversation ->
                        conversation.groupName == normalizedGroupName &&
                            (characterId == null || conversation.characterId == characterId)
                    }
            if (alreadyExists) {
                state
            } else {
                state.copy(
                    conversationGroups =
                        state.conversationGroups +
                            OperitReplicaConversationGroup(
                                name = normalizedGroupName,
                                characterId = characterId,
                            ),
                )
            }
        }
    }

    fun createNewConversation(prefillText: String? = null) {
        val now = System.currentTimeMillis()
        val newId = "chat-$now"
        val newConversation =
            OperitReplicaConversation(
                id = newId,
                title = "新对话",
                preview = "等待新的消息输入…",
                sortKey = now,
                groupName = null,
                characterId = _uiState.value.selectedCharacterId,
                accent = Color(0xFF6177B2),
            )
        mutateState { state ->
            state.copy(
                activeConversationId = newId,
                historySearchQuery = "",
                inputText = prefillText.orEmpty(),
                showHistoryPanel = false,
                showReplyPreview = true,
                conversations = listOf(newConversation) + state.conversations,
                conversationMessages =
                    state.conversationMessages +
                        (
                            newId to
                                buildOperitReplicaSeedMessages(
                                    assistantText = "新的会话已经创建。现在可以继续输入任务。",
                                    systemText = "这个会话由右侧正一屏本地生成，结构对齐 Operit 聊天页。",
                                )
                            ),
            )
        }
    }

    fun submitPrompt(): Boolean {
        var submitted = false
        mutateState { state ->
            val normalizedPrompt = state.inputText.trim()
            if (normalizedPrompt.isBlank()) {
                return@mutateState state
            }
            submitted = true

            val activeConversationId = state.activeConversationId
            val existingMessages =
                state.conversationMessages[activeConversationId]
                    ?: buildOperitReplicaSeedMessages(
                        assistantText = "新的会话已经创建，可以继续输入任务。",
                        systemText = "这是当前正一屏里的独立会话容器。",
                    )
            val updatedMessages =
                existingMessages +
                    OperitReplicaMessage(
                        role = OperitReplicaMessageRole.User,
                        text = normalizedPrompt,
                        meta = formatCurrentTimeText(),
                    ) +
                    OperitReplicaMessage(
                        role = OperitReplicaMessageRole.Assistant,
                        text =
                            buildString {
                                append("已收到：")
                                append(normalizedPrompt)
                                append("\n\n")
                                append("当前模型：")
                                append(state.currentModelLabel)
                                append("。")
                                append(
                                    when {
                                        state.enableThinking && state.enableTools -> "我会按 Operit 的 Agent 工作流组织下一步。"
                                        state.enableThinking -> "我会先做推理整理，再继续输出。"
                                        state.enableTools -> "当前允许工具调用，但仍保持在本地演示模式。"
                                        else -> "当前页面以纯聊天模式运行。"
                                    },
                                )
                                append("\n")
                                append(
                                    if (state.enableWorkspace) {
                                        "工作区入口已保留在输入区附近，可继续向真实实现扩展。"
                                    } else {
                                        "工作区已关闭，本页保持轻量聊天视图。"
                                    },
                                )
                            },
                        meta = formatCurrentTimeText(),
                    )

            state.copy(
                inputText = "",
                showAttachmentPanel = false,
                showReplyPreview = true,
                conversations = updateConversationPreview(state.conversations, activeConversationId, normalizedPrompt),
                conversationMessages = state.conversationMessages + (activeConversationId to updatedMessages),
            )
        }

        return submitted
    }

    fun dismissTopLevelOverlay(): Boolean {
        var dismissed = false
        mutateState { state ->
            when {
                state.showComputerPanel -> {
                    dismissed = true
                    state.copy(showComputerPanel = false)
                }
                state.isWorkspacePreparing -> {
                    dismissed = true
                    state.copy(isWorkspacePreparing = false)
                }
                state.showWorkspacePanel -> {
                    dismissed = true
                    state.copy(showWorkspacePanel = false)
                }
                state.showCharacterSelector -> {
                    dismissed = true
                    state.copy(showCharacterSelector = false)
                }
                state.showHistoryPanel -> {
                    dismissed = true
                    state.copy(showHistoryPanel = false)
                }
                state.showStatsMenu -> {
                    dismissed = true
                    state.copy(showStatsMenu = false)
                }
                state.showFullscreenEditor -> {
                    dismissed = true
                    state.copy(showFullscreenEditor = false)
                }
                state.showFeaturePanel -> {
                    dismissed = true
                    state.copy(showFeaturePanel = false)
                }
                state.showAttachmentPanel -> {
                    dismissed = true
                    state.copy(showAttachmentPanel = false)
                }
                else -> state
            }
        }
        return dismissed
    }

    private fun mutateState(transform: (KiyoriOperitReplicaUiState) -> KiyoriOperitReplicaUiState) {
        _uiState.update { state -> transform(state).withDerivedState() }
    }

    private fun buildInitialState(): KiyoriOperitReplicaUiState {
        val now = System.currentTimeMillis()
        val characterOptions =
            listOf(
                OperitReplicaCharacterOption(
                    id = "operit-agent",
                    title = "Operit",
                    subtitle = "默认 Agent 工作台",
                    sortKey = now,
                    accent = Color(0xFF6177B2),
                ),
                OperitReplicaCharacterOption(
                    id = "research-agent",
                    title = "Research",
                    subtitle = "偏向网页与信息整理",
                    sortKey = now - 60_000L,
                    accent = Color(0xFF4A8F66),
                ),
                OperitReplicaCharacterOption(
                    id = "builder-agent",
                    title = "Builder",
                    subtitle = "偏向工具调用与工作区",
                    sortKey = now - 120_000L,
                    accent = Color(0xFF9261B2),
                ),
            )
        val conversations =
            listOf(
                OperitReplicaConversation(
                    id = "chat-main",
                    title = "新对话",
                    preview = "开始新的对话。输入你的任务，我会按 Operit 的聊天工作流来处理。",
                    sortKey = now,
                    groupName = null,
                    characterId = "operit-agent",
                    accent = Color(0xFF6177B2),
                ),
                OperitReplicaConversation(
                    id = "chat-design",
                    title = "界面复刻",
                    preview = "右侧正一屏已切换为 Operit 风格聊天页。",
                    sortKey = now - 2 * 60 * 60 * 1000L,
                    groupName = "正一屏",
                    characterId = "builder-agent",
                    accent = Color(0xFF4A8F66),
                ),
                OperitReplicaConversation(
                    id = "chat-agent",
                    title = "Agent 配置",
                    preview = "保留历史面板、上下文统计和 Agent 输入区结构。",
                    sortKey = now - 26 * 60 * 60 * 1000L,
                    groupName = "架构",
                    characterId = "research-agent",
                    accent = Color(0xFF9261B2),
                ),
            )
        val conversationGroups =
            listOf(
                OperitReplicaConversationGroup(
                    name = "正一屏",
                    characterId = "builder-agent",
                    sortKey = now - 2 * 60 * 60 * 1000L,
                ),
                OperitReplicaConversationGroup(
                    name = "架构",
                    characterId = "research-agent",
                    sortKey = now - 26 * 60 * 60 * 1000L,
                ),
                OperitReplicaConversationGroup(
                    name = "待整理",
                    characterId = "operit-agent",
                    sortKey = now - 5 * 60 * 1000L,
                ),
            )

        return KiyoriOperitReplicaUiState(
            characterOptions = characterOptions,
            conversationGroups = conversationGroups,
            conversations = conversations,
            conversationMessages =
                mapOf(
                    "chat-main" to
                        buildOperitReplicaSeedMessages(
                            assistantText = "开始新的对话。输入你的任务，我会按 Operit 的聊天工作流来处理。",
                            systemText = "当前页已按 Operit 聊天页做兼容复刻，仅替换软件主页左滑进入的正一屏 UI。",
                        ),
                    "chat-design" to
                        buildOperitReplicaSeedMessages(
                            assistantText = "右侧正一屏现在承载独立聊天体验，和首页主内容分离。",
                            systemText = "这里可以继续叠加聊天历史、工具状态、附件面板和工作区入口。",
                        ),
                    "chat-agent" to
                        buildOperitReplicaSeedMessages(
                            assistantText = "Agent 输入区已切到更接近 Operit 的组织方式。",
                            systemText = "保留本地状态驱动，不把整个 Operit 依赖链搬进 Kiyori。",
                        ),
                ),
        )
    }

    private fun KiyoriOperitReplicaUiState.withDerivedState(): KiyoriOperitReplicaUiState {
        val sortedCharacterOptions = sortCharacterOptions(characterOptions, characterSortOption)
        val currentCharacterLabel =
            sortedCharacterOptions.firstOrNull { it.id == selectedCharacterId }?.title ?: "Operit"
        val activeConversation =
            conversations.firstOrNull { it.id == activeConversationId } ?: conversations.firstOrNull()
        val activeMessages =
            conversationMessages[activeConversation?.id ?: activeConversationId]
                ?: buildOperitReplicaSeedMessages(
                    assistantText = "新的会话已经创建，可以继续输入任务。",
                    systemText = "这是当前正一屏里的独立会话容器。",
                )
        val groupedConversations =
            buildConversationSections(
                conversationGroups = conversationGroups,
                conversations = conversations,
                historySearchQuery = historySearchQuery,
                historyDisplayMode = historyDisplayMode,
                characterOptions = sortedCharacterOptions,
                selectedCharacterId = selectedCharacterId,
            )
        val latestUserMessageText = activeMessages.lastOrNull { it.role == OperitReplicaMessageRole.User }?.text
        val inputTokenCount =
            (
                (
                    activeMessages.filter { it.role == OperitReplicaMessageRole.User }.sumOf { it.text.length } +
                        inputText.length
                    ) / 3
                )
                .coerceAtLeast(12)
        val outputTokenCount =
            (
                activeMessages.filter { it.role != OperitReplicaMessageRole.User }.sumOf { it.text.length } / 3
                )
                .coerceAtLeast(32)
        val currentWindowSize =
            activeMessages.sumOf { it.text.length } +
                inputText.length +
                if (enableWorkspace) 840 else 360 +
                if (enableTools) 520 else 180
        val maxWindowSize = 8192
        val contextUsagePercentage =
            ((currentWindowSize.toFloat() / maxWindowSize.toFloat()) * 100f).coerceIn(0f, 99f)
        val statusStripText =
            buildString {
                append(currentModelLabel)
                append(" | ")
                append(if (enableThinking) "推理已开启" else "推理已关闭")
                append(" | ")
                append(if (enableTools) "工具可用" else "纯聊天")
                append(" | ")
                append(if (enableWorkspace) "工作区已接入" else "未接入工作区")
                append(" | ")
                append("${attachments.size} 个附件")
                append(" | ")
                append("上下文 ${contextUsagePercentage.toInt()}%")
            }

        return copy(
            currentCharacterLabel = currentCharacterLabel,
            characterOptions = sortedCharacterOptions,
            activeConversation = activeConversation,
            activeMessages = activeMessages,
            groupedConversations = groupedConversations,
            latestUserMessageText = latestUserMessageText,
            statusStripText = statusStripText,
            inputTokenCount = inputTokenCount,
            outputTokenCount = outputTokenCount,
            currentWindowSize = currentWindowSize,
            contextUsagePercentage = contextUsagePercentage,
        )
    }

    private fun updateConversationPreview(
        conversations: List<OperitReplicaConversation>,
        conversationId: String,
        prompt: String,
    ): List<OperitReplicaConversation> {
        return conversations
            .map { conversation ->
                if (conversation.id == conversationId) {
                    val normalizedTitle =
                        if (conversation.title.startsWith("新对话")) {
                            prompt.take(12)
                        } else {
                            conversation.title
                        }
                    conversation.copy(
                        title = normalizedTitle.ifBlank { conversation.title },
                        preview = prompt,
                        sortKey = System.currentTimeMillis(),
                    )
                } else {
                    conversation
                }
            }
            .sortedByDescending { it.sortKey }
    }

    private fun moveConversation(
        conversationId: String,
        direction: Int,
    ) {
        mutateState { state ->
            val orderedConversations = state.conversations.sortedByDescending { it.sortKey }.toMutableList()
            val currentIndex = orderedConversations.indexOfFirst { it.id == conversationId }
            if (currentIndex == -1) {
                return@mutateState state
            }
            val targetIndex = currentIndex + direction
            if (targetIndex !in orderedConversations.indices) {
                return@mutateState state
            }
            val movedConversation = orderedConversations.removeAt(currentIndex)
            orderedConversations.add(targetIndex, movedConversation)
            state.copy(conversations = reassignConversationOrder(orderedConversations))
        }
    }

    private fun reassignConversationOrder(conversations: List<OperitReplicaConversation>): List<OperitReplicaConversation> {
        val baseSortKey = System.currentTimeMillis() + conversations.size
        return conversations.mapIndexed { index, conversation ->
            conversation.copy(sortKey = baseSortKey - index)
        }
    }

    private fun buildOperitReplicaSeedMessages(
        assistantText: String,
        systemText: String,
    ): List<OperitReplicaMessage> {
        return listOf(
            OperitReplicaMessage(
                role = OperitReplicaMessageRole.Assistant,
                text = assistantText,
                meta = "Agent",
            ),
            OperitReplicaMessage(
                role = OperitReplicaMessageRole.System,
                text = systemText,
                meta = "Init",
            ),
        )
    }

    private fun buildConversationSections(
        conversationGroups: List<OperitReplicaConversationGroup>,
        conversations: List<OperitReplicaConversation>,
        historySearchQuery: String,
        historyDisplayMode: OperitReplicaHistoryDisplayMode,
        characterOptions: List<OperitReplicaCharacterOption>,
        selectedCharacterId: String,
    ): List<OperitReplicaConversationSection> {
        val modeFilteredConversations =
            when (historyDisplayMode) {
                OperitReplicaHistoryDisplayMode.CURRENT_CHARACTER_ONLY ->
                    conversations.filter { it.characterId == selectedCharacterId }
                else -> conversations
            }
        val visibleConversations =
            if (historySearchQuery.isBlank()) {
                modeFilteredConversations
            } else {
                modeFilteredConversations.filter {
                    it.title.contains(historySearchQuery, ignoreCase = true) ||
                        it.preview.contains(historySearchQuery, ignoreCase = true) ||
                        (it.groupName?.contains(historySearchQuery, ignoreCase = true) == true)
                }
            }
        val visibleConversationGroups =
            if (historySearchQuery.isBlank()) {
                conversationGroups
            } else {
                conversationGroups.filter { group ->
                    group.name.contains(historySearchQuery, ignoreCase = true)
                }
            }
        val sortedConversations = visibleConversations.sortedByDescending { it.sortKey }
        val characterById = characterOptions.associateBy { it.id }
        val currentCharacterOption = characterById[selectedCharacterId]

        return when (historyDisplayMode) {
            OperitReplicaHistoryDisplayMode.BY_CHARACTER_CARD ->
                buildCharacterConversationSections(
                    conversationGroups = visibleConversationGroups,
                    conversations = sortedConversations,
                    characterOptions = characterOptions,
                )
            else ->
                buildGroupSections(
                    conversationGroups = visibleConversationGroups,
                    conversations = sortedConversations,
                    characterById = characterById,
                    activeCharacterId =
                        if (historyDisplayMode == OperitReplicaHistoryDisplayMode.CURRENT_CHARACTER_ONLY) {
                            selectedCharacterId
                        } else {
                            null
                        },
                    sectionSubtitle =
                        if (historyDisplayMode == OperitReplicaHistoryDisplayMode.CURRENT_CHARACTER_ONLY) {
                            currentCharacterOption?.title ?: "当前角色卡"
                        } else {
                            null
                        },
                )
        }
    }

    private fun buildCharacterConversationSections(
        conversationGroups: List<OperitReplicaConversationGroup>,
        conversations: List<OperitReplicaConversation>,
        characterOptions: List<OperitReplicaCharacterOption>,
    ): List<OperitReplicaConversationSection> {
        val characterById = characterOptions.associateBy { it.id }
        val orderedCharacterIds = characterOptions.map { it.id }
        val groupedByCharacter = conversations.groupBy { it.characterId }
        val groupedDefinitionsByCharacter = conversationGroups.groupBy { it.characterId }
        val dynamicCharacterIds =
            (groupedByCharacter.keys.toList() + groupedDefinitionsByCharacter.keys.toList())
                .filterNotNull()
                .distinct()
        val orderedIds =
            orderedCharacterIds.filter { groupedByCharacter.containsKey(it) || groupedDefinitionsByCharacter.containsKey(it) } +
                dynamicCharacterIds
                    .filterNot { it in orderedCharacterIds }
                    .sorted()

        return orderedIds.map { characterId ->
            val firstConversation = groupedByCharacter[characterId].orEmpty().firstOrNull()
            val bucket = resolveCharacterBucket(characterId, firstConversation, characterById)
            OperitReplicaConversationSection(
                key = "character::${bucket.sectionKey}",
                title = bucket.title,
                type = OperitReplicaConversationSectionType.CHARACTER,
                subtitle = bucket.subtitle,
                bindingKind = bucket.bindingKind,
                accent = bucket.accent,
                characterId = characterId,
                childSections =
                    buildGroupSections(
                        conversationGroups = groupedDefinitionsByCharacter[characterId].orEmpty(),
                        conversations = groupedByCharacter[characterId].orEmpty(),
                        characterById = characterById,
                        activeCharacterId = characterId,
                    ),
            )
        }
    }

    private fun buildGroupSections(
        conversationGroups: List<OperitReplicaConversationGroup>,
        conversations: List<OperitReplicaConversation>,
        characterById: Map<String, OperitReplicaCharacterOption>,
        activeCharacterId: String? = null,
        sectionSubtitle: String? = null,
    ): List<OperitReplicaConversationSection> {
        val scopedGroups =
            conversationGroups.filter { group ->
                activeCharacterId == null || group.characterId == activeCharacterId
            }
        val groupedConversations = conversations.groupBy { it.groupName ?: "未分组" }
        val groupNames =
            (groupedConversations.keys + scopedGroups.map { it.name })
                .distinct()
                .sortedWith(
                    compareBy<String> { if (it == "未分组") 1 else 0 }.thenBy { name ->
                        val groupSortKey = scopedGroups.firstOrNull { it.name == name }?.sortKey
                        if (groupSortKey != null) {
                            -groupSortKey
                        } else {
                            Long.MAX_VALUE
                        }
                    }.thenBy { it },
                )

        return groupNames.map { groupName ->
            val groupedItems = groupedConversations[groupName].orEmpty()
            val firstConversation = groupedItems.firstOrNull()
            val groupDefinition = scopedGroups.firstOrNull { it.name == groupName }
            val derivedCharacterId = firstConversation?.characterId ?: groupDefinition?.characterId
            val derivedAccent =
                if (derivedCharacterId != null) {
                    characterById[derivedCharacterId]?.accent ?: firstConversation?.accent ?: Color(0xFF98A2B3)
                } else {
                    firstConversation?.accent ?: Color(0xFF98A2B3)
                }
                OperitReplicaConversationSection(
                    key = "group::${derivedCharacterId ?: "all"}::$groupName",
                    title = groupName,
                    type = OperitReplicaConversationSectionType.GROUP,
                    subtitle = sectionSubtitle,
                    bindingKind = OperitReplicaConversationBindingKind.GROUP_ONLY,
                    accent = derivedAccent,
                    characterId = derivedCharacterId,
                    groupName = if (groupName == "未分组") null else groupName,
                    conversations = groupedItems.sortedByDescending { it.sortKey },
                )
        }
    }

    private data class CharacterBucketDescriptor(
        val sectionKey: String,
        val title: String,
        val subtitle: String,
        val bindingKind: OperitReplicaConversationBindingKind,
        val accent: Color,
    )

    private fun resolveCharacterBucket(
        characterId: String,
        conversation: OperitReplicaConversation?,
        characterById: Map<String, OperitReplicaCharacterOption>,
    ): CharacterBucketDescriptor {
        val character = characterById[characterId]
        val characterGroupName = conversation?.characterGroupName?.trim().orEmpty()
        return when {
            characterGroupName.isNotBlank() ->
                CharacterBucketDescriptor(
                    sectionKey = "group-binding::$characterGroupName",
                    title = "角色组绑定: $characterGroupName",
                    subtitle = "角色组绑定桶",
                    bindingKind = OperitReplicaConversationBindingKind.CHARACTER_GROUP,
                    accent = conversation?.accent ?: character?.accent ?: Color(0xFF98A2B3),
                )
            character != null ->
                CharacterBucketDescriptor(
                    sectionKey = "card::$characterId",
                    title = character.title,
                    subtitle = character.subtitle,
                    bindingKind = OperitReplicaConversationBindingKind.CHARACTER_CARD,
                    accent = character.accent,
                )
            else ->
                CharacterBucketDescriptor(
                    sectionKey = "unbound::$characterId",
                    title = "未绑定角色卡",
                    subtitle = "未分配到角色卡或角色组",
                    bindingKind = OperitReplicaConversationBindingKind.UNBOUND,
                    accent = conversation?.accent ?: Color(0xFF98A2B3),
                )
        }
    }

    private fun sortCharacterOptions(
        characterOptions: List<OperitReplicaCharacterOption>,
        sortOption: OperitReplicaCharacterSortOption,
    ): List<OperitReplicaCharacterOption> {
        return when (sortOption) {
            OperitReplicaCharacterSortOption.DEFAULT -> characterOptions
            OperitReplicaCharacterSortOption.NAME_ASC -> characterOptions.sortedBy { it.title.lowercase() }
            OperitReplicaCharacterSortOption.CREATED_DESC -> characterOptions.sortedByDescending { it.sortKey }
        }
    }

    private fun OperitReplicaConversation.matchesGroup(
        groupName: String,
        characterId: String?,
    ): Boolean {
        if (this.groupName != groupName) {
            return false
        }
        return characterId == null || this.characterId == characterId
    }

    private fun OperitReplicaConversationGroup.matchesGroup(
        groupName: String,
        characterId: String?,
    ): Boolean {
        if (name != groupName) {
            return false
        }
        return characterId == null || this.characterId == characterId
    }

    private fun createFallbackConversationState(
        state: KiyoriOperitReplicaUiState,
    ): KiyoriOperitReplicaUiState {
        val now = System.currentTimeMillis()
        val conversationId = "chat-$now"
        val fallbackConversation =
            OperitReplicaConversation(
                id = conversationId,
                title = "新对话",
                preview = "等待新的消息输入…",
                sortKey = now,
                groupName = null,
                characterId = state.selectedCharacterId,
                accent = Color(0xFF6177B2),
            )
        return state.copy(
            activeConversationId = conversationId,
            showReplyPreview = true,
            conversations = listOf(fallbackConversation),
            conversationMessages =
                state.conversationMessages +
                    (
                        conversationId to
                            buildOperitReplicaSeedMessages(
                                assistantText = "新的会话已经创建。现在可以继续输入任务。",
                                systemText = "这是当前正一屏里的独立会话容器。",
                            )
                        ),
        )
    }

    private fun formatCurrentTimeText(): String =
        android.text.format.DateFormat.format("HH:mm:ss", System.currentTimeMillis()).toString()
}
