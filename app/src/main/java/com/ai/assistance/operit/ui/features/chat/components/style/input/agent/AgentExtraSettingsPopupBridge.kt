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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.android.kiyori.R

@Composable
fun AgentExtraSettingsPopupBridge(
    visible: Boolean,
    currentMemoryProfileLabel: String,
    enableMemoryAutoUpdate: Boolean,
    isAutoReadEnabled: Boolean,
    isAutoApproveEnabled: Boolean,
    disableTools: Boolean,
    disableStreamOutput: Boolean,
    disableUserPreferenceDescription: Boolean,
    disableStatusTags: Boolean,
    onManageMemory: () -> Unit,
    onManualMemoryUpdate: () -> Unit,
    onToggleMemoryAutoUpdate: () -> Unit,
    onToggleAutoRead: () -> Unit,
    onToggleAutoApprove: () -> Unit,
    onToggleTools: () -> Unit,
    onToggleDisableStreamOutput: () -> Unit,
    onToggleDisableUserPreferenceDescription: () -> Unit,
    onToggleDisableStatusTags: () -> Unit,
    onManageTools: () -> Unit,
    onDismiss: () -> Unit,
    popupContainerColor: Color = MaterialTheme.colorScheme.surface,
) {
    if (!visible) return

    var showMemoryDropdown by remember { mutableStateOf(false) }
    var showDisableSettingsDropdown by remember { mutableStateOf(false) }
    var infoPopupContent by remember { mutableStateOf<Pair<String, String>?>(null) }
    val context = LocalContext.current

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
            Card(
                modifier =
                    Modifier
                        .padding(bottom = 44.dp, end = 12.dp)
                        .width(300.dp)
                        .heightIn(max = 420.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = popupContainerColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    AgentMemorySelectorItemBridge(
                        currentProfileName = currentMemoryProfileLabel,
                        expanded = showMemoryDropdown,
                        onExpandedChange = { showMemoryDropdown = it },
                        onManageClick = {
                            onDismiss()
                            onManageMemory()
                        },
                        onInfoClick = {
                            infoPopupContent =
                                context.getString(R.string.memory) to
                                    context.getString(R.string.memory_desc)
                        },
                    )

                    AgentSimpleToggleSettingItemBridge(
                        title = stringResource(R.string.memory_auto_update),
                        icon = if (enableMemoryAutoUpdate) Icons.Rounded.Save else Icons.Outlined.Save,
                        isChecked = enableMemoryAutoUpdate,
                        onToggle = onToggleMemoryAutoUpdate,
                        onInfoClick = {
                            infoPopupContent =
                                context.getString(R.string.memory_auto_update) to
                                    context.getString(R.string.memory_auto_update_desc)
                        },
                    )

                    AgentActionSettingItemBridge(
                        title = stringResource(R.string.manual_memory_update),
                        icon = Icons.Outlined.Save,
                        onClick = {
                            onDismiss()
                            onManualMemoryUpdate()
                        },
                        onInfoClick = {
                            infoPopupContent =
                                context.getString(R.string.manual_memory_update) to
                                    context.getString(R.string.manual_memory_update_desc)
                        },
                    )

                    AgentSimpleToggleSettingItemBridge(
                        title = stringResource(R.string.auto_read_message),
                        icon =
                            if (isAutoReadEnabled) {
                                Icons.Default.VolumeUp
                            } else {
                                Icons.Default.VolumeOff
                            },
                        isChecked = isAutoReadEnabled,
                        onToggle = onToggleAutoRead,
                        onInfoClick = {
                            infoPopupContent =
                                context.getString(R.string.auto_read_message) to
                                    context.getString(R.string.auto_read_desc)
                        },
                    )

                    AgentSimpleToggleSettingItemBridge(
                        title = stringResource(R.string.auto_approve),
                        icon =
                            if (isAutoApproveEnabled) {
                                Icons.Rounded.Security
                            } else {
                                Icons.Outlined.Security
                            },
                        isChecked = isAutoApproveEnabled,
                        onToggle = onToggleAutoApprove,
                        onInfoClick = {
                            infoPopupContent =
                                context.getString(R.string.auto_approve) to
                                    context.getString(R.string.auto_approve_desc)
                        },
                    )

                    AgentDisableSettingsGroupItemBridge(
                        disableTools = disableTools,
                        disableStreamOutput = disableStreamOutput,
                        disableUserPreferenceDescription = disableUserPreferenceDescription,
                        disableStatusTags = disableStatusTags,
                        expanded = showDisableSettingsDropdown,
                        onExpandedChange = { showDisableSettingsDropdown = it },
                        onToggleTools = onToggleTools,
                        onToggleDisableStreamOutput = onToggleDisableStreamOutput,
                        onToggleDisableUserPreferenceDescription = onToggleDisableUserPreferenceDescription,
                        onToggleDisableStatusTags = onToggleDisableStatusTags,
                        onManageTools = {
                            onDismiss()
                            onManageTools()
                        },
                        onInfoClick = {
                            infoPopupContent =
                                context.getString(R.string.disable_settings_group) to
                                    context.getString(R.string.disable_settings_group_desc)
                        },
                        onDisableStreamOutputInfoClick = {
                            infoPopupContent =
                                context.getString(R.string.disable_stream_output) to
                                    context.getString(R.string.disable_stream_output_desc)
                        },
                        onDisableToolsInfoClick = {
                            infoPopupContent =
                                context.getString(R.string.disable_tools) to
                                    context.getString(R.string.disable_tools_desc)
                        },
                        onDisableUserPreferenceDescriptionInfoClick = {
                            infoPopupContent =
                                context.getString(R.string.disable_user_preference_description) to
                                    context.getString(R.string.disable_user_preference_description_desc)
                        },
                        onDisableStatusTagsInfoClick = {
                            infoPopupContent =
                                context.getString(R.string.disable_status_tags) to
                                    context.getString(R.string.disable_status_tags_desc)
                        },
                    )
                }
            }

            infoPopupContent?.let { content ->
                AgentInfoPopupBridge(
                    popupContainerColor = popupContainerColor,
                    infoPopupContent = content,
                    onDismiss = { infoPopupContent = null },
                )
            }
        }
    }
}

