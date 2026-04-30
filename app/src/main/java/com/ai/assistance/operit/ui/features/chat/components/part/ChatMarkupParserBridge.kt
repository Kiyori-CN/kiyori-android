package com.ai.assistance.operit.ui.features.chat.components.part

private const val TOOL_TAG_SUFFIX_REGEX_SOURCE = "[A-Za-z0-9_]+"
private const val TOOL_TAG_NAME_REGEX_SOURCE = "tool(?:_(?!result(?:_|$))$TOOL_TAG_SUFFIX_REGEX_SOURCE)?"
private const val TOOL_RESULT_TAG_NAME_REGEX_SOURCE = "tool_result(?:_${TOOL_TAG_SUFFIX_REGEX_SOURCE})?"
private const val STANDARD_TAG_NAME_REGEX_SOURCE =
    "think|thinking|search|status|workspace|workspace_attachment|attachment|font|details|detail|mood|meta"

private val pairedMarkupRegexBridge =
    Regex(
        pattern =
            "<($TOOL_RESULT_TAG_NAME_REGEX_SOURCE|$TOOL_TAG_NAME_REGEX_SOURCE|$STANDARD_TAG_NAME_REGEX_SOURCE)" +
                "(\\b[^>]*)>([\\s\\S]*?)</\\1>",
        options = setOf(RegexOption.IGNORE_CASE),
    )

private val selfClosingMarkupRegexBridge =
    Regex(
        pattern =
            "<($TOOL_RESULT_TAG_NAME_REGEX_SOURCE|$TOOL_TAG_NAME_REGEX_SOURCE|$STANDARD_TAG_NAME_REGEX_SOURCE)" +
                "(\\b[^>]*)/>",
        options = setOf(RegexOption.IGNORE_CASE),
    )

private val attributeRegexBridge =
    Regex("""([a-zA-Z0-9_-]+)\s*=\s*["']([^"']*)["']""")

private val toolTagNameRegexBridge =
    Regex("^$TOOL_TAG_NAME_REGEX_SOURCE$", RegexOption.IGNORE_CASE)

private val toolResultTagNameRegexBridge =
    Regex("^$TOOL_RESULT_TAG_NAME_REGEX_SOURCE$", RegexOption.IGNORE_CASE)

internal sealed interface ChatMarkupBlock {
    data class TextBlock(val text: String) : ChatMarkupBlock

    data class XmlBlock(
        val tagName: String,
        val attributes: Map<String, String>,
        val content: String,
        val raw: String,
    ) : ChatMarkupBlock
}

internal fun parseChatMarkupBlocksBridge(content: String): List<ChatMarkupBlock> {
    if (content.isBlank()) {
        return emptyList()
    }

    val matches =
        (pairedMarkupRegexBridge.findAll(content).map { it to false } +
            selfClosingMarkupRegexBridge.findAll(content).map { it to true })
            .sortedBy { it.first.range.first }
            .toList()

    if (matches.isEmpty()) {
        return listOf(ChatMarkupBlock.TextBlock(content))
    }

    val blocks = mutableListOf<ChatMarkupBlock>()
    var cursor = 0
    matches.forEach { (match, isSelfClosing) ->
        if (match.range.first < cursor) {
            return@forEach
        }
        if (match.range.first > cursor) {
            blocks += ChatMarkupBlock.TextBlock(content.substring(cursor, match.range.first))
        }

        val rawTagName = match.groupValues[1]
        val attrs = parseAttributesBridge(match.groupValues[2])
        val innerContent =
            if (isSelfClosing) {
                attrs["content"].orEmpty()
            } else {
                match.groupValues[3]
            }
        blocks +=
            ChatMarkupBlock.XmlBlock(
                tagName = normalizeChatMarkupTagNameBridge(rawTagName),
                attributes = attrs,
                content = innerContent,
                raw = match.value,
            )
        cursor = match.range.last + 1
    }

    if (cursor < content.length) {
        blocks += ChatMarkupBlock.TextBlock(content.substring(cursor))
    }

    return blocks.filterNot { block ->
        block is ChatMarkupBlock.TextBlock && block.text.isBlank()
    }
}

private fun parseAttributesBridge(rawAttributes: String): Map<String, String> =
    attributeRegexBridge
        .findAll(rawAttributes)
        .associate { match -> match.groupValues[1] to match.groupValues[2] }

private fun normalizeChatMarkupTagNameBridge(tagName: String): String {
    val normalized = tagName.lowercase()
    return when {
        toolTagNameRegexBridge.matches(normalized) -> "tool"
        toolResultTagNameRegexBridge.matches(normalized) -> "tool_result"
        normalized == "workspace_attachment" -> "workspace"
        else -> normalized
    }
}
