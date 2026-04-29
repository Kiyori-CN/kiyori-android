package com.android.kiyori.operitreplica.ui.components

internal object ReplicaWorkspaceCodeFormatter {
    fun format(code: String, language: String): String {
        return try {
            when (language.lowercase()) {
                "javascript", "js" -> formatJavaScript(code)
                "css" -> formatCss(code)
                "html", "htm" -> formatHtml(code)
                else -> code
            }
        } catch (_: Exception) {
            code
        }
    }

    private fun formatJavaScript(code: String): String {
        val result = StringBuilder()
        var indentLevel = 0
        var index = 0
        var inString = false
        var stringChar = ' '
        var inComment = false
        var inMultiLineComment = false

        while (index < code.length) {
            val char = code[index]

            if (!inString && index + 1 < code.length && code.substring(index, index + 2) == "/*") {
                inMultiLineComment = true
                result.append("/*")
                index += 2
                continue
            }

            if (inMultiLineComment) {
                result.append(char)
                if (index + 1 < code.length && code.substring(index, index + 2) == "*/") {
                    result.append('/')
                    inMultiLineComment = false
                    index += 2
                    continue
                }
                index++
                continue
            }

            if (!inString && index + 1 < code.length && code.substring(index, index + 2) == "//") {
                inComment = true
                result.append("//")
                index += 2
                continue
            }

            if (inComment) {
                result.append(char)
                if (char == '\n') {
                    inComment = false
                }
                index++
                continue
            }

            if (char == '"' || char == '\'' || char == '`') {
                if (!inString) {
                    inString = true
                    stringChar = char
                } else if (char == stringChar && (index == 0 || code[index - 1] != '\\')) {
                    inString = false
                }
                result.append(char)
                index++
                continue
            }

            if (inString) {
                result.append(char)
                index++
                continue
            }

            when (char) {
                '{', '[' -> {
                    result.append(char)
                    var nextNonWhitespace = index + 1
                    while (nextNonWhitespace < code.length && code[nextNonWhitespace] in listOf(' ', '\t', '\n', '\r')) {
                        nextNonWhitespace++
                    }
                    val isEmptyBracket =
                        nextNonWhitespace < code.length &&
                            (
                                (char == '{' && code[nextNonWhitespace] == '}') ||
                                    (char == '[' && code[nextNonWhitespace] == ']')
                                )
                    if (!isEmptyBracket) {
                        indentLevel++
                        if (index + 1 < code.length && code[index + 1] != '\n') {
                            result.append('\n')
                        }
                    }
                }

                '}', ']' -> {
                    var lastNonWhitespace = result.length - 1
                    while (lastNonWhitespace >= 0 && result[lastNonWhitespace] in listOf(' ', '\t', '\n', '\r')) {
                        lastNonWhitespace--
                    }

                    val isEmptyBracket =
                        lastNonWhitespace >= 0 &&
                            (
                                (char == '}' && result[lastNonWhitespace] == '{') ||
                                    (char == ']' && result[lastNonWhitespace] == '[')
                                )

                    if (!isEmptyBracket) {
                        indentLevel = maxOf(0, indentLevel - 1)
                        if (result.isNotEmpty() && result.last() != '\n') {
                            result.append('\n')
                        }
                        while (result.length > 1 && result[result.length - 2] == ' ') {
                            result.setLength(result.length - 1)
                            result.append('\n')
                        }
                        result.append("    ".repeat(indentLevel))
                    } else {
                        while (result.isNotEmpty() && result.last() in listOf(' ', '\t', '\n', '\r')) {
                            result.setLength(result.length - 1)
                        }
                    }
                    result.append(char)
                }

                ';' -> {
                    result.append(char)
                    if (index + 1 < code.length && code[index + 1] != '\n') {
                        result.append('\n')
                    }
                }

                '\n', '\r' -> {
                    if (result.isNotEmpty() && result.last() != '\n') {
                        result.append('\n')
                    }
                }

                ' ', '\t' -> {
                    if (result.isNotEmpty() && result.last() != ' ' && result.last() != '\n') {
                        result.append(' ')
                    }
                }

                else -> {
                    if (result.isNotEmpty() && result.last() == '\n') {
                        result.append("    ".repeat(indentLevel))
                    }
                    result.append(char)
                }
            }

            index++
        }

        return result.toString().trim()
    }

    private fun formatCss(code: String): String {
        val result = StringBuilder()
        var indentLevel = 0
        var index = 0
        var inComment = false

        while (index < code.length) {
            val char = code[index]

            if (!inComment && index + 1 < code.length && code.substring(index, index + 2) == "/*") {
                inComment = true
                result.append("/*")
                index += 2
                continue
            }

            if (inComment) {
                result.append(char)
                if (index + 1 < code.length && code.substring(index, index + 2) == "*/") {
                    result.append('/')
                    inComment = false
                    index += 2
                    continue
                }
                index++
                continue
            }

            when (char) {
                '{' -> {
                    result.append(" {")
                    indentLevel++
                    result.append('\n')
                    result.append("    ".repeat(indentLevel))
                }

                '}' -> {
                    indentLevel = maxOf(0, indentLevel - 1)
                    while (result.isNotEmpty() && (result.last() == ' ' || result.last() == '\t')) {
                        result.setLength(result.length - 1)
                    }
                    if (result.isNotEmpty() && result.last() != '\n') {
                        result.append('\n')
                    }
                    result.append("    ".repeat(indentLevel))
                    result.append('}')
                    result.append('\n')
                    result.append("    ".repeat(indentLevel))
                }

                ';' -> {
                    result.append(';')
                    result.append('\n')
                    result.append("    ".repeat(indentLevel))
                }

                ':' -> result.append(": ")
                '\n', '\r' -> Unit
                ' ', '\t' -> {
                    if (result.isNotEmpty() && result.last() != ' ' && result.last() != '\n' && result.last() != ':') {
                        result.append(' ')
                    }
                }

                else -> result.append(char)
            }

            index++
        }

        return result.toString().trim()
    }

