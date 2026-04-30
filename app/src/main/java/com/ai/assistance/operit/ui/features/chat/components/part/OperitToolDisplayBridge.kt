package com.ai.assistance.operit.ui.features.chat.components.part

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.kiyori.R
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

private const val TOOL_PARAM_TOKEN_THRESHOLD = 50

@Composable
fun OperitToolCallDisplayBridge(
    toolName: String,
    params: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    enableDialog: Boolean = true,
) {
    val estimatedTokens = remember(params) { estimateTokenCount(params) }
    if (estimatedTokens > TOOL_PARAM_TOKEN_THRESHOLD) {
        DetailedToolDisplayBridge(
            toolName = toolName,
            params = params,
            textColor = textColor,
            modifier = modifier,
            enableDialog = enableDialog,
        )
    } else {
        CompactToolDisplayBridge(
            toolName = toolName,
            params = params,
            textColor = textColor,
            modifier = modifier,
            enableDialog = enableDialog,
        )
    }
}

@Composable
fun OperitToolResultDisplayBridge(
    toolName: String,
    result: String,
    isSuccess: Boolean,
    modifier: Modifier = Modifier,
    enableDialog: Boolean = true,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val hasContent = result.isNotBlank()
    var showDetailDialog by remember { mutableStateOf(false) }

    if (showDetailDialog && enableDialog) {
        ToolResultDetailDialogBridge(
            toolName = toolName,
            result = result,
            isSuccess = isSuccess,
            onDismiss = { showDetailDialog = false },
            onCopy = {
                clipboardManager.setText(AnnotatedString(result))
            },
        )
    }

    val summaryText =
        if (hasContent) {
            result.take(200)
        } else if (isSuccess) {
            context.getString(R.string.execution_success)
        } else {
            context.getString(R.string.execution_failed)
        }
    val semanticResultText =
        remember(result, hasContent) {
            if (!hasContent) {
                ""
            } else {
                result
                    .replace("\n", " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .let { normalized ->
                        if (normalized.length <= 20) normalized else normalized.take(20) + "..."
                    }
            }
        }
    val semanticDescription =
        remember(toolName, summaryText, semanticResultText, isSuccess, hasContent) {
            val resultLabel = context.getString(R.string.tool_execution_result)
            val statusLabel =
                if (isSuccess) context.getString(R.string.success) else context.getString(R.string.failed)
            if (hasContent && semanticResultText.isNotBlank()) {
                "$resultLabel: $toolName, $statusLabel, $semanticResultText"
            } else {
                "$resultLabel: $toolName, $statusLabel, $summaryText"
            }
        }

    CanvasToolResultRowBridge(
        summary = summaryText,
        isSuccess = isSuccess,
        semanticDescription = semanticDescription,
        modifier = modifier,
        emphasizeSummary = !hasContent,
        onClick =
            if (hasContent && enableDialog) {
                { showDetailDialog = true }
            } else {
                null
            },
        onCopyClick =
            if (hasContent) {
                {
                    clipboardManager.setText(AnnotatedString(result))
                }
            } else {
                null
            },
    )
}

