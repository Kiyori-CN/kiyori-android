package com.ai.assistance.operit.ui.features.chat.components.part

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.ui.features.chat.components.ToolCollapseModeBridge
import com.android.kiyori.R

@Composable
fun ChatMarkupRendererBridge(
    content: String,
    textColor: Color,
    backgroundColor: Color = Color.Transparent,
    showThinkingProcess: Boolean = true,
    showStatusTags: Boolean = true,
    toolCollapseMode: ToolCollapseModeBridge = ToolCollapseModeBridge.ALL,
    forceExpandGroups: Boolean = false,
    modifier: Modifier = Modifier,
    renderInlineTextOnly: Boolean = false,
) {
    val blocks = remember(content) { parseChatMarkupBlocksBridge(content) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (renderInlineTextOnly) {
            blocks.forEach { block ->
                RenderChatMarkupBlockBridge(
                    block = block,
                    textColor = textColor,
                    renderInlineTextOnly = true,
                    showThinkingProcess = showThinkingProcess,
                    showStatusTags = showStatusTags,
                )
            }
        } else {
            val groupedBlocks =
                remember(blocks, showThinkingProcess, toolCollapseMode) {
                    groupThinkToolsXmlBlocksBridge(
                        blocks = blocks,
                        showThinkingProcess = showThinkingProcess,
                        toolCollapseMode = toolCollapseMode,
                    )
                }
            groupedBlocks.forEach { item ->
                when (item) {
                    is ChatMarkupGroupedItemBridge.Single ->
                        RenderChatMarkupBlockBridge(
                            block = item.block,
                            textColor = textColor,
                            renderInlineTextOnly = false,
                            showThinkingProcess = showThinkingProcess,
                            showStatusTags = showStatusTags,
                        )

                    is ChatMarkupGroupedItemBridge.Group ->
                        ThinkToolsGroupBridge(
                            blocks = item.blocks,
                            isToolsOnly = item.isToolsOnly,
                            toolCount = item.toolCount,
                            textColor = textColor,
                            showThinkingProcess = showThinkingProcess,
                            showStatusTags = showStatusTags,
                            forceExpand = forceExpandGroups,
                        )
                }
            }
        }
    }
}

@Composable
private fun RenderChatMarkupBlockBridge(
    block: ChatMarkupBlock,
    textColor: Color,
    renderInlineTextOnly: Boolean,
    showThinkingProcess: Boolean,
    showStatusTags: Boolean,
) {
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
                OperitXmlBlockBridge(
                    block = block,
                    textColor = textColor,
                    showThinkingProcess = showThinkingProcess,
                    showStatusTags = showStatusTags,
                )
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
    showThinkingProcess: Boolean,
    showStatusTags: Boolean,
) {
    if (block.tagName == "mood" || shouldHideOperitMetaBridge(block.raw, block.tagName)) {
        return
    }
    if ((block.tagName == "think" || block.tagName == "thinking") && !showThinkingProcess) {
        return
    }

    when (block.tagName) {
        "attachment", "workspace" -> AttachmentChipBlock(block = block, textColor = textColor)
        "think", "thinking" ->
            OperitThinkingProcessBridge(
                content = block.content,
                textColor = textColor,
            )

        "search" ->
            OperitSearchSourcesBridge(
                content = block.content,
                textColor = textColor,
            )

        "tool" ->
            OperitToolCallDisplayBridge(
                toolName = block.resolveToolNameBridge(),
                params = block.content.ifBlank { block.raw },
                textColor = textColor,
            )

        "tool_result" ->
            OperitToolResultDisplayBridge(
                toolName = block.resolveToolNameBridge().ifBlank { stringResource(R.string.unknown_tool) },
                result = resolveToolResultContent(block),
                isSuccess = resolveToolResultSuccess(block),
            )

        "status" ->
            if (shouldShowStatusBlockBridge(block = block, showStatusTags = showStatusTags)) {
                OperitStatusBridge(
                    statusType = block.attributes["type"].orEmpty().ifBlank { "info" },
                    content = block.content.ifBlank { block.attributes["message"].orEmpty() },
                    textColor = textColor,
                )
            }

        "font" ->
            OperitFontTagBridge(
                xmlContent = block.raw,
                textColor = textColor,
            )

        "details", "detail" ->
            OperitDetailsTagBridge(
                xmlContent = block.raw,
                textColor = textColor,
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
private fun ThinkToolsGroupBridge(
    blocks: List<ChatMarkupBlock.XmlBlock>,
    isToolsOnly: Boolean,
    toolCount: Int,
    textColor: Color,
    showThinkingProcess: Boolean,
    showStatusTags: Boolean,
    forceExpand: Boolean,
) {
    var expanded by remember(blocks, forceExpand) { mutableStateOf(forceExpand) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = if (forceExpand) 0 else 300),
        label = "thinkToolsGroupArrowRotation",
    )
    val title =
        stringResource(
            if (isToolsOnly) {
                R.string.tools_group_title_with_count
            } else {
                R.string.thinking_tools_group_title_with_count
            },
            toolCount,
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
    ) {
        SourceStyleExpandableHeader(
            title = title,
            expanded = expanded,
            rotationDegrees = rotation,
            titleColor = textColor.copy(alpha = 0.70f),
            onClick = { expanded = !expanded },
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(durationMillis = if (forceExpand) 0 else 200)),
            exit = fadeOut(animationSpec = tween(durationMillis = if (forceExpand) 0 else 200)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp, start = 24.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    blocks.forEach { block ->
                        OperitXmlBlockBridge(
                            block = block,
                            textColor = textColor,
                            showThinkingProcess = showThinkingProcess,
                            showStatusTags = showStatusTags,
                        )
                    }
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

private fun resolveToolResultContent(block: ChatMarkupBlock.XmlBlock): String {
    val rawContent = block.content.ifBlank { block.attributes["content"].orEmpty() }
    val contentMatch =
        Regex("<content\\b[^>]*>([\\s\\S]*?)</content>", RegexOption.IGNORE_CASE)
            .find(rawContent)
    return (contentMatch?.groupValues?.getOrNull(1) ?: rawContent).trim()
}

private fun resolveToolResultSuccess(block: ChatMarkupBlock.XmlBlock): Boolean {
    val status =
        block.attributes["status"]
            ?: block.attributes["success"]
            ?: block.attributes["state"]
            ?: "success"
    return status.lowercase() !in setOf("false", "failed", "fail", "error")
}

private fun shouldShowStatusBlockBridge(
    block: ChatMarkupBlock.XmlBlock,
    showStatusTags: Boolean,
): Boolean {
    if (showStatusTags) {
        return true
    }
    val statusType = block.attributes["type"].orEmpty().lowercase()
    return statusType !in setOf("completion", "complete", "wait_for_user_need")
}
