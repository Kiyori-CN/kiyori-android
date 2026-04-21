package com.android.kiyori.media

data class VideoFolder(
    val folderPath: String,
    val folderName: String,
    val videoCount: Int,
    val videos: List<VideoFile>
)

data class VideoFile(
    val uri: String,
    val name: String,
    val path: String,
    val size: Long,
    val duration: Long,
    val dateAdded: Long
)

