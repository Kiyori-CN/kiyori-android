package com.ai.assistance.operit.ui.features.chat.components

data class ChatAreaWindowDepths(
    val newestVisibleDepth: Int,
    val oldestVisibleDepth: Int,
)

fun resolveChatAreaLoadOlderWindowDepths(
    paginationWindow: ChatAreaPaginationWindow,
    visiblePageCount: Int,
): ChatAreaWindowDepths {
    return if (visiblePageCount < MAX_VISIBLE_CHAT_PAGES) {
        ChatAreaWindowDepths(
            newestVisibleDepth = paginationWindow.newestVisibleDepth,
            oldestVisibleDepth = paginationWindow.oldestVisibleDepth + 1,
        )
    } else {
        ChatAreaWindowDepths(
            newestVisibleDepth = paginationWindow.newestVisibleDepth + 1,
            oldestVisibleDepth = paginationWindow.oldestVisibleDepth + 1,
        )
    }
}

fun resolveChatAreaLoadNewerWindowDepths(
    paginationWindow: ChatAreaPaginationWindow,
): ChatAreaWindowDepths {
    val nextNewestVisibleDepth = (paginationWindow.newestVisibleDepth - 1).coerceAtLeast(1)
    val nextOldestVisibleDepth =
        (paginationWindow.oldestVisibleDepth - 1)
            .coerceAtLeast(nextNewestVisibleDepth)
    return ChatAreaWindowDepths(
        newestVisibleDepth = nextNewestVisibleDepth,
        oldestVisibleDepth = nextOldestVisibleDepth,
    )
}

fun resolveChatAreaPendingJumpWindowDepths(
    paginationState: ChatAreaPaginationState,
    paginationWindow: ChatAreaPaginationWindow,
    targetIndex: Int,
    visibleRange: IntRange,
): ChatAreaWindowDepths? {
    if (targetIndex in visibleRange) {
        return null
    }

    val targetPageDepth = findChatAreaPaginationDepthForIndex(paginationState, targetIndex)
    val (newestDepth, oldestDepth) =
        resolveChatAreaWindowDepthsForTargetPage(
            paginationState = paginationState,
            targetPageDepth = targetPageDepth,
        )

    if (
        newestDepth == paginationWindow.newestVisibleDepth &&
            oldestDepth == paginationWindow.oldestVisibleDepth
    ) {
        return null
    }

    return ChatAreaWindowDepths(
        newestVisibleDepth = newestDepth,
        oldestVisibleDepth = oldestDepth,
    )
}

fun pruneChatAreaSelectedMessageIndices(
    selectedMessageIndices: Set<Int>,
    itemCount: Int,
): Set<Int> = selectedMessageIndices.filter { it in 0 until itemCount }.toSet()

fun resolveChatAreaMultiSelectToggle(
    isMultiSelectMode: Boolean,
    initialIndex: Int?,
): Pair<Boolean, Set<Int>> {
    val nextMultiSelectMode = !isMultiSelectMode
    val nextSelectedIndices =
        if (nextMultiSelectMode && initialIndex != null) {
            setOf(initialIndex)
        } else {
            emptySet()
        }
    return nextMultiSelectMode to nextSelectedIndices
}

fun toggleChatAreaMessageSelection(
    selectedMessageIndices: Set<Int>,
    index: Int,
): Set<Int> {
    return if (selectedMessageIndices.contains(index)) {
        selectedMessageIndices - index
    } else {
        selectedMessageIndices + index
    }
}