    private fun formatHtml(code: String): String {
        val result = StringBuilder()
        var indentLevel = 0
        var index = 0
        var inTag = false
        var inComment = false
        var inScript = false
        var inStyle = false
        var inString = false
        var stringChar = ' '

        val selfClosingTags =
            setOf(
                "br",
                "img",
                "input",
                "hr",
                "meta",
                "link",
                "area",
                "base",
                "col",
                "embed",
                "param",
                "source",
                "track",
                "wbr",
            )
        val inlineTags =
            setOf(
                "span",
                "a",
                "strong",
                "em",
                "b",
                "i",
                "u",
                "small",
                "code",
                "kbd",
                "var",
                "samp",
                "sub",
                "sup",
                "mark",
                "del",
                "ins",
                "abbr",
                "cite",
                "dfn",
                "q",
                "time",
            )

        while (index < code.length) {
            val char = code[index]

            if (inTag && (char == '"' || char == '\'' || char == '`')) {
                if (!inString) {
                    inString = true
                    stringChar = char
                } else if (char == stringChar && (index == 0 || code[index - 1] != '\\')) {
                    inString = false
                }
                result.append(char)
                index++
                continue
            }

            if (inString) {
                result.append(char)
                index++
                continue
            }

            if (!inComment && !inScript && !inStyle && index + 3 < code.length && code.substring(index, index + 4) == "<!--") {
                inComment = true
                if (result.isNotEmpty() && result.last() != '\n') {
                    result.append('\n')
                }
                result.append("    ".repeat(indentLevel))
                result.append("<!--")
                index += 4
                continue
            }

            if (inComment) {
                result.append(char)
                if (index + 2 < code.length && code.substring(index, index + 3) == "-->") {
                    result.append("->")
                    inComment = false
                    index += 3
                    continue
                }
                index++
                continue
            }

            if (inScript) {
                result.append(char)
                if (char == '<' && index + 8 < code.length && code.substring(index, index + 9).lowercase() == "</script>") {
                    inScript = false
                }
                index++
                continue
            }

            if (inStyle) {
                result.append(char)
                if (char == '<' && index + 7 < code.length && code.substring(index, index + 8).lowercase() == "</style>") {
                    inStyle = false
                }
                index++
                continue
            }

            if (!inTag && char == '<' && index + 8 < code.length && code.substring(index, index + 9).lowercase() == "<!doctype") {
                val endPos = code.indexOf('>', index)
                if (endPos != -1) {
                    result.append(code.substring(index, endPos + 1))
                    result.append('\n')
                    index = endPos + 1
                    continue
                }
            }

            if (char == '<') {
                inTag = true
                val isClosingTag = index + 1 < code.length && code[index + 1] == '/'
                var tagEnd = index + 1
                if (isClosingTag) {
                    tagEnd++
                }
                while (tagEnd < code.length && code[tagEnd] != ' ' && code[tagEnd] != '>' && code[tagEnd] != '\n') {
                    tagEnd++
                }
                val tagName = code.substring(if (isClosingTag) index + 2 else index + 1, tagEnd).lowercase()

                if (isClosingTag && tagName !in inlineTags) {
                    indentLevel = maxOf(0, indentLevel - 1)
                }

                if (tagName !in inlineTags) {
                    if (result.isNotEmpty() && result.last() != '\n') {
                        result.append('\n')
                    }
                    result.append("    ".repeat(indentLevel))
                }
                result.append(char)

                if (!isClosingTag && tagName == "script") {
                    inScript = true
                } else if (!isClosingTag && tagName == "style") {
                    inStyle = true
                }
            } else if (char == '>') {
                result.append(char)
                val tagStart = result.lastIndexOf('<')
                if (tagStart != -1 && inTag) {
                    val tagContent = result.substring(tagStart + 1, result.length - 1).trim()
                    val isClosingTag = tagContent.startsWith('/')
                    val isSelfClosing = tagContent.endsWith('/') || result[result.length - 2] == '/'
                    var tagNameEnd = 0
                    for (innerIndex in (if (isClosingTag) 1 else 0) until tagContent.length) {
                        if (tagContent[innerIndex] in listOf(' ', '/', '>')) {
                            tagNameEnd = innerIndex
                            break
                        }
                    }
                    if (tagNameEnd == 0) {
                        tagNameEnd = tagContent.length
                    }
                    val tagName = tagContent.substring(if (isClosingTag) 1 else 0, tagNameEnd).lowercase()
                    if (!isClosingTag && !isSelfClosing && tagName !in selfClosingTags && tagName !in inlineTags) {
                        indentLevel++
                    }
                }
                inTag = false
            } else if (inTag) {
                result.append(char)
            } else if (char !in listOf('\n', '\r', '\t')) {
                if (char == ' ') {
                    if (result.isNotEmpty() && result.last() != ' ' && result.last() != '\n') {
                        result.append(char)
                    }
                } else {
                    result.append(char)
                }
            }

            index++
        }

        return result.toString().trim()
    }
}
