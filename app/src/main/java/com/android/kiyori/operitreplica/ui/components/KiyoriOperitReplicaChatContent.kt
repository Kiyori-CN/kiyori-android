package com.android.kiyori.operitreplica.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.ai.assistance.operit.ui.features.chat.components.ChatAreaDisplayPreferencesBridge
import com.ai.assistance.operit.ui.features.chat.components.ChatAreaWorkbenchBridge
import com.ai.assistance.operit.ui.features.chat.components.ChatScreenContentWorkbenchBridge
import com.android.kiyori.operitreplica.bridge.toChatActionSurfaceMessage
import com.android.kiyori.operitreplica.model.OperitReplicaMessage

@Composable
internal fun KiyoriOperitReplicaChatContent(
    modifier: Modifier = Modifier,
    activeConversationId: String,
    bottomDockPadding: Dp,
    immersiveMode: Boolean,
    showHistoryPanel: Boolean,
    chatHeaderTransparent: Boolean,
    chatHeaderOverlayMode: Boolean,
    chatHeaderHistoryIconColor: Int?,
    chatHeaderPipIconColor: Int?,
    currentCharacterLabel: String,
    currentWindowSize: Int,
    maxContextLengthInK: Float,
    inputTokenCount: Int,
    outputTokenCount: Int,
    showStatsMenu: Boolean,
    showModelProvider: Boolean,
    showModelName: Boolean,
    showRoleName: Boolean,
    showMessageTokenStats: Boolean,
    showMessageTimingStats: Boolean,
    showThinkingProcess: Boolean,
    showStatusTags: Boolean,
    isInputProcessing: Boolean,
    activeMessages: List<OperitReplicaMessage>,
    onHistoryClick: () -> Unit,
    onPipClick: () -> Unit,
    onCharacterClick: () -> Unit,
    onStatsMenuDismiss: () -> Unit,
    onStatsMenuToggle: () -> Unit,
    onUpdateMessage: (Int, String) -> Unit,
    onRewindAndResendMessage: (Int, String) -> Unit,
    onDeleteMessage: (Int) -> Unit,
    onDeleteMessages: (Set<Int>) -> Unit,
    onRollbackToMessage: (Int) -> Unit,
    onRegenerateMessage: (Int) -> Unit,
    onSwitchMessageVariant: (Int, Int) -> Unit,
    onDeleteCurrentMessageVariant: (Int) -> Unit,
    onInsertSummary: (Int) -> Unit,
    onCreateBranch: (Int) -> Unit,
) {
    val actionSurfaceMessages =
        remember(activeMessages) {
            activeMessages.map { it.toChatActionSurfaceMessage() }
        }
    val displayPreferences =
        remember(
            showModelProvider,
            showModelName,
            showRoleName,
            showMessageTokenStats,
            showMessageTimingStats,
            showThinkingProcess,
            showStatusTags,
        ) {
            ChatAreaDisplayPreferencesBridge(
                showModelProvider = showModelProvider,
                showModelName = showModelName,
                showRoleName = showRoleName,
                showMessageTokenStats = showMessageTokenStats,
                showMessageTimingStats = showMessageTimingStats,
                showThinkingProcess = showThinkingProcess,
                showStatusTags = showStatusTags,
            )
        }

    ChatScreenContentWorkbenchBridge(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.White),
        backgroundColor = Color.White,
        chatHeaderOverlayMode = chatHeaderOverlayMode,
        chatHeaderTransparent = chatHeaderTransparent,
        headerContent = {
            KiyoriOperitReplicaHeader(
                showHistoryPanel = showHistoryPanel,
                immersiveMode = immersiveMode,
                isInputProcessing = isInputProcessing,
                chatHeaderTransparent = chatHeaderTransparent,
                chatHeaderHistoryIconColor = chatHeaderHistoryIconColor,
                chatHeaderPipIconColor = chatHeaderPipIconColor,
                currentCharacterLabel = currentCharacterLabel,
                currentWindowSize = currentWindowSize,
                maxContextLengthInK = maxContextLengthInK,
                inputTokenCount = inputTokenCount,
                outputTokenCount = outputTokenCount,
                showStatsMenu = showStatsMenu,
                onHistoryClick = onHistoryClick,
                onPipClick = onPipClick,
                onCharacterClick = onCharacterClick,
                onStatsMenuDismiss = onStatsMenuDismiss,
                onStatsMenuToggle = onStatsMenuToggle,
            )
        },
    ) { topPadding, bottomPadding ->
        ChatAreaWorkbenchBridge(
            activeConversationId = activeConversationId,
            messages = actionSurfaceMessages,
            isInputProcessing = isInputProcessing,
            topPadding = topPadding,
            bottomPadding = bottomPadding,
            bottomDockPadding = bottomDockPadding,
            displayPreferences = displayPreferences,
            onUpdateMessage = onUpdateMessage,
            onRewindAndResendMessage = onRewindAndResendMessage,
            onDeleteMessage = onDeleteMessage,
            onDeleteMessages = onDeleteMessages,
            onRollbackToMessage = onRollbackToMessage,
            onRegenerateMessage = onRegenerateMessage,
            onSwitchMessageVariant = onSwitchMessageVariant,
            onDeleteCurrentMessageVariant = onDeleteCurrentMessageVariant,
            onInsertSummary = { index, _ -> onInsertSummary(index) },
            onCreateBranch = onCreateBranch,
        )
    }
}
