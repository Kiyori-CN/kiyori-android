package com.ai.assistance.operit.ui.features.chat.components.style.input.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ScreenshotMonitor
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.android.kiyori.R

@Composable
fun AttachmentSelectorPopupPanelBridge(
    visible: Boolean,
    onAddAttachment: (String) -> Unit,
    onDismiss: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surface,
) {
    if (!visible) return

    val context = LocalContext.current
    val panelItems =
        remember(context) {
            listOf(
                AttachmentPopupItem(
                    icon = Icons.Default.Image,
                    label = context.getString(R.string.attachment_photo),
                ),
                AttachmentPopupItem(
                    icon = Icons.Default.PhotoCamera,
                    label = context.getString(R.string.attachment_camera),
                ),
                AttachmentPopupItem(
                    icon = Icons.Default.Memory,
                    label = context.getString(R.string.attachment_memory),
                ),
                AttachmentPopupItem(
                    icon = Icons.Default.AudioFile,
                    label = context.getString(R.string.attachment_audio),
                ),
                AttachmentPopupItem(
                    icon = Icons.Default.Description,
                    label = context.getString(R.string.attachment_file),
                ),
                AttachmentPopupItem(
                    icon = Icons.Default.ScreenshotMonitor,
                    label = context.getString(R.string.attachment_screen_content),
                ),
                AttachmentPopupItem(
                    icon = Icons.Default.Notifications,
                    label = context.getString(R.string.attachment_notifications),
                ),
                AttachmentPopupItem(
                    icon = Icons.Default.LocationOn,
                    label = context.getString(R.string.attachment_location),
                ),
            )
        }

    Popup(
        alignment = Alignment.TopStart,
        onDismissRequest = onDismiss,
        properties =
            PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = containerColor,
                shadowElevation = 4.dp,
                modifier =
                    Modifier
                        .padding(bottom = 44.dp, end = 12.dp)
                        .width(200.dp)
                        .heightIn(max = 420.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    panelItems.forEach { item ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clickable {
                                        onAddAttachment(item.label)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class AttachmentPopupItem(
    val icon: ImageVector,
    val label: String,
)