@Composable
private fun CompactToolDisplayBridge(
    toolName: String,
    params: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    enableDialog: Boolean = true,
) {
    val context = LocalContext.current
    val (displayToolName, displayParams) = remember(toolName, params) {
        normalizeToolDisplayForStrictProxy(toolName, params)
    }
    var showDetailDialog by remember { mutableStateOf(false) }
    val hasParams = displayParams.isNotBlank()
    val semanticDescription =
        remember(displayToolName, displayParams.length) {
            buildToolSemanticDescription(
                context = context,
                toolName = displayToolName,
                params = displayParams,
                useByteSummary = false,
            )
        }

    if (showDetailDialog && hasParams && enableDialog) {
        ContentDetailDialogBridge(
            title = "$displayToolName ${context.getString(R.string.tool_call_parameters)}",
            content = displayParams,
            icon = getToolIcon(displayToolName),
            onDismiss = { showDetailDialog = false },
        )
    }

    val summary =
        remember(displayParams.length) {
            val firstParamRegex = "<param.*?>([^<]*)<\\/param>".toRegex()
            val match = firstParamRegex.find(displayParams)
            match?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
                ?: displayParams.replace("\n", " ").trim()
        }

    CanvasToolSummaryRowBridge(
        toolName = displayToolName,
        summary = summary,
        semanticDescription = semanticDescription,
        leadingIcon = getToolIcon(displayToolName),
        titleColor = MaterialTheme.colorScheme.primary,
        summaryColor = textColor.copy(alpha = 0.7f),
        modifier = modifier,
        onClick =
            if (hasParams && enableDialog) {
                { showDetailDialog = true }
            } else {
                null
            },
    )
}

@Composable
private fun DetailedToolDisplayBridge(
    toolName: String,
    params: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    enableDialog: Boolean = true,
) {
    val context = LocalContext.current
    val (displayToolName, displayParams) = remember(toolName, params) {
        normalizeToolDisplayForStrictProxy(toolName, params)
    }
    var showDetailDialog by remember { mutableStateOf(false) }
    val hasParams = displayParams.isNotBlank()
    val semanticDescription =
        remember(displayToolName, displayParams.length) {
            buildToolSemanticDescription(
                context = context,
                toolName = displayToolName,
                params = displayParams,
                useByteSummary = true,
            )
        }
    val paramsSizeLabel = remember(displayParams) { buildToolParamsSizeLabel(context, displayParams) }

    if (showDetailDialog && hasParams && enableDialog) {
        ContentDetailDialogBridge(
            title = "$displayToolName ${context.getString(R.string.tool_call_parameters)}",
            content = displayParams,
            icon = getToolIcon(displayToolName),
            onDismiss = { showDetailDialog = false },
        )
    }

    CanvasToolSummaryRowBridge(
        toolName = displayToolName,
        summary = paramsSizeLabel,
        semanticDescription = semanticDescription,
        leadingIcon = getToolIcon(displayToolName),
        titleColor = MaterialTheme.colorScheme.primary,
        summaryColor = textColor.copy(alpha = 0.7f),
        modifier = modifier,
        onClick =
            if (hasParams && enableDialog) {
                { showDetailDialog = true }
            } else {
                null
            },
    )
}

