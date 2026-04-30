package com.android.kiyori.operitreplica.model

import androidx.compose.ui.graphics.Color

internal data class OperitReplicaConversation(
    val id: String,
    val title: String,
    val preview: String,
    val sortKey: Long = System.currentTimeMillis(),
    val groupName: String? = null,
    val characterGroupName: String? = null,
    val characterId: String = "operit-agent",
    val accent: Color,
)

internal enum class OperitReplicaConversationSectionType {
    CHARACTER,
    GROUP,
}

internal enum class OperitReplicaConversationBindingKind {
    CHARACTER_CARD,
    CHARACTER_GROUP,
    UNBOUND,
    GROUP_ONLY,
}

internal data class OperitReplicaConversationSection(
    val key: String,
    val title: String,
    val type: OperitReplicaConversationSectionType,
    val subtitle: String? = null,
    val bindingKind: OperitReplicaConversationBindingKind = OperitReplicaConversationBindingKind.GROUP_ONLY,
    val accent: Color = Color(0xFF6177B2),
    val characterId: String? = null,
    val groupName: String? = null,
    val childSections: List<OperitReplicaConversationSection> = emptyList(),
    val conversations: List<OperitReplicaConversation> = emptyList(),
)

internal data class OperitReplicaConversationGroup(
    val name: String,
    val characterId: String? = null,
    val characterGroupName: String? = null,
    val sortKey: Long = System.currentTimeMillis(),
)

internal data class OperitReplicaCharacterOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val sortKey: Long = System.currentTimeMillis(),
    val accent: Color,
)

internal enum class OperitReplicaCharacterSortOption {
    DEFAULT,
    NAME_ASC,
    CREATED_DESC,
}

internal enum class OperitReplicaHistoryDisplayMode {
    BY_CHARACTER_CARD,
    BY_FOLDER,
    CURRENT_CHARACTER_ONLY,
}

internal data class OperitReplicaMessage(
    val role: OperitReplicaMessageRole,
    val text: String,
    val meta: String? = null,
    val timestamp: Long = 0L,
    val roleName: String = "",
    val provider: String = "",
    val modelName: String = "",
    val displayMode: OperitReplicaMessageDisplayMode = OperitReplicaMessageDisplayMode.NORMAL,
    val selectedVariantIndex: Int = 0,
    val variantCount: Int = 1,
    val variantTexts: List<String> = emptyList(),
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val cachedInputTokens: Int = 0,
    val waitDurationMs: Long = 0L,
    val outputDurationMs: Long = 0L,
)

internal enum class OperitReplicaMessageDisplayMode {
    NORMAL,
    HIDDEN_PLACEHOLDER,
}

internal enum class OperitReplicaMessageRole {
    Assistant,
    System,
    User,
}
