package com.ai.assistance.operit.ui.features.chat.components.part

import com.ai.assistance.operit.ui.features.chat.components.ToolCollapseModeBridge

internal sealed interface ChatMarkupGroupedItemBridge {
    data class Single(val block: ChatMarkupBlock) : ChatMarkupGroupedItemBridge

    data class Group(
        val blocks: List<ChatMarkupBlock.XmlBlock>,
        val isToolsOnly: Boolean,
        val toolCount: Int,
    ) : ChatMarkupGroupedItemBridge
}

internal fun groupThinkToolsXmlBlocksBridge(
    blocks: List<ChatMarkupBlock>,
    showThinkingProcess: Boolean,
    toolCollapseMode: ToolCollapseModeBridge,
): List<ChatMarkupGroupedItemBridge> {
    val out = mutableListOf<ChatMarkupGroupedItemBridge>()
    var index = 0
    while (index < blocks.size) {
        val block = blocks[index]
        if (block !is ChatMarkupBlock.XmlBlock) {
            out += ChatMarkupGroupedItemBridge.Single(block)
            index++
            continue
        }

        when {
            showThinkingProcess && block.tagName in thinkingTagsBridge -> {
                val group = resolveThinkToolGroupBridge(blocks, index, toolCollapseMode)
                if (group != null) {
                    out += group.item
                    index = group.nextIndex
                } else {
                    out += ChatMarkupGroupedItemBridge.Single(block)
                    index++
                }
            }

            block.tagName in toolRelatedTagsBridge -> {
                val group = resolveToolsOnlyGroupBridge(blocks, index, toolCollapseMode)
                if (group != null) {
                    out += group.item
                    index = group.nextIndex
                } else {
                    out += ChatMarkupGroupedItemBridge.Single(block)
                    index++
                }
            }

            else -> {
                out += ChatMarkupGroupedItemBridge.Single(block)
                index++
            }
        }
    }
    return out
}

private val thinkingTagsBridge = setOf("think", "thinking")
private val toolRelatedTagsBridge = setOf("tool", "tool_result")

private data class ResolvedThinkToolsGroupBridge(
    val item: ChatMarkupGroupedItemBridge.Group,
    val nextIndex: Int,
)

private fun resolveThinkToolGroupBridge(
    blocks: List<ChatMarkupBlock>,
    startIndex: Int,
    toolCollapseMode: ToolCollapseModeBridge,
): ResolvedThinkToolsGroupBridge? {
    var index = startIndex + 1
    var toolCount = 0
    var xmlToolRelatedCount = 0
    while (index < blocks.size) {
        val block = blocks[index]
        if (block is ChatMarkupBlock.TextBlock && block.text.isBlank()) {
            index++
            continue
        }
        if (block !is ChatMarkupBlock.XmlBlock) {
            break
        }
        if (isIgnorableXmlTagForToolGroupingBridge(block.tagName)) {
            index++
            continue
        }

        val isThinkAgain = block.tagName in thinkingTagsBridge
        val isToolRelated = block.tagName in toolRelatedTagsBridge
        if (!isThinkAgain && !isToolRelated) {
            break
        }

        if (isToolRelated) {
            val toolName = block.resolveToolNameBridge()
            if (!shouldGroupToolByNameBridge(toolName, toolCollapseMode)) {
                break
            }
            if (block.tagName == "tool") {
                toolCount++
            }
            xmlToolRelatedCount++
        }
        index++
    }

    if (!shouldCollapseToolSequenceBridge(toolCollapseMode, toolCount, xmlToolRelatedCount)) {
        return null
    }
    return ResolvedThinkToolsGroupBridge(
        item =
            ChatMarkupGroupedItemBridge.Group(
                blocks = blocks.subList(startIndex, index).filterIsInstance<ChatMarkupBlock.XmlBlock>(),
                isToolsOnly = false,
                toolCount = toolCount,
            ),
        nextIndex = index,
    )
}

private fun resolveToolsOnlyGroupBridge(
    blocks: List<ChatMarkupBlock>,
    startIndex: Int,
    toolCollapseMode: ToolCollapseModeBridge,
): ResolvedThinkToolsGroupBridge? {
    val firstBlock = blocks[startIndex] as? ChatMarkupBlock.XmlBlock ?: return null
    val firstToolName = firstBlock.resolveToolNameBridge()
    if (!shouldGroupToolByNameBridge(firstToolName, toolCollapseMode)) {
        return null
    }

    var index = startIndex + 1
    var toolCount = if (firstBlock.tagName == "tool") 1 else 0
    var xmlToolRelatedCount = 1
    while (index < blocks.size) {
        val block = blocks[index]
        if (block is ChatMarkupBlock.TextBlock && block.text.isBlank()) {
            index++
            continue
        }
        if (block !is ChatMarkupBlock.XmlBlock) {
            break
        }
        if (isIgnorableXmlTagForToolGroupingBridge(block.tagName)) {
            index++
            continue
        }
        if (block.tagName !in toolRelatedTagsBridge) {
            break
        }
        val toolName = block.resolveToolNameBridge()
        if (!shouldGroupToolByNameBridge(toolName, toolCollapseMode)) {
            break
        }
        if (block.tagName == "tool") {
            toolCount++
        }
        xmlToolRelatedCount++
        index++
    }

    if (!shouldCollapseToolSequenceBridge(toolCollapseMode, toolCount, xmlToolRelatedCount)) {
        return null
    }
    return ResolvedThinkToolsGroupBridge(
        item =
            ChatMarkupGroupedItemBridge.Group(
                blocks = blocks.subList(startIndex, index).filterIsInstance<ChatMarkupBlock.XmlBlock>(),
                isToolsOnly = true,
                toolCount = toolCount,
            ),
        nextIndex = index,
    )
}

private fun isIgnorableXmlTagForToolGroupingBridge(tagName: String): Boolean = tagName == "meta"

private fun shouldGroupToolByNameBridge(
    toolName: String,
    toolCollapseMode: ToolCollapseModeBridge,
): Boolean {
    if (toolCollapseMode == ToolCollapseModeBridge.ALL || toolCollapseMode == ToolCollapseModeBridge.FULL) {
        return true
    }
    val normalizedName = toolName.trim().lowercase()
    if (normalizedName.isBlank()) {
        return false
    }
    if (normalizedName.contains("search")) {
        return true
    }
    return normalizedName in
        setOf(
            "list_files",
            "grep_code",
            "grep_context",
            "read_file",
            "read_file_part",
            "read_file_full",
            "read_file_binary",
            "use_package",
            "find_files",
            "visit_web",
        )
}

private fun shouldCollapseToolSequenceBridge(
    toolCollapseMode: ToolCollapseModeBridge,
    toolCount: Int,
    xmlToolRelatedCount: Int,
): Boolean {
    if (xmlToolRelatedCount <= 0) {
        return false
    }
    return when (toolCollapseMode) {
        ToolCollapseModeBridge.FULL -> true
        ToolCollapseModeBridge.READ_ONLY,
        ToolCollapseModeBridge.ALL -> toolCount >= 2 && xmlToolRelatedCount >= 2
    }
}

internal fun ChatMarkupBlock.XmlBlock.resolveToolNameBridge(): String =
    attributes["name"]
        ?: attributes["tool"]
        ?: attributes["type"]
        ?: attributes["toolName"]
        ?: ""
