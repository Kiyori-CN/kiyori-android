package com.ai.assistance.operit.ui.features.chat.components

const val MAX_VISIBLE_CHAT_PAGES = 2

data class ChatAreaPaginationState(
    val pageStartIndices: List<Int>,
)

data class ChatAreaPaginationWindow(
    val newestVisibleDepth: Int,
    val oldestVisibleDepth: Int,
    val minVisibleIndex: Int,
    val maxVisibleIndexExclusive: Int,
    val hasOlderPages: Boolean,
    val hasNewerPages: Boolean,
)

fun buildChatAreaPaginationState(
    itemCount: Int,
    messagesPerPage: Int,
    isPaginationTrigger: (Int) -> Boolean,
): ChatAreaPaginationState {
    if (itemCount <= 0) {
        return ChatAreaPaginationState(pageStartIndices = emptyList())
    }

    val safeMessagesPerPage = messagesPerPage.coerceAtLeast(1)
    val pageStartIndices = mutableListOf<Int>()
    var cursor = itemCount - 1

    while (cursor >= 0) {
        var pageStartIndex = 0
        var triggerCountInCurrentPage = 0
        var pageClosed = false

        while (cursor >= 0 && !pageClosed) {
            if (isPaginationTrigger(cursor)) {
                triggerCountInCurrentPage += 1
                if (triggerCountInCurrentPage >= safeMessagesPerPage) {
                    pageStartIndex = cursor
                    cursor -= 1
                    pageClosed = true
                }
            }

            if (!pageClosed) {
                cursor -= 1
            }
        }

        if (!pageClosed) {
            pageStartIndex = 0
            cursor = -1
        }

        pageStartIndices += pageStartIndex
    }

    return ChatAreaPaginationState(pageStartIndices = pageStartIndices)
}

fun resolveChatAreaPaginationWindow(
    paginationState: ChatAreaPaginationState,
    itemCount: Int,
    newestVisibleDepth: Int,
    oldestVisibleDepth: Int,
): ChatAreaPaginationWindow {
    if (itemCount <= 0 || paginationState.pageStartIndices.isEmpty()) {
        return ChatAreaPaginationWindow(
            newestVisibleDepth = 1,
            oldestVisibleDepth = 1,
            minVisibleIndex = 0,
            maxVisibleIndexExclusive = itemCount,
            hasOlderPages = false,
            hasNewerPages = false,
        )
    }

    val pageCount = paginationState.pageStartIndices.size
    val safeNewestVisibleDepth = newestVisibleDepth.coerceIn(1, pageCount)
    val safeOldestVisibleDepth =
        oldestVisibleDepth
            .coerceIn(safeNewestVisibleDepth, pageCount)
            .coerceAtMost(safeNewestVisibleDepth + MAX_VISIBLE_CHAT_PAGES - 1)
    val minVisibleIndex = paginationState.pageStartIndices[safeOldestVisibleDepth - 1]
    val maxVisibleIndexExclusive =
        if (safeNewestVisibleDepth == 1) {
            itemCount
        } else {
            paginationState.pageStartIndices[safeNewestVisibleDepth - 2]
        }

    return ChatAreaPaginationWindow(
        newestVisibleDepth = safeNewestVisibleDepth,
        oldestVisibleDepth = safeOldestVisibleDepth,
        minVisibleIndex = minVisibleIndex,
        maxVisibleIndexExclusive = maxVisibleIndexExclusive,
        hasOlderPages = safeOldestVisibleDepth < pageCount,
        hasNewerPages = safeNewestVisibleDepth > 1,
    )
}

fun findChatAreaPaginationDepthForIndex(
    paginationState: ChatAreaPaginationState,
    targetIndex: Int,
): Int {
    if (paginationState.pageStartIndices.isEmpty()) {
        return 1
    }

    val safeTargetIndex = targetIndex.coerceAtLeast(0)
    paginationState.pageStartIndices.forEachIndexed { index, pageStartIndex ->
        if (safeTargetIndex >= pageStartIndex) {
            return index + 1
        }
    }

    return paginationState.pageStartIndices.size
}

fun resolveChatAreaWindowDepthsForTargetPage(
    paginationState: ChatAreaPaginationState,
    targetPageDepth: Int,
): Pair<Int, Int> {
    if (paginationState.pageStartIndices.isEmpty()) {
        return 1 to 1
    }

    val pageCount = paginationState.pageStartIndices.size
    val safeTargetPageDepth = targetPageDepth.coerceIn(1, pageCount)
    val newestVisibleDepth = (safeTargetPageDepth - MAX_VISIBLE_CHAT_PAGES + 1).coerceAtLeast(1)
    val oldestVisibleDepth =
        (newestVisibleDepth + MAX_VISIBLE_CHAT_PAGES - 1)
            .coerceAtMost(pageCount)
            .coerceAtLeast(safeTargetPageDepth)

    return newestVisibleDepth to oldestVisibleDepth
}
