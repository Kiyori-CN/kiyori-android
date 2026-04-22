package com.android.kiyori.media.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 视频文件缓存实体
 * 用于缓存扫描结果，避免每次都查询MediaStore
 * 
 * 索引优化说明：
 * - folderName: 用于 getAllVideos() 的排序优化
 * - folderPath, nameSortKey: 复合索引，优化自然排序查询
 * - folderPath, dateAdded: 复合索引，优化日期排序查询
 * - lastScanned: 用于 deleteOldEntries() 的条件过滤
 */
@Entity(
    tableName = "video_cache",
    indices = [
        Index(value = ["folderName"]),
        Index(value = ["folderPath", "nameSortKey"]),
        Index(value = ["folderPath", "dateAdded"]),
        Index(value = ["lastScanned"])
    ]
)
data class VideoCacheEntity(
    @PrimaryKey
    val uri: String,
    val name: String,
    val nameSortKey: String,  // 用于自然排序的key，将数字补零以支持自然排序
    val path: String,
    val folderPath: String,
    val folderName: String,
    val size: Long,
    val duration: Long,
    val dateModified: Long,
    val dateAdded: Long,
    val lastScanned: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * 生成自然排序的key
         * 将文件名中的数字补零，使得字典序排序等同于自然排序
         * 例如："第1集.mp4" -> "第00000001集.mp4"
         */
        fun generateSortKey(name: String): String {
            val result = StringBuilder()
            val lowerName = name.lowercase()
            var i = 0
            
            while (i < lowerName.length) {
                val c = lowerName[i]
                
                if (c.isDigit()) {
                    // 提取完整的数字
                    var num = 0
                    while (i < lowerName.length && lowerName[i].isDigit()) {
                        num = num * 10 + (lowerName[i] - '0')
                        i++
                    }
                    // 将数字格式化为固定长度（8位，支持最大99999999）
                    result.append(String.format("%08d", num))
                } else {
                    result.append(c)
                    i++
                }
            }
            
            return result.toString()
        }
    }
}

