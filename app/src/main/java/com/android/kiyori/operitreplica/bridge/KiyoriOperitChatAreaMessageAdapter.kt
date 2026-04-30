package com.android.kiyori.operitreplica.bridge

import com.ai.assistance.operit.ui.features.chat.components.ChatActionSurfaceDisplayMode
import com.ai.assistance.operit.ui.features.chat.components.ChatActionSurfaceMessage
import com.ai.assistance.operit.ui.features.chat.components.ChatActionSurfaceSender
import com.android.kiyori.operitreplica.model.OperitReplicaMessage
import com.android.kiyori.operitreplica.model.OperitReplicaMessageDisplayMode
import com.android.kiyori.operitreplica.model.OperitReplicaMessageRole

internal fun OperitReplicaMessage.toChatActionSurfaceMessage(): ChatActionSurfaceMessage =
    ChatActionSurfaceMessage(
        sender =
            when (role) {
                OperitReplicaMessageRole.User -> ChatActionSurfaceSender.USER
                OperitReplicaMessageRole.Assistant -> ChatActionSurfaceSender.AI
                OperitReplicaMessageRole.System -> ChatActionSurfaceSender.SYSTEM
            },
        content = text,
        meta = meta,
        roleName = roleName,
        modelName = modelName,
        provider = provider,
        timestamp = timestamp,
        displayMode =
            when (displayMode) {
                OperitReplicaMessageDisplayMode.NORMAL -> ChatActionSurfaceDisplayMode.NORMAL
                OperitReplicaMessageDisplayMode.HIDDEN_PLACEHOLDER ->
                    ChatActionSurfaceDisplayMode.HIDDEN_PLACEHOLDER
            },
        selectedVariantIndex = selectedVariantIndex,
        variantCount = variantCount,
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        cachedInputTokens = cachedInputTokens,
        waitDurationMs = waitDurationMs,
        outputDurationMs = outputDurationMs,
    )