@Composable
private fun CanvasToolSummaryRowBridge(
    toolName: String,
    summary: String,
    semanticDescription: String,
    leadingIcon: ImageVector,
    titleColor: Color,
    summaryColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val iconPainter = rememberVectorPainter(leadingIcon)
    val titleStyle = MaterialTheme.typography.labelMedium.copy(color = titleColor)
    val summaryStyle = MaterialTheme.typography.bodySmall.copy(color = summaryColor)
    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier =
        if (onClick != null) {
            Modifier
                .semantics {
                    contentDescription = semanticDescription
                    role = Role.Button
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(),
                    role = Role.Button,
                    onClick = onClick,
                )
        } else {
            Modifier.semantics { contentDescription = semanticDescription }
        }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().then(clickableModifier)) {
        val widthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val topPaddingPx = with(density) { 4.dp.roundToPx() }
        val iconSizePx = with(density) { 16.dp.roundToPx().toFloat() }
        val gap1Px = with(density) { 8.dp.roundToPx().toFloat() }
        val gap2Px = with(density) { 8.dp.roundToPx().toFloat() }
        val titleMinWidthPx = with(density) { 80.dp.roundToPx() }
        val titleMaxWidthPx = with(density) { 120.dp.roundToPx() }

        val unconstrainedTitleLayout =
            remember(toolName, titleStyle, textMeasurer) {
                textMeasurer.measure(
                    text = AnnotatedString(toolName),
                    style = titleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        val titleWidthPx =
            unconstrainedTitleLayout.size.width
                .coerceAtLeast(titleMinWidthPx)
                .coerceAtMost(titleMaxWidthPx)
        val titleLayout =
            remember(toolName, titleStyle, textMeasurer, titleWidthPx) {
                textMeasurer.measure(
                    text = AnnotatedString(toolName),
                    style = titleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    constraints = Constraints(maxWidth = titleWidthPx),
                )
            }

        val summaryMaxWidth =
            (widthPx - iconSizePx.toInt() - gap1Px.toInt() - titleWidthPx - gap2Px.toInt())
                .coerceAtLeast(0)
        val summaryLayout =
            remember(summary, summaryStyle, textMeasurer, summaryMaxWidth) {
                textMeasurer.measure(
                    text = AnnotatedString(summary),
                    style = summaryStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    constraints = Constraints(maxWidth = summaryMaxWidth),
                )
            }

        val contentHeightPx =
            max(
                iconSizePx.toInt(),
                max(titleLayout.size.height, summaryLayout.size.height),
            )
        val totalHeightPx = topPaddingPx + contentHeightPx

        Canvas(modifier = Modifier.fillMaxWidth().height(with(density) { totalHeightPx.toDp() })) {
            val iconTop = topPaddingPx + (contentHeightPx - iconSizePx) / 2f
            translate(top = iconTop) {
                with(iconPainter) {
                    draw(
                        size = Size(iconSizePx, iconSizePx),
                        colorFilter = ColorFilter.tint(titleColor.copy(alpha = 0.7f)),
                    )
                }
            }

            val textTopBase = topPaddingPx.toFloat()
            val titleX = iconSizePx + gap1Px
            val titleY = textTopBase + (contentHeightPx - titleLayout.size.height) / 2f
            drawText(titleLayout, topLeft = Offset(titleX, titleY))

            val summaryX = titleX + titleWidthPx + gap2Px
            val summaryY = textTopBase + (contentHeightPx - summaryLayout.size.height) / 2f
            drawText(summaryLayout, topLeft = Offset(summaryX, summaryY))
        }
    }
}

@Composable
private fun CanvasToolResultRowBridge(
    summary: String,
    isSuccess: Boolean,
    semanticDescription: String,
    modifier: Modifier = Modifier,
    emphasizeSummary: Boolean = false,
    onClick: (() -> Unit)? = null,
    onCopyClick: (() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val arrowPainter = rememberVectorPainter(Icons.Default.SubdirectoryArrowRight)
    val statusPainter =
        rememberVectorPainter(
            if (isSuccess) {
                Icons.Default.Check
            } else {
                Icons.Default.Close
            },
        )
    val copyPainter = rememberVectorPainter(Icons.Default.ContentCopy)
    val summaryColor =
        if (isSuccess) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = if (emphasizeSummary) 0.9f else 0.8f)
        } else {
            MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        }
    val summaryStyle = MaterialTheme.typography.bodySmall.copy(color = summaryColor)
    val leadingTint =
        if (isSuccess) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        }
    val statusTint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val copyTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    val rowInteractionSource = remember { MutableInteractionSource() }
    val copyInteractionSource = remember { MutableInteractionSource() }
    val rowModifier =
        if (onClick != null) {
            Modifier
                .semantics {
                    contentDescription = semanticDescription
                    role = Role.Button
                }
                .indication(rowInteractionSource, rememberRipple())
                .clickable(
                    interactionSource = rowInteractionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                )
        } else {
            Modifier.semantics { contentDescription = semanticDescription }
        }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().then(rowModifier)) {
        val widthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val startPaddingPx = with(density) { 24.dp.roundToPx() }
        val endPaddingPx = with(density) { 16.dp.roundToPx() }
        val verticalPaddingPx = with(density) { 2.dp.roundToPx() }
        val arrowSizePx = with(density) { 18.dp.roundToPx().toFloat() }
        val statusSizePx = with(density) { 14.dp.roundToPx().toFloat() }
        val gapPx = with(density) { 8.dp.roundToPx().toFloat() }
        val trailingSlotPx = if (onCopyClick != null) with(density) { 24.dp.roundToPx() } else 0
        val trailingIconPx = with(density) { 14.dp.roundToPx().toFloat() }

        val summaryWidthPx =
            (widthPx - startPaddingPx - endPaddingPx - arrowSizePx.toInt() - gapPx.toInt() -
                statusSizePx.toInt() - gapPx.toInt() - trailingSlotPx)
                .coerceAtLeast(0)
        val summaryLayout =
            remember(summary, summaryStyle, textMeasurer, summaryWidthPx) {
                textMeasurer.measure(
                    text = AnnotatedString(summary),
                    style = summaryStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    constraints = Constraints(maxWidth = summaryWidthPx),
                )
            }

        val contentHeightPx =
            max(
                max(arrowSizePx.toInt(), statusSizePx.toInt()),
                max(summaryLayout.size.height, trailingSlotPx),
            )
        val totalHeightPx = verticalPaddingPx * 2 + contentHeightPx
        val trailingStartPx = widthPx - endPaddingPx - trailingSlotPx

        Box(modifier = Modifier.fillMaxWidth().height(with(density) { totalHeightPx.toDp() })) {
            Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                val contentTop = verticalPaddingPx.toFloat()
                val arrowTop = contentTop + (contentHeightPx - arrowSizePx) / 2f
                translate(left = startPaddingPx.toFloat(), top = arrowTop) {
                    with(arrowPainter) {
                        draw(
                            size = Size(arrowSizePx, arrowSizePx),
                            colorFilter = ColorFilter.tint(leadingTint),
                        )
                    }
                }

                val statusLeft = startPaddingPx + arrowSizePx + gapPx
                val statusTop = contentTop + (contentHeightPx - statusSizePx) / 2f
                translate(left = statusLeft, top = statusTop) {
                    with(statusPainter) {
                        draw(
                            size = Size(statusSizePx, statusSizePx),
                            colorFilter = ColorFilter.tint(statusTint),
                        )
                    }
                }

                val textX = statusLeft + statusSizePx + gapPx
                val textY = contentTop + (contentHeightPx - summaryLayout.size.height) / 2f
                drawText(summaryLayout, topLeft = Offset(textX, textY))

                if (onCopyClick != null) {
                    val copyLeft = widthPx - endPaddingPx - trailingSlotPx + (trailingSlotPx - trailingIconPx) / 2f
                    val copyTop = contentTop + (contentHeightPx - trailingIconPx) / 2f
                    translate(left = copyLeft, top = copyTop) {
                        with(copyPainter) {
                            draw(
                                size = Size(trailingIconPx, trailingIconPx),
                                colorFilter = ColorFilter.tint(copyTint),
                            )
                        }
                    }
                }
            }

            if (onCopyClick != null && trailingSlotPx > 0) {
                Box(
                    modifier =
                        Modifier
                            .offset(x = with(density) { trailingStartPx.toDp() })
                            .width(with(density) { trailingSlotPx.toDp() })
                            .fillMaxHeight()
                            .semantics {
                                contentDescription = "$semanticDescription, copy"
                                role = Role.Button
                            }
                            .indication(copyInteractionSource, rememberRipple(bounded = false))
                            .clickable(
                                interactionSource = copyInteractionSource,
                                indication = null,
                                role = Role.Button,
                                onClick = onCopyClick,
                            )
                )
            }
        }
    }
}

