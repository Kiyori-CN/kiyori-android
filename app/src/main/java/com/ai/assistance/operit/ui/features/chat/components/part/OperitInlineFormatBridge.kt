package com.ai.assistance.operit.ui.features.chat.components.part

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OperitFontTagBridge(
    xmlContent: String,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val innerText = extractXmlContent(xmlContent, "font").trim()
    if (innerText.isBlank()) {
        return
    }

    val colorAttr = extractXmlAttribute(xmlContent, "color")
    val sizeAttr = extractXmlAttribute(xmlContent, "size")
    val faceAttr = extractXmlAttribute(xmlContent, "face")
    val styleAttr = extractXmlAttribute(xmlContent, "style")
    val bgColorAttr = extractXmlAttribute(xmlContent, "bgcolor")

    val baseStyle = MaterialTheme.typography.bodyMedium
    val (weight, fontStyle, decoration) = parseFontStyle(styleAttr)
    val backgroundColor = parseFontColor(bgColorAttr, Color.Transparent)
    val resolvedStyle =
        baseStyle.copy(
            fontSize = parseFontSize(sizeAttr) ?: baseStyle.fontSize,
            fontFamily = parseFontFamily(faceAttr) ?: baseStyle.fontFamily,
            fontWeight = weight ?: baseStyle.fontWeight,
            fontStyle = fontStyle ?: baseStyle.fontStyle,
            textDecoration = decoration ?: baseStyle.textDecoration,
        )

    SelectionContainer {
        Text(
            text = innerText,
            color = parseFontColor(colorAttr, textColor),
            style = resolvedStyle,
            modifier =
                modifier
                    .then(
                        if (backgroundColor != Color.Transparent) {
                            Modifier
                                .background(backgroundColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 2.dp)
                        } else {
                            Modifier
                        },
                    ),
        )
    }
}

@Composable
fun OperitDetailsTagBridge(
    xmlContent: String,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val tagName = extractXmlTagName(xmlContent) ?: "details"
    val innerContent = extractXmlContent(xmlContent, tagName)
    val summary = extractDetailsSummary(innerContent)
    val body = removeDetailsSummary(innerContent).trim()
    val defaultExpanded = hasOpenAttribute(xmlContent, tagName)
    var expanded by remember(xmlContent) { mutableStateOf(defaultExpanded) }
    val rotation by
        animateFloatAsState(
            targetValue = if (expanded) 90f else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "detailsArrowRotation",
        )

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        SourceStyleExpandableHeader(
            title = summary.ifBlank { "Details" },
            expanded = expanded,
            rotationDegrees = rotation,
            titleColor = textColor.copy(alpha = 0.85f),
            onClick = { expanded = !expanded },
        )

        AnimatedVisibility(
            visible = expanded && body.isNotBlank(),
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp, start = 24.dp),
            ) {
                ChatMarkupRendererBridge(
                    content = body,
                    textColor = textColor.copy(alpha = 0.85f),
                    renderInlineTextOnly = true,
                )
            }
        }
    }
}

fun shouldHideOperitMetaBridge(blockRaw: String, tagName: String): Boolean =
    tagName == "meta" &&
        Regex("""\bprovider\s*=\s*["']gemini:thought_signature["']""", RegexOption.IGNORE_CASE)
            .containsMatchIn(blockRaw)

private fun extractXmlTagName(content: String): String? =
    Regex("<\\s*([a-zA-Z_][a-zA-Z0-9_]*)")
        .find(content)
        ?.groupValues
        ?.getOrNull(1)
        ?.lowercase()

private fun extractXmlContent(content: String, tagName: String): String {
    val startTag = "<$tagName"
    val startTagIndex = content.indexOf(startTag, ignoreCase = true)
    if (startTagIndex < 0) {
        return content
    }
    val startTagEnd = content.indexOf('>', startTagIndex)
    if (startTagEnd < 0) {
        return content
    }
    val endTag = "</$tagName>"
    val endIndex = content.lastIndexOf(endTag, ignoreCase = true)
    return if (endIndex > startTagEnd) {
        content.substring(startTagEnd + 1, endIndex)
    } else {
        content.substring(startTagEnd + 1)
    }
}

private fun extractXmlAttribute(content: String, attributeName: String): String? {
    val regex = ("\\b" + Regex.escape(attributeName) + "\\s*=\\s*([\"'])(.*?)\\1").toRegex()
    return regex.find(content)?.groupValues?.getOrNull(2)
}

private fun parseFontColor(value: String?, fallback: Color): Color {
    if (value.isNullOrBlank()) {
        return fallback
    }
    return try {
        Color(android.graphics.Color.parseColor(value.trim()))
    } catch (_: Exception) {
        fallback
    }
}

private fun parseFontSize(value: String?): TextUnit? {
    if (value.isNullOrBlank()) {
        return null
    }
    val raw = value.trim().lowercase()
    val htmlSize = raw.toIntOrNull()
    if (htmlSize != null && htmlSize in 1..7) {
        return when (htmlSize) {
            1 -> 10.sp
            2 -> 12.sp
            3 -> 14.sp
            4 -> 16.sp
            5 -> 18.sp
            6 -> 20.sp
            else -> 24.sp
        }
    }

    val number =
        raw
            .removeSuffix("sp")
            .removeSuffix("px")
            .removeSuffix("dp")
            .toFloatOrNull()
            ?: return null
    return number.sp
}

private fun parseFontFamily(value: String?): FontFamily? {
    if (value.isNullOrBlank()) {
        return null
    }
    return when (value.trim().lowercase()) {
        "monospace", "mono" -> FontFamily.Monospace
        "serif" -> FontFamily.Serif
        "sans-serif", "sansserif", "sans" -> FontFamily.SansSerif
        "cursive" -> FontFamily.Cursive
        else -> null
    }
}

private fun parseFontStyle(value: String?): Triple<FontWeight?, FontStyle?, TextDecoration?> {
    if (value.isNullOrBlank()) {
        return Triple(null, null, null)
    }
    val style = value.lowercase()
    val weight = if (style.contains("bold")) FontWeight.Bold else null
    val fontStyle = if (style.contains("italic")) FontStyle.Italic else null
    val decorations = mutableListOf<TextDecoration>()
    if (style.contains("underline")) {
        decorations += TextDecoration.Underline
    }
    if (style.contains("line-through") || style.contains("strikethrough")) {
        decorations += TextDecoration.LineThrough
    }
    val decoration =
        when (decorations.size) {
            0 -> null
            1 -> decorations.first()
            else -> TextDecoration.combine(decorations)
        }
    return Triple(weight, fontStyle, decoration)
}

private fun extractDetailsSummary(detailsInner: String): String {
    val regex = "<summary>(.*?)</summary>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    return regex.find(detailsInner)?.groupValues?.getOrNull(1)?.trim().orEmpty()
}

private fun removeDetailsSummary(detailsInner: String): String {
    val regex = "<summary>.*?</summary>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    return detailsInner.replaceFirst(regex, "")
}

private fun hasOpenAttribute(detailsXml: String, tagName: String): Boolean {
    val openRegex = ("<" + Regex.escape(tagName) + "\\b[^>]*\\bopen\\b").toRegex(RegexOption.IGNORE_CASE)
    return openRegex.containsMatchIn(detailsXml)
}
