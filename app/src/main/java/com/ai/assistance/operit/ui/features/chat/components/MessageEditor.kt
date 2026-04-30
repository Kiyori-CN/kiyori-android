package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.kiyori.R

data class ParsedMessagePart(
    val type: PartType,
    val content: String,
    val tag: String? = null,
    val attributes: String? = null,
)

enum class PartType {
    TEXT,
    XML,
}

fun parseMessageContentForEditor(content: String): List<ParsedMessagePart> {
    val parts = mutableListOf<ParsedMessagePart>()
    val regex = "<([a-zA-Z0-9_-]+)([^>]*)>([\\s\\S]*?)</\\1>".toRegex(RegexOption.DOT_MATCHES_ALL)
    var lastIndex = 0

    regex.findAll(content).forEach { matchResult ->
        val startIndex = matchResult.range.first
        if (startIndex > lastIndex) {
            val textPart = content.substring(lastIndex, startIndex)
            if (textPart.isNotBlank()) {
                parts.add(ParsedMessagePart(PartType.TEXT, textPart, null, null))
            }
        }
        val tag = matchResult.groupValues[1]
        val attributes = matchResult.groupValues[2]
        val tagContent = matchResult.groupValues[3]
        parts.add(ParsedMessagePart(PartType.XML, tagContent, tag, attributes))
        lastIndex = matchResult.range.last + 1
    }

    if (lastIndex < content.length) {
        val trailingText = content.substring(lastIndex)
        if (trailingText.isNotBlank()) {
            parts.add(ParsedMessagePart(PartType.TEXT, trailingText, null, null))
        }
    }
    return parts.ifEmpty { listOf(ParsedMessagePart(PartType.TEXT, content)) }
}

