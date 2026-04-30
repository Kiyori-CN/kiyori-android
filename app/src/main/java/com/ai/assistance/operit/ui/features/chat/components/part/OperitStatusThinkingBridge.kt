package com.ai.assistance.operit.ui.features.chat.components.part

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.kiyori.R

@Composable
fun OperitThinkingProcessBridge(
    content: String,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    ExpandableXmlBlockBridge(
        title = stringResource(R.string.thinking_process),
        content = content,
        textColor = textColor.copy(alpha = 0.6f),
        modifier = modifier,
        initiallyExpanded = false,
        maxBodyHeight = 300.dp,
    )
}

@Composable
fun OperitSearchSourcesBridge(
    content: String,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    ExpandableXmlBlockBridge(
        title = stringResource(R.string.search_sources),
        content = content,
        textColor = textColor.copy(alpha = 0.8f),
        modifier = modifier.padding(vertical = 4.dp),
        initiallyExpanded = false,
        maxBodyHeight = 240.dp,
    )
}

@Composable
fun OperitStatusBridge(
    statusType: String,
    content: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    enableDialog: Boolean = true,
) {
    val normalizedType = statusType.lowercase()
    val statusText =
        when (normalizedType) {
            "completion", "complete" -> "\u2713 Task completed"
            "wait_for_user_need" -> "\u2713 Ready for further assistance"
            else -> content
        }

    if (normalizedType == "warning") {
        WarningStatusBridge(
            summaryText = stringResource(R.string.status_warning_ai_error_summary),
            detailText = content,
            modifier = modifier,
            enableDialog = enableDialog,
        )
        return
    }

    val backgroundColor =
        when (normalizedType) {
            "completion", "complete" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            "wait_for_user_need" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        }
    val borderColor =
        when (normalizedType) {
            "completion", "complete" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            "wait_for_user_need" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        }
    val resolvedTextColor =
        when (normalizedType) {
            "completion", "complete" -> MaterialTheme.colorScheme.primary
            "wait_for_user_need" -> MaterialTheme.colorScheme.tertiary
            else -> textColor
        }

    if (statusText.isBlank()) {
        return
    }

    Surface(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        SelectionContainer {
            Text(
                text = statusText,
                color = resolvedTextColor,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun ExpandableXmlBlockBridge(
    title: String,
    content: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean,
    maxBodyHeight: androidx.compose.ui.unit.Dp,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "xmlBlockArrowRotation",
    )
    val cleanContent = content.trim()

    Column(modifier = modifier.fillMaxWidth()) {
        SourceStyleExpandableHeader(
            title = title,
            expanded = expanded,
            rotationDegrees = rotation,
            titleColor = textColor.copy(alpha = 0.86f),
            onClick = { expanded = !expanded },
        )

        AnimatedVisibility(
            visible = expanded && cleanContent.isNotBlank(),
            enter = expandVertically(animationSpec = tween(durationMillis = 220), expandFrom = Alignment.Top) +
                fadeIn(animationSpec = tween(durationMillis = 220)),
            exit = shrinkVertically(animationSpec = tween(durationMillis = 220), shrinkTowards = Alignment.Top) +
                fadeOut(animationSpec = tween(durationMillis = 220)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp)
                        .animateContentSize(animationSpec = tween(durationMillis = 240))
                        .heightIn(max = maxBodyHeight),
            ) {
                Box(
                    modifier =
                        Modifier
                            .padding(start = 8.dp)
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                )
                SelectionContainer {
                    Text(
                        text = cleanContent,
                        color = textColor,
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Default,
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(start = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun SourceStyleExpandableHeader(
    title: String,
    expanded: Boolean,
    rotationDegrees: Float,
    titleColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = title
                    role = Role.Button
                }
                .clickable(onClick = onClick)
                .padding(vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription =
                if (expanded) {
                    stringResource(R.string.common_collapse)
                } else {
                    stringResource(R.string.common_expand)
                },
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp).rotate(rotationDegrees),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            color = titleColor,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WarningStatusBridge(
    summaryText: String,
    detailText: String,
    modifier: Modifier,
    enableDialog: Boolean,
) {
    val canOpenDetail = enableDialog && detailText.isNotBlank()
    var showDetailDialog by remember { mutableStateOf(false) }

    if (showDetailDialog && canOpenDetail) {
        WarningStatusDetailDialog(
            title = stringResource(R.string.status_warning_ai_error_detail_title),
            detailText = detailText,
            onDismiss = { showDetailDialog = false },
        )
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (canOpenDetail) {
                        Modifier.clickable { showDetailDialog = true }
                    } else {
                        Modifier
                    },
                )
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(2.dp)
                    .height(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(999.dp),
                    ),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = summaryText,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WarningStatusDetailDialog(
    title: String,
    detailText: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp, max = 320.dp)
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                ) {
                    Text(
                        text = detailText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(text = context.getString(R.string.close))
                }
            }
        }
    }
}
