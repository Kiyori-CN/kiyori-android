package com.android.kiyori.download

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "internal_downloads",
    indices = [
        Index(value = ["systemDownloadId"], unique = true),
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class InternalDownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val systemDownloadId: Long,
    val title: String,
    val fileName: String,
    val url: String,
    val mimeType: String,
    val description: String,
    val sourcePageUrl: String,
    val sourcePageTitle: String,
    val headersJson: String,
    val status: String,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val localUri: String = "",
    val localPath: String = "",
    val mediaType: String = "",
    val reason: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long = 0
) {
    val progressPercent: Int
        get() = if (totalBytes > 0L) {
            ((downloadedBytes.coerceAtLeast(0L) * 100L) / totalBytes).toInt().coerceIn(0, 100)
        } else {
            0
        }
}

object InternalDownloadStatus {
    const val PENDING = "pending"
    const val RUNNING = "running"
    const val PAUSED = "paused"
    const val SUCCESS = "success"
    const val FAILED = "failed"
    const val CANCELLED = "cancelled"
    const val UNKNOWN = "unknown"

    fun isCompleted(status: String): Boolean {
        return status == SUCCESS
    }

    fun isFailed(status: String): Boolean {
        return status == FAILED
    }

    fun isActive(status: String): Boolean {
        return status == PENDING || status == RUNNING || status == PAUSED
    }

    fun isIncomplete(status: String): Boolean {
        return status == PENDING || status == RUNNING || status == PAUSED || status == UNKNOWN
    }
}

data class InternalDownloadRequest(
    val url: String,
    val title: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val description: String = "",
    val sourcePageUrl: String = "",
    val sourcePageTitle: String = "",
    val mediaType: String = "",
    val headers: Map<String, String> = emptyMap()
)
