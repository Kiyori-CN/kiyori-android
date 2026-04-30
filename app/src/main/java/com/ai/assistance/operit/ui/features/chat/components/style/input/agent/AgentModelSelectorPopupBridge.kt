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
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlin.math.roundToInt

@Composable
fun AgentModelSelectorPopupBridge(
    visible: Boolean,
    currentModelLabel: String,
    availableModelLabels: List<String>,
    enableThinkingMode: Boolean,
    thinkingQualityLevel: Int,
    enableMaxContextMode: Boolean,
    baseContextLengthInK: Float,
    maxContextLengthInK: Float,
    onToggleThinkingMode: () -> Unit,
    onThinkingQualityLevelChange: (Int) -> Unit,
    onToggleEnableMaxContextMode: () -> Unit,
    onSelectModelLabel: (String) -> Unit,
    onManageModels: () -> Unit,
    onDismiss: () -> Unit,
    popupContainerColor: Color = MaterialTheme.colorScheme.surface,
) {
    if (!visible) return

    var showThinkingDropdown by remember { mutableStateOf(false) }
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
                    AgentThinkingSettingsItemBridge(
                        popupContainerColor = popupContainerColor,
                        enableThinkingMode = enableThinkingMode,
                        thinkingQualityLevel = thinkingQualityLevel,
                        expanded = showThinkingDropdown,
                        onExpandedChange = { showThinkingDropdown = it },
                        onToggleThinkingMode = onToggleThinkingMode,
                        onThinkingQualityLevelChange = onThinkingQualityLevelChange,
                        onInfoClick = {
                            infoPopupContent =
                                context.getString(R.string.thinking_settings) to
                                    context.getString(R.string.thinking_settings_desc)
                        },
                        onThinkingModeInfoClick = {
                            infoPopupContent =
                                context.getString(R.string.thinking_mode) to
                                    context.getString(R.string.thinking_mode_desc)
                        },
                        onThinkingQualityInfoClick = {
                            infoPopupContent =
                                context.getString(R.string.thinking_quality) to
                                    context.getString(R.string.thinking_quality_desc)
                        },
                    )

                    AgentMaxContextSettingItemBridge(
                        enableMaxContextMode = enableMaxContextMode,
                        onToggleEnableMaxContextMode = onToggleEnableMaxContextMode,
                        onInfoClick = {
                            val normalLengthText =
                                if (baseContextLengthInK % 1f == 0f) {
                                    baseContextLengthInK.toInt().toString()
                                } else {
                                    String.format("%.1f", baseContextLengthInK)
                                }
                            val maxLengthText =
                                if (maxContextLengthInK % 1f == 0f) {
                                    maxContextLengthInK.toInt().toString()
                                } else {
                                    String.format("%.1f", maxContextLengthInK)
                                }
                            infoPopupContent =
                                context.getString(R.string.max_mode_title) to
                                    context.getString(
                                        R.string.max_mode_info,
                                        normalLengthText,
                                        maxLengthText,
                                    )
                        },
                    )

                    AgentModelSelectorItemBridge(
                        currentModelLabel = currentModelLabel,
                        availableModelLabels = availableModelLabels,
                        onSelectModelLabel = {
                            onSelectModelLabel(it)
                            onDismiss()
                        },
                        onManageClick = {
                            onDismiss()
                            onManageModels()
                        },
                        onInfoClick = {
                            infoPopupContent =
                                context.getString(R.string.model_config) to
                                    context.getString(R.string.model_config_desc)
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
private fun AgentThinkingSettingsItemBridge(
    popupContainerColor: Color,
    enableThinkingMode: Boolean,
    thinkingQualityLevel: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onToggleThinkingMode: () -> Unit,
    onThinkingQualityLevelChange: (Int) -> Unit,
    onInfoClick: () -> Unit,
    onThinkingModeInfoClick: () -> Unit,
    onThinkingQualityInfoClick: () -> Unit,
) {
    val thinkingTypeText =
        if (enableThinkingMode) {
            stringResource(R.string.thinking_type_mode)
        } else {
            stringResource(R.string.thinking_type_off)
        }

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
            imageVector = Icons.Rounded.Psychology,
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
            text = stringResource(R.string.thinking_settings) + ":",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = thinkingTypeText,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
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
                    .background(popupContainerColor)
                    .padding(horizontal = 12.dp),
        ) {
            AgentThinkingSubSettingItemBridge(
                title = stringResource(R.string.thinking_mode),
                icon = if (enableThinkingMode) Icons.Rounded.Psychology else Icons.Outlined.Psychology,
                iconTint =
                    if (enableThinkingMode) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                isChecked = enableThinkingMode,
                onToggle = onToggleThinkingMode,
                onInfoClick = onThinkingModeInfoClick,
            )
            if (enableThinkingMode) {
                AgentThinkingSliderSettingItemBridge(
                    label = stringResource(R.string.thinking_quality),
                    value = thinkingQualityLevel,
                    onValueChange = onThinkingQualityLevelChange,
                    onInfoClick = onThinkingQualityInfoClick,
                )
            }
        }
    }
}

@Composable
private fun AgentThinkingSubSettingItemBridge(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    isChecked: Boolean,
    onToggle: () -> Unit,
    onInfoClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isChecked) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    } else {
                        Color.Transparent
                    },
                )
                .toggleable(
                    value = isChecked,
                    onValueChange = { onToggle() },
                    role = Role.Switch,
                )
                .heightIn(min = 36.dp)
                .padding(horizontal = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
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
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color =
                    if (isChecked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Switch(
                checked = isChecked,
                onCheckedChange = null,
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
}

@Composable
private fun AgentThinkingSliderSettingItemBridge(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    onInfoClick: () -> Unit,
) {
    var sliderValue by remember { mutableStateOf(value.toFloat()) }

    LaunchedEffect(value) {
        sliderValue = value.toFloat().coerceIn(1f, 4f)
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Speed,
                contentDescription = label,
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
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = sliderValue.roundToInt().coerceIn(1, 4).toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
                onValueChange(sliderValue.roundToInt().coerceIn(1, 4))
            },
            valueRange = 1f..4f,
            steps = 2,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AgentMaxContextSettingItemBridge(
    enableMaxContextMode: Boolean,
    onToggleEnableMaxContextMode: () -> Unit,
    onInfoClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .padding(horizontal = 12.dp)
                .toggleable(
                    value = enableMaxContextMode,
                    onValueChange = { onToggleEnableMaxContextMode() },
                    role = Role.Switch,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Whatshot,
            contentDescription = null,
            tint =
                if (enableMaxContextMode) {
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
            text = stringResource(R.string.max_mode_title),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = enableMaxContextMode,
            onCheckedChange = null,
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
private fun AgentModelSelectorItemBridge(
    currentModelLabel: String,
    availableModelLabels: List<String>,
    onSelectModelLabel: (String) -> Unit,
    onManageClick: () -> Unit,
    onInfoClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
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
        Text(
            text = stringResource(R.string.model) + ":",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = currentModelLabel.ifBlank { stringResource(R.string.not_selected) },
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (availableModelLabels.isEmpty()) {
            Text(
                text = stringResource(R.string.no_models_available),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        } else {
            availableModelLabels.forEachIndexed { index, label ->
                val isSelected = label == currentModelLabel
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                } else {
                                    Color.Transparent
                                },
                            )
                            .clickable { onSelectModelLabel(label) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (index < availableModelLabels.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
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
                text = stringResource(R.string.manage_config),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
            )
        }
    }
}
