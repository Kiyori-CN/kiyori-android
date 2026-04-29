package com.android.kiyori.operitreplica.state

import com.android.kiyori.operitreplica.model.OperitReplicaCharacterOption
import com.android.kiyori.operitreplica.model.OperitReplicaCharacterSortOption
import com.android.kiyori.operitreplica.model.OperitReplicaConversation
import com.android.kiyori.operitreplica.model.OperitReplicaConversationGroup
import com.android.kiyori.operitreplica.model.OperitReplicaConversationSection
import com.android.kiyori.operitreplica.model.OperitReplicaHistoryDisplayMode
import com.android.kiyori.operitreplica.model.OperitReplicaMessage

internal data class KiyoriOperitReplicaUiState(
    val currentModelLabel: String = "DeepSeek / Agent",
    val currentCharacterLabel: String = "Operit",
    val activeConversationId: String = "chat-main",
    val selectedCharacterId: String = "operit-agent",
    val historySearchQuery: String = "",
    val inputText: String = "",
    val latestUserMessageText: String? = null,
    val statusStripText: String = "",
    val inputTokenCount: Int = 12,
    val outputTokenCount: Int = 32,
    val currentWindowSize: Int = 1720,
    val contextUsagePercentage: Float = 0f,
    val showHistoryPanel: Boolean = false,
    val showStatsMenu: Boolean = false,
    val showFeaturePanel: Boolean = false,
    val showAttachmentPanel: Boolean = false,
    val showFullscreenEditor: Boolean = false,
    val showCharacterSelector: Boolean = false,
    val showWorkspacePanel: Boolean = false,
    val hasEverOpenedWorkspace: Boolean = false,
    val isWorkspacePreparing: Boolean = false,
    val workspaceReloadVersion: Int = 0,
    val showComputerPanel: Boolean = false,
    val showHistorySearchBox: Boolean = false,
    val showHistorySettingsDialog: Boolean = false,
    val showSwipeHint: Boolean = true,
    val showReplyPreview: Boolean = true,
    val historyDisplayMode: OperitReplicaHistoryDisplayMode = OperitReplicaHistoryDisplayMode.BY_FOLDER,
    val autoSwitchCharacterCard: Boolean = true,
    val autoSwitchChatOnCharacterSelect: Boolean = false,
    val enableThinking: Boolean = true,
    val enableTools: Boolean = true,
    val enableMemory: Boolean = true,
    val enableStream: Boolean = true,
    val enableVoice: Boolean = false,
    val enableWorkspace: Boolean = true,
    val enableNotification: Boolean = false,
    val characterSortOption: OperitReplicaCharacterSortOption = OperitReplicaCharacterSortOption.DEFAULT,
    val attachments: List<String> = listOf("屏幕内容", "工作区"),
    val characterOptions: List<OperitReplicaCharacterOption> = emptyList(),
    val conversationGroups: List<OperitReplicaConversationGroup> = emptyList(),
    val conversations: List<OperitReplicaConversation> = emptyList(),
    val conversationMessages: Map<String, List<OperitReplicaMessage>> = emptyMap(),
    val activeConversation: OperitReplicaConversation? = null,
    val activeMessages: List<OperitReplicaMessage> = emptyList(),
    val groupedConversations: List<OperitReplicaConversationSection> = emptyList(),
)
