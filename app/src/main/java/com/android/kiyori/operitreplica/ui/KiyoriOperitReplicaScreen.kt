package com.android.kiyori.operitreplica.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.kiyori.operitreplica.state.KiyoriOperitReplicaViewModel
import com.android.kiyori.operitreplica.ui.components.KiyoriOperitReplicaChatContent
import com.android.kiyori.operitreplica.ui.components.KiyoriOperitReplicaInputBar
import com.android.kiyori.operitreplica.ui.components.KiyoriOperitReplicaOverlayHost

@Composable
fun KiyoriOperitReplicaScreen(
    bottomDockPadding: Dp = 0.dp,
    onExpandChatClick: (() -> Unit)? = null,
    onAuxiliaryActionClick: (() -> Unit)? = null,
    immersiveMode: Boolean = true,
) {
    val replicaViewModel: KiyoriOperitReplicaViewModel = viewModel()
    val replicaUiState by replicaViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val currentCharacterLabel = replicaUiState.currentCharacterLabel
    val currentModelLabel = replicaUiState.currentModelLabel
    val currentMemoryProfileLabel = replicaUiState.currentMemoryProfileLabel
    val selectedCharacterId = replicaUiState.selectedCharacterId
    val historySearchQuery = replicaUiState.historySearchQuery
    val inputText = replicaUiState.inputText
    val showHistoryPanel = replicaUiState.showHistoryPanel
    val showStatsMenu = replicaUiState.showStatsMenu
    val showModelSelectorPopup = replicaUiState.showModelSelectorPopup
    val showFeaturePanel = replicaUiState.showFeaturePanel
    val showAttachmentPanel = replicaUiState.showAttachmentPanel
    val showFullscreenEditor = replicaUiState.showFullscreenEditor
    val isInputProcessing = replicaUiState.isInputProcessing
    val inputProcessingLabel = replicaUiState.inputProcessingLabel
    val inputProcessingProgress = replicaUiState.inputProcessingProgress
    val pendingQueueMessages = replicaUiState.pendingQueueMessages
    val isPendingQueueExpanded = replicaUiState.isPendingQueueExpanded
    val showCharacterSelector = replicaUiState.showCharacterSelector
    val showWorkspacePanel = replicaUiState.showWorkspacePanel
    val hasEverOpenedWorkspace = replicaUiState.hasEverOpenedWorkspace
    val isWorkspacePreparing = replicaUiState.isWorkspacePreparing
    val workspaceReloadVersion = replicaUiState.workspaceReloadVersion
    val showComputerPanel = replicaUiState.showComputerPanel
    val showHistorySearchBox = replicaUiState.showHistorySearchBox
    val showHistorySettingsDialog = replicaUiState.showHistorySettingsDialog
    val showSwipeHint = replicaUiState.showSwipeHint
    val historyDisplayMode = replicaUiState.historyDisplayMode
    val autoSwitchCharacterCard = replicaUiState.autoSwitchCharacterCard
    val autoSwitchChatOnCharacterSelect = replicaUiState.autoSwitchChatOnCharacterSelect
    val inputTokenCount = replicaUiState.inputTokenCount
    val outputTokenCount = replicaUiState.outputTokenCount
    val currentWindowSize = replicaUiState.currentWindowSize
    val contextUsagePercentage = replicaUiState.contextUsagePercentage
    val enableThinking = replicaUiState.enableThinking
    val enableTools = replicaUiState.enableTools
    val enableMemory = replicaUiState.enableMemory
    val enableStream = replicaUiState.enableStream
    val enableVoice = replicaUiState.enableVoice
    val enableWorkspace = replicaUiState.enableWorkspace
    val enableNotification = replicaUiState.enableNotification
    val thinkingQualityLevel = replicaUiState.thinkingQualityLevel
    val enableMaxContextMode = replicaUiState.enableMaxContextMode
    val baseContextLengthInK = replicaUiState.baseContextLengthInK
    val maxContextLengthInK = replicaUiState.maxContextLengthInK
    val enableMemoryAutoUpdate = replicaUiState.enableMemoryAutoUpdate
    val isAutoReadEnabled = replicaUiState.isAutoReadEnabled
    val isAutoApproveEnabled = replicaUiState.isAutoApproveEnabled
    val disableStreamOutput = replicaUiState.disableStreamOutput
    val disableUserPreferenceDescription = replicaUiState.disableUserPreferenceDescription
    val disableStatusTags = replicaUiState.disableStatusTags
    val characterSortOption = replicaUiState.characterSortOption
    val attachments = replicaUiState.attachments
    val availableModelLabels = replicaUiState.availableModelLabels
    val characterOptions = replicaUiState.characterOptions
    val activeConversation = replicaUiState.activeConversation
    val activeMessages = replicaUiState.activeMessages
    val groupedConversations = replicaUiState.groupedConversations

    val usageProgress by animateFloatAsState(
        targetValue = (contextUsagePercentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 280),
        label = "operit_context_usage",
    )
    val placeholderClick = remember(context) {
        {
            android.widget.Toast.makeText(
                context,
                "该交互将在后续接入真实能力",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }
    fun createNewConversation(prefillText: String? = null) {
        replicaViewModel.createNewConversation(prefillText)
    }

    fun submitPrompt() {
        val submitted = replicaViewModel.submitPrompt()
        if (!submitted) {
            placeholderClick()
        }
    }

    BackHandler(
        showHistoryPanel ||
            showStatsMenu ||
            showFullscreenEditor ||
            showModelSelectorPopup ||
            showAttachmentPanel ||
            showFeaturePanel ||
            showCharacterSelector ||
            showWorkspacePanel ||
            isWorkspacePreparing ||
            showComputerPanel,
    ) {
        replicaViewModel.dismissTopLevelOverlay()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.White),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.White),
        ) {
            KiyoriOperitReplicaChatContent(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                activeConversationId = replicaUiState.activeConversationId,
                bottomDockPadding = bottomDockPadding,
                immersiveMode = immersiveMode,
                showHistoryPanel = showHistoryPanel,
                currentCharacterLabel = currentCharacterLabel,
                usageProgress = usageProgress,
                contextUsagePercentage = contextUsagePercentage,
                currentWindowSize = currentWindowSize,
                inputTokenCount = inputTokenCount,
                outputTokenCount = outputTokenCount,
                showStatsMenu = showStatsMenu,
                isInputProcessing = isInputProcessing,
                activeMessages = activeMessages,
                onHistoryClick = { replicaViewModel.setHistoryPanelVisible(true) },
                onPipClick = { replicaViewModel.setComputerPanelVisible(true) },
                onCharacterClick = { replicaViewModel.setCharacterSelectorVisible(true) },
                onStatsMenuDismiss = { replicaViewModel.setStatsMenuVisible(false) },
                onStatsMenuToggle = replicaViewModel::toggleStatsMenu,
                onUpdateMessage = replicaViewModel::updateMessage,
                onRewindAndResendMessage = replicaViewModel::rewindAndResendMessage,
                onDeleteMessage = replicaViewModel::deleteMessage,
                onDeleteMessages = replicaViewModel::deleteMessages,
                onRollbackToMessage = replicaViewModel::rollbackToMessage,
                onRegenerateMessage = replicaViewModel::regenerateMessage,
                onInsertSummary = replicaViewModel::insertSummaryAfter,
                onCreateBranch = replicaViewModel::createBranchFromMessage,
            )
        }

        KiyoriOperitReplicaInputBar(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 4.dp, bottom = bottomDockPadding)
                    .navigationBarsPadding(),
            inputText = inputText,
            currentModelLabel = currentModelLabel,
            availableModelLabels = availableModelLabels,
            currentMemoryProfileLabel = currentMemoryProfileLabel,
            attachments = attachments,
            showModelSelectorPopup = showModelSelectorPopup,
            showFeaturePanel = showFeaturePanel,
            showAttachmentPanel = showAttachmentPanel,
            isInputProcessing = isInputProcessing,
            inputProcessingLabel = inputProcessingLabel,
            inputProcessingProgress = inputProcessingProgress,
            enableTools = enableTools,
            enableThinking = enableThinking,
            thinkingQualityLevel = thinkingQualityLevel,
            enableMaxContextMode = enableMaxContextMode,
            baseContextLengthInK = baseContextLengthInK,
            maxContextLengthInK = maxContextLengthInK,
            enableMemoryAutoUpdate = enableMemoryAutoUpdate,
            isAutoReadEnabled = isAutoReadEnabled,
            isAutoApproveEnabled = isAutoApproveEnabled,
            disableStreamOutput = disableStreamOutput,
            disableUserPreferenceDescription = disableUserPreferenceDescription,
            disableStatusTags = disableStatusTags,
            pendingQueueMessages = pendingQueueMessages,
            isPendingQueueExpanded = isPendingQueueExpanded,
            onInputTextChange = replicaViewModel::updateInputText,
            onRemoveAttachment = replicaViewModel::removeAttachment,
            onToggleModelSelectorPopup = replicaViewModel::toggleModelSelectorPopup,
            onSelectModelLabel = replicaViewModel::selectModelLabel,
            onModelSelectorVisibilityChange = replicaViewModel::setModelSelectorPopupVisible,
            onToggleFeaturePanel = replicaViewModel::toggleFeaturePanel,
            onFeaturePanelVisibilityChange = replicaViewModel::setFeaturePanelVisible,
            onToggleAttachmentPanel = replicaViewModel::toggleAttachmentPanel,
            onAttachmentPanelVisibilityChange = replicaViewModel::setAttachmentPanelVisible,
            onAddAttachment = replicaViewModel::addAttachment,
            onOpenFullscreenEditor = { replicaViewModel.setFullscreenEditorVisible(true) },
            onSubmitPrompt = ::submitPrompt,
            onEnqueuePendingPrompt = replicaViewModel::enqueuePendingPrompt,
            onPendingQueueExpandedChange = replicaViewModel::setPendingQueueExpanded,
            onDeletePendingQueueMessage = replicaViewModel::deletePendingQueueMessage,
            onEditPendingQueueMessage = replicaViewModel::editPendingQueueMessage,
            onSendPendingQueueMessage = replicaViewModel::sendPendingQueueMessage,
            onCancelProcessing = replicaViewModel::cancelInputProcessing,
            onManageMemory = placeholderClick,
            onManualMemoryUpdate = placeholderClick,
            onManageModels = placeholderClick,
            onToggleMemoryAutoUpdate = {
                replicaViewModel.setMemoryAutoUpdateEnabled(!enableMemoryAutoUpdate)
            },
            onToggleThinkingMode = {
                replicaViewModel.setThinkingEnabled(!enableThinking)
            },
            onThinkingQualityLevelChange = replicaViewModel::setThinkingQualityLevel,
            onToggleMaxContextMode = {
                replicaViewModel.setMaxContextModeEnabled(!enableMaxContextMode)
            },
            onToggleAutoRead = {
                replicaViewModel.setAutoReadEnabled(!isAutoReadEnabled)
            },
            onToggleAutoApprove = {
                replicaViewModel.setAutoApproveEnabled(!isAutoApproveEnabled)
            },
            onToggleTools = {
                replicaViewModel.setToolsEnabled(!enableTools)
            },
            onToggleDisableStreamOutput = {
                replicaViewModel.setDisableStreamOutput(!disableStreamOutput)
            },
            onToggleDisableUserPreferenceDescription = {
                replicaViewModel.setDisableUserPreferenceDescription(!disableUserPreferenceDescription)
            },
            onToggleDisableStatusTags = {
                replicaViewModel.setDisableStatusTags(!disableStatusTags)
            },
            onManageTools = placeholderClick,
            onAuxiliaryActionClick = onAuxiliaryActionClick,
        )

        KiyoriOperitReplicaOverlayHost(
            bottomDockPadding = bottomDockPadding,
            showHistoryPanel = showHistoryPanel,
            historySearchQuery = historySearchQuery,
            showHistorySearchBox = showHistorySearchBox,
            activeConversation = activeConversation,
            groupedConversations = groupedConversations,
            showFeaturePanel = showFeaturePanel,
            showSwipeHint = showSwipeHint,
            showFullscreenEditor = showFullscreenEditor,
            showHistorySettingsDialog = showHistorySettingsDialog,
            historyDisplayMode = historyDisplayMode,
            autoSwitchCharacterCard = autoSwitchCharacterCard,
            autoSwitchChatOnCharacterSelect = autoSwitchChatOnCharacterSelect,
            showCharacterSelector = showCharacterSelector,
            selectedCharacterId = selectedCharacterId,
            characterSortOption = characterSortOption,
            characterOptions = characterOptions,
            showWorkspacePanel = showWorkspacePanel,
            hasEverOpenedWorkspace = hasEverOpenedWorkspace,
            isWorkspacePreparing = isWorkspacePreparing,
            workspaceReloadVersion = workspaceReloadVersion,
            showComputerPanel = showComputerPanel,
            currentModelLabel = currentModelLabel,
            enableThinking = enableThinking,
            enableTools = enableTools,
            enableMemory = enableMemory,
            enableStream = enableStream,
            enableVoice = enableVoice,
            enableWorkspace = enableWorkspace,
            enableNotification = enableNotification,
            inputText = inputText,
            activeMessages = activeMessages,
            onHistoryPanelDismiss = { replicaViewModel.setHistoryPanelVisible(false) },
            onToggleHistorySearchBox = replicaViewModel::toggleHistorySearchBox,
            onHistoryQueryChange = replicaViewModel::updateHistorySearchQuery,
            onCreateConversation = { createNewConversation() },
            onCreateGroup = replicaViewModel::createConversationGroup,
            onSelectConversation = replicaViewModel::selectConversation,
            onRenameConversation = replicaViewModel::renameConversation,
            onDeleteConversation = replicaViewModel::deleteConversation,
            onMoveConversationUp = replicaViewModel::moveConversationUp,
            onMoveConversationDown = replicaViewModel::moveConversationDown,
            onRenameGroup = replicaViewModel::renameConversationGroup,
            onClearGroup = replicaViewModel::clearConversationGroup,
            onDeleteGroup = replicaViewModel::deleteConversationGroup,
            onOpenHistorySettings = { replicaViewModel.setHistorySettingsDialogVisible(true) },
            onThinkingChange = replicaViewModel::setThinkingEnabled,
            onToolsChange = replicaViewModel::setToolsEnabled,
            onMemoryChange = replicaViewModel::setMemoryEnabled,
            onStreamChange = replicaViewModel::setStreamEnabled,
            onVoiceChange = replicaViewModel::setVoiceEnabled,
            onWorkspaceChange = replicaViewModel::setWorkspaceEnabled,
            onNotificationChange = replicaViewModel::setNotificationEnabled,
            onFeaturePanelDismiss = { replicaViewModel.setFeaturePanelVisible(false) },
            onDismissSwipeHint = replicaViewModel::dismissSwipeHint,
            onFullscreenInputChange = replicaViewModel::updateInputText,
            onDismissFullscreenEditor = { replicaViewModel.setFullscreenEditorVisible(false) },
            onSubmitFullscreenEditor = {
                replicaViewModel.setFullscreenEditorVisible(false)
                submitPrompt()
            },
            onDismissHistorySettings = { replicaViewModel.setHistorySettingsDialogVisible(false) },
            onHistoryDisplayModeChange = replicaViewModel::setHistoryDisplayMode,
            onAutoSwitchCharacterCardChange = replicaViewModel::setAutoSwitchCharacterCard,
            onAutoSwitchChatOnCharacterSelectChange = replicaViewModel::setAutoSwitchChatOnCharacterSelect,
            onDismissCharacterSelector = { replicaViewModel.setCharacterSelectorVisible(false) },
            onSelectCharacter = replicaViewModel::selectCharacter,
            onCharacterSortOptionChange = replicaViewModel::setCharacterSortOption,
            onOpenCharacterSettings = placeholderClick,
            onCloseWorkspace = replicaViewModel::closeWorkspacePanel,
            onReloadWorkspace = replicaViewModel::reloadWorkspacePanel,
            onOpenComputer = { replicaViewModel.setComputerPanelVisible(true) },
            onCloseComputer = { replicaViewModel.setComputerPanelVisible(false) },
            onModelClick = placeholderClick,
        )
    }
}