@Composable
private fun ContentDetailDialogBridge(
    title: String,
    content: String,
    icon: ImageVector,
    onDismiss: () -> Unit,
) {
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
                DialogTitleRowBridge(title = title, icon = icon)
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))
                CodeContentBoxBridge(content = content)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(text = LocalContext.current.getString(R.string.close))
                }
            }
        }
    }
}

@Composable
private fun ToolResultDetailDialogBridge(
    toolName: String,
    result: String,
    isSuccess: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
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
                        imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Close,
                        contentDescription =
                            if (isSuccess) context.getString(R.string.success) else context.getString(R.string.failed),
                        tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text =
                            "$toolName ${
                                if (isSuccess) {
                                    context.getString(R.string.execution_success)
                                } else {
                                    context.getString(R.string.execution_failed)
                                }
                            }",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onCopy) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = context.getString(R.string.copy_result),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))
                CodeContentBoxBridge(
                    content = result,
                    backgroundColor =
                        if (isSuccess) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        },
                )
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

@Composable
private fun DialogTitleRowBridge(
    title: String,
    icon: ImageVector,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
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
}

@Composable
private fun CodeContentBoxBridge(
    content: String,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp, max = 400.dp)
                .background(color = backgroundColor, shape = RoundedCornerShape(8.dp))
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
    ) {
        Text(
            text = content,
            style =
                MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            softWrap = true,
        )
    }
}

