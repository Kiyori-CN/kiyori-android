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
import com.ai.assistance.operit.ui.features.chat.components.style.input.common.PendingQueueMessageItem
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
    private var inputProcessingJob: Job? = null
    private var nextPendingQueueId: Long = 1L

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

    fun toggleModelSelectorPopup() {
        mutateState { state ->
            state.copy(
                showModelSelectorPopup = !state.showModelSelectorPopup,
                showFeaturePanel = false,
                showAttachmentPanel = false,
            )
        }
    }

    fun setModelSelectorPopupVisible(visible: Boolean) {
        mutateState { it.copy(showModelSelectorPopup = visible) }
    }

    fun selectModelLabel(modelLabel: String) {
        mutateState { state ->
            if (modelLabel !in state.availableModelLabels) {
                state
            } else {
                state.copy(currentModelLabel = modelLabel, showModelSelectorPopup = false)
            }
        }
    }

    fun toggleStatsMenu() {
        mutateState { it.copy(showStatsMenu = !it.showStatsMenu) }
    }

    fun toggleFeaturePanel() {
        mutateState { state ->
            state.copy(
                showFeaturePanel = !state.showFeaturePanel,
                showAttachmentPanel = false,
                showModelSelectorPopup = false,
            )
        }
    }

    fun setFeaturePanelVisible(visible: Boolean) {
        mutateState { it.copy(showFeaturePanel = visible) }
    }

    fun toggleAttachmentPanel() {
        mutateState { state ->
            state.copy(
                showAttachmentPanel = !state.showAttachmentPanel,
                showFeaturePanel = false,
                showModelSelectorPopup = false,
            )
        }
    }

    fun setAttachmentPanelVisible(visible: Boolean) {
        mutateState { it.copy(showAttachmentPanel = visible) }
    }

    fun setFullscreenEditorVisible(visible: Boolean) {
        mutateState { it.copy(showFullscreenEditor = visible) }
    }

    fun setPendingQueueExpanded(expanded: Boolean) {
        mutateState { it.copy(isPendingQueueExpanded = expanded) }
    }

    fun enqueuePendingPrompt(): Boolean {
        var enqueued = false
        mutateState { state ->
            val normalizedPrompt = state.inputText.trim()
            if (normalizedPrompt.isBlank()) {
                return@mutateState state
            }
            enqueued = true
            val item = PendingQueueMessageItem(id = nextPendingQueueId++, text = normalizedPrompt)
            state.copy(
                inputText = "",
                showAttachmentPanel = false,
                showModelSelectorPopup = false,
                pendingQueueMessages = state.pendingQueueMessages + item,
                isPendingQueueExpanded = true,
            )
        }
        return enqueued
    }

    fun deletePendingQueueMessage(id: Long) {
        mutateState { state ->
            state.copy(pendingQueueMessages = state.pendingQueueMessages.filterNot { it.id == id })
        }
    }

    fun editPendingQueueMessage(id: Long) {
        mutateState { state ->
            val item = state.pendingQueueMessages.firstOrNull { it.id == id } ?: return@mutateState state
            state.copy(
                inputText = item.text,
                pendingQueueMessages = state.pendingQueueMessages.filterNot { it.id == id },
            )
        }
    }

    fun sendPendingQueueMessage(id: Long) {
        val item = _uiState.value.pendingQueueMessages.firstOrNull { it.id == id } ?: return
        mutateState { state ->
            state.copy(pendingQueueMessages = state.pendingQueueMessages.filterNot { it.id == id })
        }
        if (_uiState.value.isInputProcessing) {
            cancelInputProcessing()
        }
        beginPromptSubmission(item.text)
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

    fun setThinkingQualityLevel(level: Int) {
        mutateState { it.copy(thinkingQualityLevel = level.coerceIn(1, 4)) }
    }

    fun setMaxContextModeEnabled(enabled: Boolean) {
        mutateState { it.copy(enableMaxContextMode = enabled) }
    }

    fun setMemoryAutoUpdateEnabled(enabled: Boolean) {
        mutateState { it.copy(enableMemoryAutoUpdate = enabled) }
    }

    fun setAutoReadEnabled(enabled: Boolean) {
        mutateState { it.copy(isAutoReadEnabled = enabled) }
    }

    fun setAutoApproveEnabled(enabled: Boolean) {
        mutateState { it.copy(isAutoApproveEnabled = enabled) }
    }

    fun setDisableStreamOutput(enabled: Boolean) {
        mutateState { it.copy(disableStreamOutput = enabled) }
    }

    fun setDisableUserPreferenceDescription(enabled: Boolean) {
        mutateState { it.copy(disableUserPreferenceDescription = enabled) }
    }

    fun setDisableStatusTags(enabled: Boolean) {
        mutateState { it.copy(disableStatusTags = enabled) }
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
                preview = "等待新的消息输入...",
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
                conversations = listOf(newConversation) + state.conversations,
                conversationMessages = state.conversationMessages + (newId to emptyList()),
            )
        }
    }
    fun submitPrompt(): Boolean {
        return beginPromptSubmission(_uiState.value.inputText.trim())
    }

    fun updateMessage(
        index: Int,
        content: String,
    ) {
        mutateState { state ->
            val conversationId = state.activeConversationId
            val messages = state.conversationMessages[conversationId] ?: return@mutateState state
            if (index !in messages.indices) {
                return@mutateState state
            }
            state.copy(
                conversationMessages =
                    state.conversationMessages +
                        (conversationId to messages.mapIndexed { messageIndex, message ->
                            if (messageIndex == index) {
                                message.copy(text = content, meta = formatCurrentTimeText())
                            } else {
                                message
                            }
                        }),
            )
        }
    }

    fun rewindAndResendMessage(
        index: Int,
        content: String,
    ): Boolean {
        var promptToSend: String? = null
        mutateState { state ->
            val conversationId = state.activeConversationId
            val messages = state.conversationMessages[conversationId] ?: return@mutateState state
            if (index !in messages.indices || messages[index].role != OperitReplicaMessageRole.User) {
                return@mutateState state
            }
            promptToSend = content.trim()
            if (promptToSend.isNullOrBlank()) {
                return@mutateState state
            }
            state.copy(
                conversationMessages = state.conversationMessages + (conversationId to messages.take(index)),
            )
        }
        return promptToSend?.let(::beginPromptSubmission) ?: false
    }

    fun deleteMessage(index: Int) {
        mutateState { state ->
            val conversationId = state.activeConversationId
            val messages = state.conversationMessages[conversationId] ?: return@mutateState state
            if (index !in messages.indices) {
                return@mutateState state
            }
            state.copy(
                conversationMessages =
                    state.conversationMessages +
                        (conversationId to messages.filterIndexed { messageIndex, _ -> messageIndex != index }),
            )
        }
    }

    fun deleteMessages(indices: Set<Int>) {
        if (indices.isEmpty()) {
            return
        }
        mutateState { state ->
            val conversationId = state.activeConversationId
            val messages = state.conversationMessages[conversationId] ?: return@mutateState state
            state.copy(
                conversationMessages =
                    state.conversationMessages +
                        (conversationId to messages.filterIndexed { index, _ -> index !in indices }),
            )
        }
    }

    fun rollbackToMessage(index: Int) {
        mutateState { state ->
            val conversationId = state.activeConversationId
            val messages = state.conversationMessages[conversationId] ?: return@mutateState state
            if (index !in messages.indices) {
                return@mutateState state
            }
            state.copy(
                conversationMessages = state.conversationMessages + (conversationId to messages.take(index + 1)),
            )
        }
    }

    fun regenerateMessage(index: Int) {
        inputProcessingJob?.cancel()
        val state = _uiState.value
        val conversationId = state.activeConversationId
        val messages = state.conversationMessages[conversationId].orEmpty()
        if (index !in messages.indices || messages[index].role != OperitReplicaMessageRole.Assistant) {
            return
        }
        mutateState {
            it.copy(
                isInputProcessing = true,
                inputProcessingLabel = "Regenerating...",
                inputProcessingProgress = 0.56f,
            )
        }
        inputProcessingJob =
            viewModelScope.launch {
                delay(520)
                mutateState { currentState ->
                    val latestMessages = currentState.conversationMessages[conversationId] ?: return@mutateState currentState
                    if (index !in latestMessages.indices) {
                        return@mutateState currentState.copy(
                            isInputProcessing = false,
                            inputProcessingLabel = "",
                            inputProcessingProgress = 0f,
                        )
                    }
                    currentState.copy(
                        isInputProcessing = false,
                        inputProcessingLabel = "",
                        inputProcessingProgress = 0f,
                        conversationMessages =
                            currentState.conversationMessages +
                                (
                                    conversationId to
                                        latestMessages.mapIndexed { messageIndex, message ->
                                            if (messageIndex == index) {
                                                message.copy(
                                                    text = "${message.text}\n\n已重新生成。",
                                                    meta = formatCurrentTimeText(),
                                                )
                                            } else {
                                                message
                                            }
                                        }
                                    ),
                    )
                }
                inputProcessingJob = null
            }
    }

    fun insertSummaryAfter(index: Int) {
        mutateState { state ->
            val conversationId = state.activeConversationId
            val messages = state.conversationMessages[conversationId] ?: return@mutateState state
            if (index !in messages.indices) {
                return@mutateState state
            }
            val summary =
                OperitReplicaMessage(
                    role = OperitReplicaMessageRole.System,
                    text = "Summary inserted at message ${index + 1}",
                    meta = formatCurrentTimeText(),
                )
            state.copy(
                conversationMessages =
                    state.conversationMessages +
                        (conversationId to messages.take(index + 1) + summary + messages.drop(index + 1)),
            )
        }
    }

    fun createBranchFromMessage(index: Int) {
        val state = _uiState.value
        val sourceMessages = state.conversationMessages[state.activeConversationId] ?: return
        if (index !in sourceMessages.indices) {
            return
        }
        val now = System.currentTimeMillis()
        val newId = "chat-branch-$now"
        val branchConversation =
            OperitReplicaConversation(
                id = newId,
                title = "分支 ${index + 1}",
                preview = sourceMessages[index].text.take(24),
                sortKey = now,
                groupName = state.activeConversation?.groupName,
                characterId = state.selectedCharacterId,
                accent = Color(0xFF6177B2),
            )
        mutateState {
            it.copy(
                activeConversationId = newId,
                conversations = listOf(branchConversation) + it.conversations,
                conversationMessages = it.conversationMessages + (newId to sourceMessages.take(index + 1)),
            )
        }
    }

    private fun beginPromptSubmission(prompt: String): Boolean {
        if (prompt.isBlank()) {
            return false
        }

        var scheduledConversationId: String? = null
        mutateState { state ->
            val activeConversationId = state.activeConversationId
            scheduledConversationId = activeConversationId
            val existingMessages = state.conversationMessages[activeConversationId] ?: emptyList()
            val updatedMessages =
                existingMessages +
                    OperitReplicaMessage(
                        role = OperitReplicaMessageRole.User,
                        text = prompt,
                        meta = formatCurrentTimeText(),
                    )

            state.copy(
                inputText = "",
                showModelSelectorPopup = false,
                showAttachmentPanel = false,
                isInputProcessing = true,
                inputProcessingLabel = "Connecting...",
                inputProcessingProgress = 0.34f,
                conversations = updateConversationPreview(state.conversations, activeConversationId, prompt),
                conversationMessages = state.conversationMessages + (activeConversationId to updatedMessages),
            )
        }

        val conversationId = scheduledConversationId ?: return false
        launchPromptProcessing(conversationId = conversationId, prompt = prompt)
        return true
    }

    private fun launchPromptProcessing(
        conversationId: String,
        prompt: String,
    ) {
        inputProcessingJob?.cancel()
        inputProcessingJob =
            viewModelScope.launch {
                delay(320)
                mutateState {
                    it.copy(
                        isInputProcessing = true,
                        inputProcessingLabel = "Generating response...",
                        inputProcessingProgress = 0.72f,
                    )
                }
                delay(520)
                var nextQueuedPrompt: String? = null
                mutateState { state ->
                    val existingMessages = state.conversationMessages[conversationId] ?: emptyList()
                    val nextQueuedItem = state.pendingQueueMessages.firstOrNull()
                    nextQueuedPrompt = nextQueuedItem?.text
                    state.copy(
                        isInputProcessing = false,
                        inputProcessingLabel = "",
                        inputProcessingProgress = 0f,
                        pendingQueueMessages =
                            if (nextQueuedItem != null) {
                                state.pendingQueueMessages.drop(1)
                            } else {
                                state.pendingQueueMessages
                            },
                        conversationMessages =
                            state.conversationMessages +
                                (
                                    conversationId to
                                        (
                                            existingMessages +
                                                OperitReplicaMessage(
                                                    role = OperitReplicaMessageRole.Assistant,
                                                    text = buildAssistantReplicaReply(prompt),
                                                    meta = formatCurrentTimeText(),
                                                )
                                        )
                                    ),
                    )
                }
                inputProcessingJob = null
                nextQueuedPrompt?.let { queuedPrompt ->
                    delay(180)
                    beginPromptSubmission(queuedPrompt)
                }
            }
    }

    fun cancelInputProcessing() {
        inputProcessingJob?.cancel()
        inputProcessingJob = null
        mutateState {
            it.copy(
                isInputProcessing = false,
                inputProcessingLabel = "",
                inputProcessingProgress = 0f,
            )
        }
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
                state.showModelSelectorPopup -> {
                    dismissed = true
                    state.copy(showModelSelectorPopup = false)
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
                    preview = "等待新的消息输入...",
                    sortKey = now,
                    groupName = null,
                    characterId = "operit-agent",
                    accent = Color(0xFF6177B2),
                ),
                OperitReplicaConversation(
                    id = "chat-design",
                    title = "界面复刻",
                    preview = "等待新的消息输入...",
                    sortKey = now - 2 * 60 * 60 * 1000L,
                    groupName = "正一屏",
                    characterId = "builder-agent",
                    accent = Color(0xFF4A8F66),
                ),
                OperitReplicaConversation(
                    id = "chat-agent",
                    title = "Agent 配置",
                    preview = "等待新的消息输入...",
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
            availableModelLabels =
                listOf(
                    "DeepSeek / Agent",
                    "Claude / Reasoning",
                    "Gemini / Flash",
                ),
            characterOptions = characterOptions,
            conversationGroups = conversationGroups,
            conversations = conversations,
            conversationMessages =
                mapOf(
                    "chat-main" to emptyList(),
                    "chat-design" to emptyList(),
                    "chat-agent" to emptyList(),
                ),
        )
    }

    private fun KiyoriOperitReplicaUiState.withDerivedState(): KiyoriOperitReplicaUiState {
        val sortedCharacterOptions = sortCharacterOptions(characterOptions, characterSortOption)
        val currentCharacterLabel =
            sortedCharacterOptions.firstOrNull { it.id == selectedCharacterId }?.title ?: "Operit"
        val activeConversation =
            conversations.firstOrNull { it.id == activeConversationId } ?: conversations.firstOrNull()
        val activeMessages = conversationMessages[activeConversation?.id ?: activeConversationId] ?: emptyList()
        val groupedConversations =
            buildConversationSections(
                conversationGroups = conversationGroups,
                conversations = conversations,
                historySearchQuery = historySearchQuery,
                historyDisplayMode = historyDisplayMode,
                characterOptions = sortedCharacterOptions,
                selectedCharacterId = selectedCharacterId,
            )
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
        return copy(
            currentCharacterLabel = currentCharacterLabel,
            characterOptions = sortedCharacterOptions,
            activeConversation = activeConversation,
            activeMessages = activeMessages,
            groupedConversations = groupedConversations,
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
                        if (conversation.title.startsWith("æ–°å¯¹è¯")) {
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
                            currentCharacterOption?.title ?: "å½“å‰è§’è‰²å¡"
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
        val groupedConversations = conversations.groupBy { it.groupName ?: "æœªåˆ†ç»„" }
        val groupNames =
            (groupedConversations.keys + scopedGroups.map { it.name })
                .distinct()
                .sortedWith(
                    compareBy<String> { if (it == "æœªåˆ†ç»„") 1 else 0 }.thenBy { name ->
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
                    groupName = if (groupName == "æœªåˆ†ç»„") null else groupName,
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
                    title = "è§’è‰²ç»„ç»‘å®š: $characterGroupName",
                    subtitle = "è§’è‰²ç»„ç»‘å®šæ¡¶",
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
                    title = "æœªç»‘å®šè§’è‰²å¡",
                    subtitle = "æœªåˆ†é…åˆ°è§’è‰²å¡æˆ–è§’è‰²ç»„",
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
                preview = "等待新的消息输入...",
                sortKey = now,
                groupName = null,
                characterId = state.selectedCharacterId,
                accent = Color(0xFF6177B2),
            )
        return state.copy(
            activeConversationId = conversationId,
            conversations = listOf(fallbackConversation),
            conversationMessages = state.conversationMessages + (conversationId to emptyList()),
        )
    }

    private fun formatCurrentTimeText(): String =
        android.text.format.DateFormat.format("HH:mm:ss", System.currentTimeMillis()).toString()

    private fun buildAssistantReplicaReply(prompt: String): String {
        return when {
            prompt.length <= 18 -> "已收到，继续处理：$prompt"
            else -> "已收到，继续处理：${prompt.take(18)}..."
        }
    }
}

