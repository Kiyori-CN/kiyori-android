package com.android.kiyori.operitreplica.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun KiyoriOperitReplicaInputBar(
    modifier: Modifier = Modifier,
    inputText: String,
    latestUserMessage: String?,
    showReplyPreview: Boolean,
    statusStripText: String,
    attachments: List<String>,
    showFeaturePanel: Boolean,
    showAttachmentPanel: Boolean,
    onInputTextChange: (String) -> Unit,
    onDismissReply: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onToggleFeaturePanel: () -> Unit,
    onToggleAttachmentPanel: () -> Unit,
    onOpenFullscreenEditor: () -> Unit,
    onSubmitPrompt: () -> Unit,
    onAuxiliaryActionClick: (() -> Unit)?,
    onExpandChatClick: (() -> Unit)?,
) {
    Box(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, Color(0xFFE7EAF1), RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showReplyPreview && !latestUserMessage.isNullOrBlank()) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF3F5F9)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Reply,
                                contentDescription = "reply",
                                tint = Color(0xFF4568B2),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = latestUserMessage,
                                fontSize = 12.sp,
                                color = Color(0xFF667085),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = onDismissReply, modifier = Modifier.size(22.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "close reply",
                                    tint = Color(0xFF667085),
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }

                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFD)) {
                    Text(
                        text = statusStripText,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF667085),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }

                if (attachments.isNotEmpty()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        attachments.forEach { attachment ->
                            OperitReplicaAttachmentChip(
                                label = attachment,
                                onRemove = { onRemoveAttachment(attachment) },
                            )
                        }
                    }
                }

                BasicTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    textStyle = TextStyle(fontSize = 14.sp, lineHeight = 22.sp, color = Color(0xFF111111)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSubmitPrompt() }),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 34.dp, max = 140.dp),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (inputText.isBlank()) {
                                Text(
                                    text = "输入任务，让正一屏完全按 Operit 的方式工作",
                                    fontSize = 14.sp,
                                    color = Color(0xFF98A2B3),
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OperitCircleActionButton(
                            backgroundColor =
                                if (showAttachmentPanel) Color(0xFFE8EEF9) else Color(0xFFF4F6FA),
                            icon = Icons.Default.Add,
                            contentDescription = "attach",
                            iconTint = Color(0xFF475467),
                            onClick = onToggleAttachmentPanel,
                        )
                        OperitCircleActionButton(
                            backgroundColor =
                                if (showFeaturePanel) Color(0xFFE8EEF9) else Color(0xFFF4F6FA),
                            icon = Icons.Default.SettingsSuggest,
                            contentDescription = "features",
                            iconTint = Color(0xFF475467),
                            onClick = onToggleFeaturePanel,
                        )
                        OperitCircleActionButton(
                            backgroundColor = Color(0xFFF4F6FA),
                            icon = Icons.Default.Fullscreen,
                            contentDescription = "fullscreen",
                            iconTint = Color(0xFF475467),
                            onClick = onOpenFullscreenEditor,
                        )
                        if (onExpandChatClick != null) {
                            OperitCircleActionButton(
                                backgroundColor = Color(0xFFF4F6FA),
                                icon = Icons.Default.Code,
                                contentDescription = "workspace",
                                iconTint = Color(0xFF475467),
                                onClick = onExpandChatClick,
                            )
                        }
                        if (onAuxiliaryActionClick != null) {
                            OperitCircleActionButton(
                                backgroundColor = Color(0xFFF4F6FA),
                                icon = Icons.Default.MoreHoriz,
                                contentDescription = "more",
                                iconTint = Color(0xFF475467),
                                onClick = onAuxiliaryActionClick,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    OperitCircleActionButton(
                        backgroundColor = Color(0xFF111827),
                        icon = Icons.Default.Send,
                        contentDescription = "send",
                        iconTint = Color.White,
                        onClick = onSubmitPrompt,
                    )
                }
            }
        }
    }
}

@Composable
internal fun OperitReplicaAttachmentChip(
    label: String,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFF4F6FA),
        border = BorderStroke(1.dp, Color(0xFFE5EAF2)),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = null,
                tint = Color(0xFF667085),
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(text = label, fontSize = 11.sp, color = Color(0xFF475467))
            IconButton(onClick = onRemove, modifier = Modifier.size(18.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color(0xFF667085),
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
internal fun OperitCircleActionButton(
    backgroundColor: Color,
    icon: ImageVector,
    contentDescription: String,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
    }
}
