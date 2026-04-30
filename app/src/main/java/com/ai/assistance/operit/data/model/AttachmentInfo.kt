package com.ai.assistance.operit.data.model

data class AttachmentInfo(
    val filePath: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val content: String = "",
)
