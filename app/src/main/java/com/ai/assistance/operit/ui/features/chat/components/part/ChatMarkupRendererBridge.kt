package com.ai.assistance.operit.ui.features.chat.components.part

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.kiyori.R

private val pairedMarkupRegex =
    Regex(
        pattern = "<(think|thinking|search|tool|tool_result|status|workspace|attachment)(\\b[^>]*)>([\\s\\S]*?)</\\1>",
        options = setOf(RegexOption.IGNORE_CASE),
    )

private val selfClosingMarkupRegex =
    Regex(
        pattern = "<(think|thinking|search|tool|tool_result|status|workspace|attachment)(\\b[^>]*)/>",
        options = setOf(RegexOption.IGNORE_CASE),
    )

private val attributeRegex =
    Regex("""([a-zA-Z0-9_-]+)\s*=\s*["']([^"']*)["']""")

private sealed interface ChatMarkupBlock {
    data class TextBlock(val text: String) : ChatMarkupBlock

    data class XmlBlock(
        val tagName: String,
        val attributes: Map<String, String>,
        val content: String,
        val raw: String,
    ) : ChatMarkupBlock
}

@Composable
fun ChatMarkupRendererBridge(
    content: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    renderInlineTextOnly: Boolean = false,
) {
    val blocks = remember(content) { parseChatMarkupBlocks(content) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is ChatMarkupBlock.TextBlock -> {
                    if (block.text.isNotBlank()) {
                        SelectionContainer {
                            Text(
                                text = block.text.trim(),
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                is ChatMarkupBlock.XmlBlock -> {
                    if (renderInlineTextOnly) {
                        InlineAttachmentOrTextBlock(block = block, textColor = textColor)
                    } else {
                        OperitXmlBlockBridge(block = block, textColor = textColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineAttachmentOrTextBlock(
    block: ChatMarkupBlock.XmlBlock,
    textColor: Color,
) {
    when (block.tagName) {
        "attachment" -> AttachmentChipBlock(block = block, textColor = textColor)
        "workspace" -> AttachmentChipBlock(block = block.copy(tagName = "workspace"), textColor = textColor)
        else -> {
            val cleanText = block.content.ifBlank { block.raw }
            if (cleanText.isNotBlank()) {
                Text(
                    text = cleanText.trim(),
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun OperitXmlBlockBridge(
    block: ChatMarkupBlock.XmlBlock,
    textColor: Color,
) {
    when (block.tagName) {
        "attachment", "workspace" -> AttachmentChipBlock(block = block, textColor = textColor)
        "think", "thinking" ->
            ToolLikeBlock(
                title = stringResource(R.string.thinking_process_block),
                icon = Icons.Default.Psychology,
                content = block.content,
                textColor = textColor,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f),
            )

        "search" ->
            ToolLikeBlock(
                title = stringResource(R.string.search_content_block),
                icon = Icons.Default.Search,
                content = block.content,
                textColor = textColor,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
            )

        "tool" ->
            ToolLikeBlock(
                title = resolveToolTitle(block, stringResource(R.string.tool_call_block)),
                icon = Icons.Default.Terminal,
                content = block.content.ifBlank { block.raw },
                textColor = textColor,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f),
                monospace = true,
            )

        "tool_result" ->
            ToolLikeBlock(
                title = resolveToolTitle(block, stringResource(R.string.tool_result_block)),
                icon = Icons.Default.Code,
                content = block.content.ifBlank { block.raw },
                textColor = textColor,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f),
                monospace = true,
            )

        "status" ->
            ToolLikeBlock(
                title = resolveStatusTitle(block),
                icon = Icons.Default.Settings,
                content = block.content.ifBlank { block.attributes["message"].orEmpty() },
                textColor = textColor,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
            )

        else ->
            Text(
                text = block.raw,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
            )
    }
}

@Composable
private fun ToolLikeBlock(
    title: String,
    icon: ImageVector,
    content: String,
    textColor: Color,
    containerColor: Color,
    monospace: Boolean = false,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            val cleanContent = content.trim()
            if (cleanContent.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = cleanContent,
                        color = textColor.copy(alpha = 0.86f),
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentChipBlock(
    block: ChatMarkupBlock.XmlBlock,
    textColor: Color,
) {
    val isWorkspace = block.tagName == "workspace"
    val filename =
        if (isWorkspace) {
            stringResource(R.string.workspace)
        } else {
            block.attributes["filename"]
                ?: block.attributes["name"]
                ?: block.attributes["fileName"]
                ?: stringResource(R.string.attachment_file)
        }
    val mimeType = block.attributes["type"].orEmpty()
    val icon =
        when {
            isWorkspace -> Icons.Default.WorkspacePremium
            mimeType.startsWith("image/") -> Icons.Default.Image
            else -> Icons.Default.Description
        }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.76f),
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = filename,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp),
            )
        }
    }
}

private fun parseChatMarkupBlocks(content: String): List<ChatMarkupBlock> {
    if (content.isBlank()) {
        return emptyList()
    }

    val matches =
        (pairedMarkupRegex.findAll(content).map { it to false } +
            selfClosingMarkupRegex.findAll(content).map { it to true })
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

        val tagName = match.groupValues[1].lowercase()
        val attrs = parseAttributes(match.groupValues[2])
        val innerContent =
            if (isSelfClosing) {
                attrs["content"].orEmpty()
            } else {
                match.groupValues[3]
            }
        blocks +=
            ChatMarkupBlock.XmlBlock(
                tagName = tagName,
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

private fun parseAttributes(rawAttributes: String): Map<String, String> =
    attributeRegex
        .findAll(rawAttributes)
        .associate { match -> match.groupValues[1] to match.groupValues[2] }

private fun resolveToolTitle(
    block: ChatMarkupBlock.XmlBlock,
    fallback: String,
): String {
    val toolName =
        block.attributes["name"]
            ?: block.attributes["tool"]
            ?: block.attributes["type"]
    return toolName?.takeIf { it.isNotBlank() }?.let { "$fallback: $it" } ?: fallback
}

@Composable
private fun resolveStatusTitle(block: ChatMarkupBlock.XmlBlock): String {
    val type = block.attributes["type"]
    return type?.takeIf { it.isNotBlank() }?.let {
        "${stringResource(R.string.status_info_block)}: $it"
    } ?: stringResource(R.string.status_info_block)
}
