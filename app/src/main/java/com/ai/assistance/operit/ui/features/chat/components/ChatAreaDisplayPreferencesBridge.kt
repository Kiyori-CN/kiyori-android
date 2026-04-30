package com.ai.assistance.operit.ui.features.chat.components

data class ChatAreaDisplayPreferencesBridge(
    val showModelProvider: Boolean = false,
    val showModelName: Boolean = false,
    val showRoleName: Boolean = false,
    val showMessageTokenStats: Boolean = false,
    val showMessageTimingStats: Boolean = false,
    val showThinkingProcess: Boolean = true,
    val showStatusTags: Boolean = true,
    val toolCollapseMode: ToolCollapseModeBridge = ToolCollapseModeBridge.ALL,
)

enum class ToolCollapseModeBridge {
    READ_ONLY,
    ALL,
    FULL,
}
