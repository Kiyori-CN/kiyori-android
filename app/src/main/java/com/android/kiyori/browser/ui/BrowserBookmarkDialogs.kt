package com.android.kiyori.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.android.kiyori.browser.domain.BrowserBookmarkFolderOption

data class BrowserBookmarkDraft(
    val title: String,
    val url: String,
    val iconUrl: String,
    val folderId: Long?
)

@Composable
fun BrowserAddBookmarkDialog(
    title: String,
    initialDraft: BrowserBookmarkDraft,
    folderOptions: List<BrowserBookmarkFolderOption>,
    onDismiss: () -> Unit,
    onConfirm: (BrowserBookmarkDraft) -> Unit
) {
    var bookmarkTitle by remember(initialDraft) { mutableStateOf(initialDraft.title) }
    var bookmarkUrl by remember(initialDraft) { mutableStateOf(initialDraft.url) }
    var bookmarkIconUrl by remember(initialDraft) { mutableStateOf(initialDraft.iconUrl) }
    var selectedFolderId by remember(initialDraft) { mutableStateOf(initialDraft.folderId) }
    var folderMenuExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(min = 308.dp, max = 308.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
            ) {
                Text(
                    text = title,
                    color = Color(0xFF1F2937),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
                BrowserDialogTextField(
                    value = bookmarkTitle,
                    onValueChange = { bookmarkTitle = it },
                    placeholder = "请输入书签标题",
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
                BrowserDialogTextField(
                    value = bookmarkUrl,
                    onValueChange = { bookmarkUrl = it },
                    placeholder = "请输入网页地址",
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
                BrowserDialogTextField(
                    value = bookmarkIconUrl,
                    onValueChange = { bookmarkIconUrl = it },
                    placeholder = "请输入图标地址",
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                    BrowserDialogDropdownField(
                        value = folderOptions.firstOrNull { it.id == selectedFolderId }?.title ?: "/",
                        onClick = { folderMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = folderMenuExpanded,
                        onDismissRequest = { folderMenuExpanded = false },
                        modifier = Modifier
                            .widthIn(min = 272.dp, max = 272.dp)
                            .background(Color.White, RoundedCornerShape(16.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "/",
                                    color = Color(0xFF111827),
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                selectedFolderId = null
                                folderMenuExpanded = false
                            }
                        )
                        folderOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.title,
                                        color = Color(0xFF111827),
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    selectedFolderId = option.id
                                    folderMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Color(0xFFF3F4F6))
                        .height(58.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrowserDialogActionButton(
                        text = "取消",
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6))
                            .size(width = 0.5.dp, height = 58.dp)
                    )
                    BrowserDialogActionButton(
                        text = "确认",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onConfirm(
                                BrowserBookmarkDraft(
                                    title = bookmarkTitle.trim(),
                                    url = bookmarkUrl.trim(),
                                    iconUrl = bookmarkIconUrl.trim(),
                                    folderId = selectedFolderId
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BrowserSingleInputDialog(
    title: String,
    initialValue: String,
    confirmText: String = "确定",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(min = 308.dp, max = 308.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                Text(
                    text = title,
                    color = Color(0xFF1F2937),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
                BrowserDialogTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = "",
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Color(0xFFF3F4F6))
                        .height(58.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrowserDialogActionButton(
                        text = "取消",
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6))
                            .size(width = 0.5.dp, height = 58.dp)
                    )
                    BrowserDialogActionButton(
                        text = confirmText,
                        modifier = Modifier.weight(1f),
                        onClick = { onConfirm(value.trim()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowserDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp),
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0xFF111827),
                fontSize = 14.sp
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isBlank() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color(0xFFBDBDBD),
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFBDBDBD))
        )
    }
}

@Composable
private fun BrowserDialogDropdownField(
    value: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp)
                .padding(top = 2.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                color = Color(0xFF111827),
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "选择文件夹",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(18.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFBDBDBD))
        )
    }
}

@Composable
private fun BrowserDialogActionButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(58.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF374151),
            fontSize = 16.sp
        )
    }
}
