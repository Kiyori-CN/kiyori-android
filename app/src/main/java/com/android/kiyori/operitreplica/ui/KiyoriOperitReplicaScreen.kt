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
import com.android.kiyori.operitreplica.model.OperitReplicaCharacterSortOption
import com.android.kiyori.operitreplica.model.OperitReplicaHistoryDisplayMode
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
    val selectedCharacterId = replicaUiState.selectedCharacterId
    val historySearchQuery = replicaUiState.historySearchQuery
    val inputText = replicaUiState.inputText
    val showHistoryPanel = replicaUiState.showHistoryPanel
    val showStatsMenu = replicaUiState.showStatsMenu
    val showFeaturePanel = replicaUiState.showFeaturePanel
    val showAttachmentPanel = replicaUiState.showAttachmentPanel
    val showFullscreenEditor = replicaUiState.showFullscreenEditor
    val showCharacterSelector = replicaUiState.showCharacterSelector
    val showWorkspacePanel = replicaUiState.showWorkspacePanel
    val hasEverOpenedWorkspace = replicaUiState.hasEverOpenedWorkspace
    val isWorkspacePreparing = replicaUiState.isWorkspacePreparing
    val workspaceReloadVersion = replicaUiState.workspaceReloadVersion
    val showComputerPanel = replicaUiState.showComputerPanel
    val showHistorySearchBox = replicaUiState.showHistorySearchBox
    val showHistorySettingsDialog = replicaUiState.showHistorySettingsDialog
    val showSwipeHint = replicaUiState.showSwipeHint
    val showReplyPreview = replicaUiState.showReplyPreview
    val historyDisplayMode = replicaUiState.historyDisplayMode
    val autoSwitchCharacterCard = replicaUiState.autoSwitchCharacterCard
    val autoSwitchChatOnCharacterSelect = replicaUiState.autoSwitchChatOnCharacterSelect
    val latestUserMessageText = replicaUiState.latestUserMessageText
    val statusStripText = replicaUiState.statusStripText
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
    val characterSortOption = replicaUiState.characterSortOption
    val attachments = replicaUiState.attachments
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
    val workspaceAction = onExpandChatClick ?: { replicaViewModel.openWorkspacePanel() }

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
                activeMessages = activeMessages,
                onHistoryClick = { replicaViewModel.setHistoryPanelVisible(true) },
                onPipClick = { replicaViewModel.setComputerPanelVisible(true) },
                onCharacterClick = { replicaViewModel.setCharacterSelectorVisible(true) },
                onStatsMenuDismiss = { replicaViewModel.setStatsMenuVisible(false) },
                onStatsMenuToggle = replicaViewModel::toggleStatsMenu,
            )
        }

        KiyoriOperitReplicaInputBar(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 12.dp + bottomDockPadding)
                    .navigationBarsPadding(),
            inputText = inputText,
            latestUserMessage = latestUserMessageText,
            showReplyPreview = showReplyPreview,
            statusStripText = statusStripText,
            attachments = attachments,
            showFeaturePanel = showFeaturePanel,
            showAttachmentPanel = showAttachmentPanel,
            onInputTextChange = replicaViewModel::updateInputText,
            onDismissReply = { replicaViewModel.setReplyPreviewVisible(false) },
            onRemoveAttachment = replicaViewModel::removeAttachment,
            onToggleFeaturePanel = replicaViewModel::toggleFeaturePanel,
            onToggleAttachmentPanel = replicaViewModel::toggleAttachmentPanel,
            onOpenFullscreenEditor = { replicaViewModel.setFullscreenEditorVisible(true) },
            onSubmitPrompt = ::submitPrompt,
            onAuxiliaryActionClick = onAuxiliaryActionClick,
            onExpandChatClick = workspaceAction,
        )

        KiyoriOperitReplicaOverlayHost(
            bottomDockPadding = bottomDockPadding,
            showHistoryPanel = showHistoryPanel,
            historySearchQuery = historySearchQuery,
            showHistorySearchBox = showHistorySearchBox,
            activeConversation = activeConversation,
            groupedConversations = groupedConversations,
            showFeaturePanel = showFeaturePanel,
            showAttachmentPanel = showAttachmentPanel,
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
            onAttachmentPanelDismiss = { replicaViewModel.setAttachmentPanelVisible(false) },
            onAddAttachment = replicaViewModel::addAttachment,
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