fun recomposeMessageFromParts(parts: List<ParsedMessagePart>): String {
    return parts.joinToString(separator = "") { part ->
        if (part.type == PartType.TEXT) {
            part.content
        } else {
            "<${part.tag}${part.attributes ?: ""}>${part.content}</${part.tag}>"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageEditor(
    editingMessageContent: MutableState<String>,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onResend: () -> Unit,
    showResendButton: Boolean,
) {
    val context = LocalContext.current
    val initialParts = remember(editingMessageContent.value) {
        parseMessageContentForEditor(editingMessageContent.value)
    }
    var partsState by remember { mutableStateOf(initialParts) }
    var partToEdit by remember { mutableStateOf<Pair<Int, ParsedMessagePart>?>(null) }
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var isRawEditMode by remember { mutableStateOf(false) }

    LaunchedEffect(partsState) {
        if (!isRawEditMode) {
            editingMessageContent.value = recomposeMessageFromParts(partsState)
        }
    }

    LaunchedEffect(isRawEditMode) {
        if (!isRawEditMode) {
            partsState = parseMessageContentForEditor(editingMessageContent.value)
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier =
                Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(0.9f)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Column {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text =
                            if (showResendButton) {
                                context.getString(R.string.edit_message)
                            } else {
                                context.getString(R.string.modify_memory)
                            },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { isRawEditMode = !isRawEditMode }) {
                            Text(
                                if (isRawEditMode) {
                                    context.getString(R.string.visual)
                                } else {
                                    context.getString(R.string.plain_text)
                                },
                            )
                        }

                        IconButton(
                            onClick = onCancel,
                            modifier =
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = context.getString(R.string.cancel),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp),
                ) {
                    if (isRawEditMode) {
                        OutlinedTextField(
                            value = editingMessageContent.value,
                            onValueChange = { editingMessageContent.value = it },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            label = { Text(context.getString(R.string.plain_text_content)) },
                            textStyle =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                            maxLines = 16,
                            minLines = 6,
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                ),
                        )
                    } else {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = context.getString(R.string.content_fragment),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp, top = 4.dp, start = 4.dp),
                            )

                            partsState.forEachIndexed { index, part ->
                                when (part.type) {
                                    PartType.TEXT -> {
                                        Box(modifier = Modifier.padding(bottom = 8.dp)) {
                                            OutlinedTextField(
                                                value = part.content,
                                                onValueChange = { newText ->
                                                    val updatedParts = partsState.toMutableList()
                                                    updatedParts[index] = part.copy(content = newText)
                                                    partsState = updatedParts
                                                },
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(min = 56.dp, max = 260.dp),
                                                label = { Text(context.getString(R.string.text_label)) },
                                                placeholder = { Text(context.getString(R.string.input_text_content)) },
                                                shape = RoundedCornerShape(12.dp),
                                                colors =
                                                    OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                        unfocusedBorderColor =
                                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                        focusedContainerColor =
                                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                        unfocusedContainerColor =
                                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                    ),
                                                textStyle = MaterialTheme.typography.bodyMedium,
                                                maxLines = 12,
                                                minLines = 1,
                                            )
                                            ActionIconButton(
                                                icon = Icons.Default.Delete,
                                                contentDescription = context.getString(R.string.delete),
                                                onClick = {
                                                    val updatedParts = partsState.toMutableList()
                                                    updatedParts.removeAt(index)
                                                    partsState = updatedParts
                                                },
                                                modifier =
                                                    Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(top = 6.dp, end = 4.dp),
                                            )
                                        }
                                    }
                                    PartType.XML -> {
                                        XmlTagItem(
                                            part = part,
                                            onClick = { partToEdit = index to part },
                                            onDelete = {
                                                val updatedParts = partsState.toMutableList()
                                                updatedParts.removeAt(index)
                                                partsState = updatedParts
                                            },
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            ) {
                                OutlinedButton(
                                    onClick = { partsState = partsState + ParsedMessagePart(PartType.TEXT, "") },
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    colors =
                                        ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary,
                                        ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = context.getString(R.string.add_text),
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(context.getString(R.string.add_text))
                                }

                                OutlinedButton(
                                    onClick = { showCreateTagDialog = true },
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    colors =
                                        ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary,
                                        ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.Tag,
                                        contentDescription = context.getString(R.string.add_tag),
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(context.getString(R.string.add_tag))
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(context.getString(R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (showResendButton) {
                        OutlinedButton(
                            onClick = onSave,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(context.getString(R.string.save))
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = onResend,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(context.getString(R.string.save_and_resend))
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    } else {
                        Button(
                            onClick = onSave,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(context.getString(R.string.update_memory))
                        }
                    }
                }
            }
        }
    }

    if (showCreateTagDialog || partToEdit != null) {
        val editingPart = partToEdit?.second
        TagEditorDialog(
            part = editingPart,
            onDismiss = {
                partToEdit = null
                showCreateTagDialog = false
            },
            onSave = { updatedPart ->
                if (partToEdit != null) {
                    partsState = partsState.toMutableList().apply { set(partToEdit!!.first, updatedPart) }
                } else {
                    partsState = partsState + updatedPart
                }
                partToEdit = null
                showCreateTagDialog = false
            },
        )
    }
}

@Composable
private fun XmlTagItem(
    part: ParsedMessagePart,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = part.tag ?: "XML",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = part.content,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ActionIconButton(
                icon = Icons.Default.Edit,
                contentDescription = context.getString(R.string.edit),
                onClick = onClick,
            )
            ActionIconButton(
                icon = Icons.Default.Delete,
                contentDescription = context.getString(R.string.delete),
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun ActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier =
            modifier
                .size(28.dp)
                .clip(CircleShape),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagEditorDialog(
    part: ParsedMessagePart?,
    onDismiss: () -> Unit,
    onSave: (ParsedMessagePart) -> Unit,
) {
    val context = LocalContext.current
    var tagName by remember { mutableStateOf(part?.tag ?: "") }
    var attributes by remember { mutableStateOf(part?.attributes ?: "") }
    var content by remember { mutableStateOf(part?.content ?: "") }
    val isNewTag = part == null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(0.9f)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text =
                        if (isNewTag) {
                            context.getString(R.string.new_tag)
                        } else {
                            context.getString(R.string.edit_tag)
                        },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text =
                        if (isNewTag) {
                            context.getString(R.string.create_new_xml_tag)
                        } else {
                            context.getString(R.string.modify_xml_tag_content)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
                )
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text(context.getString(R.string.tag_name)) },
                    placeholder = { Text(context.getString(R.string.tag_example)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = attributes,
                    onValueChange = { attributes = it },
                    label = { Text(context.getString(R.string.attributes_optional)) },
                    placeholder = { Text(context.getString(R.string.attributes_example)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(context.getString(R.string.content_label)) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(context.getString(R.string.cancel))
                    }
                    Button(
                        onClick = { onSave(ParsedMessagePart(PartType.XML, content, tagName, attributes)) },
                        enabled = tagName.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(context.getString(R.string.save))
                    }
                }
            }
        }
    }
}