@Composable
private fun AgentMemorySelectorItemBridge(
    currentProfileName: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onManageClick: () -> Unit,
    onInfoClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .clickable { onExpandedChange(!expanded) }
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.DataObject,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp),
        )
        IconButton(onClick = onInfoClick, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.details),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.memory),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = currentProfileName,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp),
        )
    }

    if (expanded) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(32.dp))
                Text(
                    text = currentProfileName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clickable(onClick = onManageClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.manage_memory),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun AgentDisableSettingsGroupItemBridge(
    disableTools: Boolean,
    disableStreamOutput: Boolean,
    disableUserPreferenceDescription: Boolean,
    disableStatusTags: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onToggleTools: () -> Unit,
    onToggleDisableStreamOutput: () -> Unit,
    onToggleDisableUserPreferenceDescription: () -> Unit,
    onToggleDisableStatusTags: () -> Unit,
    onManageTools: () -> Unit,
    onInfoClick: () -> Unit,
    onDisableStreamOutputInfoClick: () -> Unit,
    onDisableToolsInfoClick: () -> Unit,
    onDisableUserPreferenceDescriptionInfoClick: () -> Unit,
    onDisableStatusTagsInfoClick: () -> Unit,
) {
    val disabledCount =
        listOf(
            disableStreamOutput,
            disableTools,
            disableUserPreferenceDescription,
            disableStatusTags,
        ).count { it }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .clickable { onExpandedChange(!expanded) }
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Block,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp),
        )
        IconButton(onClick = onInfoClick, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.details),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.disable_settings_group) + ":",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$disabledCount/4",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }

    if (expanded) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp),
        ) {
            AgentSimpleToggleSettingItemBridge(
                title = stringResource(R.string.disable_stream_output),
                icon = if (disableStreamOutput) Icons.Outlined.Block else Icons.Outlined.Speed,
                isChecked = disableStreamOutput,
                onToggle = onToggleDisableStreamOutput,
                onInfoClick = onDisableStreamOutputInfoClick,
            )
            AgentSimpleToggleSettingItemBridge(
                title = stringResource(R.string.disable_tools),
                icon = Icons.Outlined.Block,
                isChecked = disableTools,
                onToggle = onToggleTools,
                onInfoClick = onDisableToolsInfoClick,
            )
            AgentSimpleToggleSettingItemBridge(
                title = stringResource(R.string.disable_user_preference_description),
                icon = Icons.Outlined.Block,
                isChecked = disableUserPreferenceDescription,
                onToggle = onToggleDisableUserPreferenceDescription,
                onInfoClick = onDisableUserPreferenceDescriptionInfoClick,
            )
            AgentSimpleToggleSettingItemBridge(
                title = stringResource(R.string.disable_status_tags),
                icon = Icons.Outlined.Block,
                isChecked = disableStatusTags,
                onToggle = onToggleDisableStatusTags,
                onInfoClick = onDisableStatusTagsInfoClick,
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clickable(onClick = onManageTools),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.manage_tools),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun AgentSimpleToggleSettingItemBridge(
    title: String,
    icon: ImageVector,
    isChecked: Boolean,
    isEnabled: Boolean = true,
    onToggle: () -> Unit,
    onInfoClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .toggleable(
                    value = isChecked,
                    enabled = isEnabled,
                    onValueChange = { onToggle() },
                    role = Role.Switch,
                )
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint =
                if (!isEnabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                } else if (isChecked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                },
            modifier = Modifier.size(16.dp),
        )
        IconButton(onClick = onInfoClick, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.details),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color =
                if (isEnabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = isChecked,
            onCheckedChange = null,
            enabled = isEnabled,
            modifier = Modifier.size(width = 40.dp, height = 24.dp),
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
        )
    }
}

@Composable
private fun AgentActionSettingItemBridge(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp),
        )
        IconButton(onClick = onInfoClick, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.details),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun AgentInfoPopupBridge(
    popupContainerColor: Color,
    infoPopupContent: Pair<String, String>,
    onDismiss: () -> Unit,
) {
    Popup(
        alignment = Alignment.TopStart,
        onDismissRequest = onDismiss,
        properties =
            PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
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
            Card(
                modifier =
                    Modifier
                        .padding(bottom = 52.dp, end = 12.dp)
                        .width(220.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = popupContainerColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = infoPopupContent.first,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = infoPopupContent.second,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}