private fun getToolIcon(toolName: String): ImageVector {
    val name = toolName.lowercase()
    return when {
        name.contains("file") || name.contains("read") || name.contains("write") -> Icons.Default.FileOpen
        name.contains("search") || name.contains("find") || name.contains("query") -> Icons.Default.Search
        name.contains("terminal") ||
            name.contains("exec") ||
            name.contains("command") ||
            name.contains("shell") -> Icons.Default.Terminal
        name.contains("code") || name.contains("ffmpeg") -> Icons.Default.Code
        name.contains("http") || name.contains("web") || name.contains("visit") -> Icons.Default.Web
        else -> Icons.Default.ArrowForward
    }
}

private fun normalizeToolDisplayForStrictProxy(
    toolName: String,
    params: String,
): Pair<String, String> {
    if (toolName != "package_proxy") {
        return toolName to params
    }

    val toolNameRegex = "<param\\s+name=\"tool_name\">([\\s\\S]*?)<\\/param>".toRegex()
    val paramsRegex = "<param\\s+name=\"params\">([\\s\\S]*?)<\\/param>".toRegex()
    val rawTargetToolName = toolNameRegex.find(params)?.groupValues?.getOrNull(1)?.trim().orEmpty()
    val rawProxiedParams = paramsRegex.find(params)?.groupValues?.getOrNull(1)?.trim().orEmpty()

    val displayToolName = normalizeEscapedTextForDisplay(rawTargetToolName).ifBlank { toolName }
    val displayParams =
        if (rawProxiedParams.isNotBlank()) {
            parseProxyJsonParamsToXml(normalizeEscapedTextForDisplay(rawProxiedParams)) ?: params
        } else {
            params
        }
    return displayToolName to displayParams
}

private fun buildToolSemanticDescription(
    context: android.content.Context,
    toolName: String,
    params: String,
    useByteSummary: Boolean,
): String {
    val toolLabel = context.getString(R.string.tool_call)
    if (params.isBlank()) {
        return "$toolLabel: $toolName"
    }
    val paramsLabel = context.getString(R.string.tool_call_parameters)
    val summary =
        if (useByteSummary) {
            buildToolParamsSizeLabel(context, params)
        } else {
            buildParamsHeadPreview(params)
        }
    return "$toolLabel: $toolName, $paramsLabel: $summary"
}

private fun buildToolParamsSizeLabel(
    context: android.content.Context,
    params: String,
): String = context.getString(R.string.tool_call_param_bytes, calculateToolParamsBytes(params))

private fun calculateToolParamsBytes(params: String): Int {
    if (params.isBlank()) {
        return 0
    }
    val targetTexts = extractParamPayloadsForSize(params)
    return targetTexts.sumOf { it.toByteArray(Charsets.UTF_8).size }
}

