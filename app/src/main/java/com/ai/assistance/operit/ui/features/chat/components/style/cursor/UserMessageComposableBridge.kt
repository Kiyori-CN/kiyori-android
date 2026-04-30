package com.ai.assistance.operit.ui.features.chat.components.style.cursor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.ui.features.chat.components.ChatActionSurfaceDisplayMode
import com.ai.assistance.operit.ui.features.chat.components.ChatActionSurfaceMessage
import com.ai.assistance.operit.ui.features.chat.components.part.ChatMarkupRendererBridge
import com.android.kiyori.R

@Composable
fun UserMessageComposableBridge(
    message: ChatActionSurfaceMessage,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val isHiddenPlaceholder =
        message.displayMode == ChatActionSurfaceDisplayMode.HIDDEN_PLACEHOLDER
    val parseResult =
        remember(message.content, isHiddenPlaceholder) {
            if (isHiddenPlaceholder) {
                UserMessageParseResult(processedText = "", trailingAttachments = emptyList())
            } else {
                parseUserMessageContentBridge(message.content)
            }
        }
    val proxySenderName = parseResult.proxySenderName
    val isProxySender = !proxySenderName.isNullOrBlank()
    val effectiveBackgroundColor =
        when {
            isHiddenPlaceholder -> Color.Transparent
            isProxySender -> MaterialTheme.colorScheme.secondaryContainer
            else -> backgroundColor
        }
    val effectiveTextColor =
        when {
            isProxySender -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> textColor
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = if (isHiddenPlaceholder) 0.dp else 4.dp),
    ) {
        parseResult.replyInfo?.let { reply ->
            ReplyPreviewBridge(
                replyInfo = reply,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
            )
        }

        if (parseResult.trailingAttachments.isNotEmpty()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                parseResult.trailingAttachments.forEach { attachment ->
                    AttachmentTagBridge(
                        attachment = attachment,
                        textColor = effectiveTextColor,
                        backgroundColor = effectiveBackgroundColor,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }

        Card(
            modifier =
                if (isHiddenPlaceholder) {
                    Modifier.widthIn(max = 320.dp)
                } else {
                    Modifier.fillMaxWidth()
                },
            colors = CardDefaults.cardColors(containerColor = effectiveBackgroundColor),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            top = if (isHiddenPlaceholder) 0.dp else 16.dp,
                            end = 16.dp,
                            bottom = if (isHiddenPlaceholder) 0.dp else 16.dp,
                        ),
            ) {
                if (isHiddenPlaceholder) {
                    HiddenUserMessagePlaceholderBridge(textColor = effectiveTextColor)
                } else {
                    Text(
                        text =
                            if (!proxySenderName.isNullOrBlank()) {
                                "Prompt by $proxySenderName"
                            } else {
                                "Prompt"
                            },
                        style = MaterialTheme.typography.labelSmall,
                        color = effectiveTextColor.copy(alpha = 0.70f),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    ChatMarkupRendererBridge(
                        content = parseResult.processedText,
                        textColor = effectiveTextColor,
                        renderInlineTextOnly = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplyPreviewBridge(
    replyInfo: ReplyInfoBridge,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Reply,
                contentDescription = stringResource(R.string.reply_message),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(12.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${replyInfo.sender}: ${replyInfo.content}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HiddenUserMessagePlaceholderBridge(
    textColor: Color,
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.chat_hidden_user_message_badge),
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.70f),
        )
        Text(
            text = stringResource(R.string.chat_hidden_user_message_placeholder),
            style = MaterialTheme.typography.bodySmall,
            color = textColor.copy(alpha = 0.72f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AttachmentTagBridge(
    attachment: AttachmentDataBridge,
    textColor: Color,
    backgroundColor: Color,
) {
    val icon: ImageVector =
        when {
            attachment.type.startsWith("image/") -> Icons.Default.Image
            attachment.type.startsWith("audio/") -> Icons.Default.VolumeUp
            attachment.type.startsWith("video/") -> Icons.Default.PlayArrow
            attachment.type == "text/json" && attachment.filename == "screen_content.json" ->
                Icons.Default.ScreenshotMonitor

            attachment.type == "application/vnd.workspace-context+xml" -> Icons.Default.Code
            else -> Icons.Default.Description
        }
    val displayLabel =
        when {
            attachment.type == "text/json" && attachment.filename == "screen_content.json" ->
                stringResource(R.string.attachment_screen_content)

            attachment.type == "application/vnd.workspace-context+xml" ->
                stringResource(R.string.workspace)

            else -> attachment.filename
        }

    Surface(
        modifier =
            Modifier
                .height(24.dp)
                .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor.copy(alpha = 0.50f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.width(12.dp),
                tint = textColor.copy(alpha = 0.80f),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = displayLabel,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = textColor,
                modifier = Modifier.widthIn(max = 120.dp),
            )
        }
    }
}

private data class UserMessageParseResult(
    val processedText: String,
    val trailingAttachments: List<AttachmentDataBridge>,
    val replyInfo: ReplyInfoBridge? = null,
    val proxySenderName: String? = null,
)

private data class ReplyInfoBridge(
    val sender: String,
    val timestamp: Long,
    val content: String,
)

private data class AttachmentDataBridge(
    val id: String,
    val filename: String,
    val type: String,
    val size: Long = 0,
    val content: String = "",
)

private val memoryTagRegex = Regex("<memory>.*?</memory>", RegexOption.DOT_MATCHES_ALL)
private val proxySenderRegex =
    Regex("<proxy_sender\\s+name=\"([^\"]+)\"\\s*/>", RegexOption.IGNORE_CASE)
private val replyToRegex =
    Regex("<reply_to\\s+sender=\"([^\"]+)\"\\s+timestamp=\"([^\"]+)\">([^<]*)</reply_to>")
private val workspaceAttachmentRegex =
    Regex(
        "<workspace_attachment\\b[\\s\\S]*?</workspace_attachment>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
private val attachmentDataRegex =
    Regex(
        "<attachment\\s+id=\"([^\"]+)\"\\s+filename=\"([^\"]+)\"\\s+type=\"([^\"]+)\"(?:\\s+size=\"([^\"]+)\")?\\s*>([\\s\\S]*?)</attachment>",
    )
private val attachmentDataSelfClosingRegex =
    Regex(
        "<attachment\\s+id=\"([^\"]+)\"\\s+filename=\"([^\"]+)\"\\s+type=\"([^\"]+)\"(?:\\s+size=\"([^\"]+)\")?(?:\\s+content=\"(.*?)\")?\\s*/>",
        RegexOption.DOT_MATCHES_ALL,
    )

private fun parseUserMessageContentBridge(content: String): UserMessageParseResult {
    var cleanedContent = content.replace(memoryTagRegex, "").trim()
    val proxySenderMatch = proxySenderRegex.find(cleanedContent)
    val proxySenderName = proxySenderMatch?.groupValues?.getOrNull(1)
    if (proxySenderMatch != null) {
        cleanedContent = cleanedContent.replace(proxySenderMatch.value, "").trim()
    }

    val replyMatch = replyToRegex.find(cleanedContent)
    val replyInfo =
        replyMatch?.let { match ->
            ReplyInfoBridge(
                sender = match.groupValues[1],
                timestamp = match.groupValues[2].toLongOrNull() ?: 0L,
                content = match.groupValues[3].trim().removeSurrounding("\""),
            )
        }
    if (replyMatch != null) {
        cleanedContent = cleanedContent.replace(replyMatch.value, "").trim()
    }

    val trailingAttachments = mutableListOf<AttachmentDataBridge>()
    val workspaceMatch = workspaceAttachmentRegex.find(cleanedContent)
    if (workspaceMatch != null) {
        trailingAttachments +=
            AttachmentDataBridge(
                id = "workspace_context",
                filename = "workspace",
                type = "application/vnd.workspace-context+xml",
                size = workspaceMatch.value.length.toLong(),
                content = workspaceMatch.value,
            )
        cleanedContent = cleanedContent.replace(workspaceMatch.value, "").trim()
    }

    if (!cleanedContent.contains("<attachment")) {
        return UserMessageParseResult(
            processedText = cleanedContent,
            trailingAttachments = trailingAttachments,
            replyInfo = replyInfo,
            proxySenderName = proxySenderName,
        )
    }

    return parseAttachmentTagsBridge(
        cleanedContent = cleanedContent,
        baseTrailingAttachments = trailingAttachments,
        replyInfo = replyInfo,
        proxySenderName = proxySenderName,
    )
}

private fun parseAttachmentTagsBridge(
    cleanedContent: String,
    baseTrailingAttachments: List<AttachmentDataBridge>,
    replyInfo: ReplyInfoBridge?,
    proxySenderName: String?,
): UserMessageParseResult {
    val pairedMatches = attachmentDataRegex.findAll(cleanedContent).toList()
    val selfClosingMatches = attachmentDataSelfClosingRegex.findAll(cleanedContent).toList()
    val matches =
        (pairedMatches.map { it to true } + selfClosingMatches.map { it to false })
            .sortedBy { it.first.range.first }
            .filterNonOverlappingMatches()

    if (matches.isEmpty()) {
        return UserMessageParseResult(
            processedText = cleanedContent,
            trailingAttachments = baseTrailingAttachments,
            replyInfo = replyInfo,
            proxySenderName = proxySenderName,
        )
    }

    val trailingAttachmentIndices = findTrailingAttachmentIndices(cleanedContent, matches)
    val trailingAttachments = baseTrailingAttachments.toMutableList()
    val messageText = StringBuilder()
    var lastIndex = 0
    matches.forEachIndexed { index, (matchResult, _) ->
        val startIndex = matchResult.range.first
        val attachment =
            AttachmentDataBridge(
                id = matchResult.groupValues[1],
                filename = matchResult.groupValues[2],
                type = matchResult.groupValues[3],
                size = matchResult.groupValues[4].toLongOrNull() ?: 0L,
                content = matchResult.groupValues.getOrNull(5).orEmpty(),
            )
        val isTrailingAttachment = trailingAttachmentIndices.contains(index)
        val isScreenContent = attachment.type == "text/json" && attachment.filename == "screen_content.json"
        val shouldBeTrailing = isTrailingAttachment || isScreenContent
        if (startIndex > lastIndex) {
            val textBefore = cleanedContent.substring(lastIndex, startIndex)
            if (!shouldBeTrailing || (trailingAttachmentIndices.isNotEmpty() && index == trailingAttachmentIndices.minOrNull())) {
                messageText.append(textBefore)
            }
        }
        if (shouldBeTrailing) {
            trailingAttachments += attachment
        } else {
            messageText.append("@${attachment.filename}")
        }
        lastIndex = matchResult.range.last + 1
    }
    if (lastIndex < cleanedContent.length) {
        messageText.append(cleanedContent.substring(lastIndex))
    }

    return UserMessageParseResult(
        processedText = messageText.toString(),
        trailingAttachments = trailingAttachments,
        replyInfo = replyInfo,
        proxySenderName = proxySenderName,
    )
}

private fun List<Pair<MatchResult, Boolean>>.filterNonOverlappingMatches(): List<Pair<MatchResult, Boolean>> {
    val filtered = mutableListOf<Pair<MatchResult, Boolean>>()
    var lastEnd = -1
    forEach { match ->
        if (match.first.range.first > lastEnd) {
            filtered += match
            lastEnd = match.first.range.last
        }
    }
    return filtered
}

private fun findTrailingAttachmentIndices(
    cleanedContent: String,
    matches: List<Pair<MatchResult, Boolean>>,
): Set<Int> {
    val trailingAttachmentIndices = mutableSetOf<Int>()
    val contentAfterLast = cleanedContent.substring(matches.last().first.range.last + 1)
    if (contentAfterLast.isBlank()) {
        trailingAttachmentIndices += matches.lastIndex
        for (i in matches.lastIndex - 1 downTo 0) {
            val textBetween =
                cleanedContent.substring(
                    matches[i].first.range.last + 1,
                    matches[i + 1].first.range.first,
                )
            if (textBetween.isBlank()) {
                trailingAttachmentIndices += i
            } else {
                break
            }
        }
    }
    return trailingAttachmentIndices
}
