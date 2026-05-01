package com.android.kiyori.download

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val bilibiliDownloadGson = Gson()
private val downloadFragmentStateListType = object : TypeToken<List<DownloadFragmentState>>() {}.type

@Entity(
    tableName = "bilibili_downloads",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class BilibiliDownloadEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val url: String,
    val status: String,
    val progress: Int,
    val filePath: String?,
    val errorMessage: String?,
    val mediaType: String,
    val totalSize: Long,
    val downloadedSize: Long,
    val fragmentsJson: String,
    val aid: String,
    val cid: String,
    val epId: String?,
    val seasonId: String?,
    val createdAt: Long,
    val updatedAt: Long
)

fun BilibiliDownloadEntity.toDownloadItem(): DownloadItem {
    return DownloadItem(
        id = id,
        title = title,
        url = url,
        status = status,
        progress = progress,
        filePath = filePath,
        errorMessage = errorMessage,
        mediaType = runCatching { MediaType.valueOf(mediaType) }.getOrDefault(MediaType.Video),
        totalSize = totalSize,
        downloadedSize = downloadedSize,
        fragments = decodeDownloadFragments(fragmentsJson),
        aid = aid,
        cid = cid,
        epId = epId,
        seasonId = seasonId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun DownloadItem.toBilibiliDownloadEntity(): BilibiliDownloadEntity {
    return BilibiliDownloadEntity(
        id = id,
        title = title,
        url = url,
        status = status,
        progress = progress,
        filePath = filePath,
        errorMessage = errorMessage,
        mediaType = mediaType.name,
        totalSize = totalSize,
        downloadedSize = downloadedSize,
        fragmentsJson = encodeDownloadFragments(fragments),
        aid = aid,
        cid = cid,
        epId = epId,
        seasonId = seasonId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun encodeDownloadFragments(fragments: List<DownloadFragmentState>): String {
    return bilibiliDownloadGson.toJson(fragments)
}

private fun decodeDownloadFragments(json: String): List<DownloadFragmentState> {
    if (json.isBlank()) {
        return emptyList()
    }
    return runCatching {
        bilibiliDownloadGson.fromJson<List<DownloadFragmentState>>(
            json,
            downloadFragmentStateListType
        )
    }.getOrDefault(emptyList())
}