private fun extractParamPayloadsForSize(params: String): List<String> {
    val tagRegex = "</?param\\b[^>]*>".toRegex()
    val payloads = mutableListOf<String>()
    var insideParam = false
    var valueStart = -1

    for (match in tagRegex.findAll(params)) {
        val tagText = match.value
        if (tagText.startsWith("</")) {
            if (insideParam) {
                val rawValue = params.substring(valueStart, match.range.first)
                payloads += normalizeEscapedTextForDisplay(rawValue)
                insideParam = false
                valueStart = -1
            }
            continue
        }

        if (!insideParam) {
            insideParam = true
            valueStart = match.range.last + 1
        }
    }

    if (insideParam && valueStart in 0..params.length) {
        payloads += normalizeEscapedTextForDisplay(params.substring(valueStart))
    }

    return if (payloads.isNotEmpty()) {
        payloads
    } else {
        listOf(normalizeEscapedTextForDisplay(params))
    }
}

private fun buildParamsHeadPreview(
    params: String,
    maxChars: Int = 120,
): String {
    val firstParamRegex = "<param.*?>([^<]*)<\\/param>".toRegex()
    val matched = firstParamRegex.find(params)?.groupValues?.get(1)?.trim()
    val cleaned =
        (matched?.takeIf { it.isNotEmpty() } ?: params)
            .replace("\n", " ")
            .trim()
    return if (cleaned.length <= maxChars) cleaned else cleaned.take(maxChars) + "..."
}

private fun estimateTokenCount(text: String): Int {
    var chineseCharCount = 0
    var otherCharCount = 0
    text.forEach { char ->
        if (char.code in 0x4E00..0x9FFF) {
            chineseCharCount++
        } else if (!char.isWhitespace()) {
            otherCharCount++
        }
    }
    return chineseCharCount + (otherCharCount / 4)
}

private fun normalizeEscapedTextForDisplay(input: String): String {
    val unescaped = unescapeXmlForDisplay(input).replace("\\\"", "\"")
    val trimmed = unescaped.trim()
    return if (
        (trimmed.startsWith("\"{") && trimmed.endsWith("}\"")) ||
        (trimmed.startsWith("\"[") && trimmed.endsWith("]\""))
    ) {
        trimmed.substring(1, trimmed.length - 1).replace("\\\"", "\"")
    } else {
        unescaped
    }
}

private fun unescapeXmlForDisplay(input: String): String {
    var result = input
    if (result.startsWith("<![CDATA[") && result.endsWith("]]>")) {
        result = result.substring(9, result.length - 3)
    }
    if (result.endsWith("]]>")) {
        result = result.substring(0, result.length - 3)
    }
    if (result.startsWith("<![CDATA[")) {
        result = result.substring(9)
    }
    return result
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
}

private fun parseProxyJsonParamsToXml(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) {
        return ""
    }

    return try {
        when {
            trimmed.startsWith("{") && trimmed.endsWith("}") -> {
                val obj = JSONObject(trimmed)
                val lines = mutableListOf<String>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val valueText = jsonValueToParamText(obj.opt(key))
                    lines.add("<param name=\"${escapeXmlAttribute(key)}\">${escapeXmlText(valueText)}</param>")
                }
                lines.joinToString("\n")
            }
            trimmed.startsWith("[") && trimmed.endsWith("]") -> {
                val array = JSONArray(trimmed)
                val lines = mutableListOf<String>()
                for (index in 0 until array.length()) {
                    val valueText = jsonValueToParamText(array.opt(index))
                    lines.add("<param name=\"$index\">${escapeXmlText(valueText)}</param>")
                }
                lines.joinToString("\n")
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

private fun jsonValueToParamText(value: Any?): String =
    when (value) {
        null, JSONObject.NULL -> "null"
        is String -> value
        else -> value.toString()
    }

private fun escapeXmlAttribute(input: String): String =
    input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

private fun escapeXmlText(input: String): String =
    input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
