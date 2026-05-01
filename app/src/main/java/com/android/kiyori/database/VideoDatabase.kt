package com.android.kiyori.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.android.kiyori.download.InternalDownloadDao
import com.android.kiyori.download.InternalDownloadEntity
import com.android.kiyori.download.BilibiliDownloadDao
import com.android.kiyori.download.BilibiliDownloadEntity
import com.android.kiyori.history.data.PlaybackHistoryDao
import com.android.kiyori.history.data.PlaybackHistoryEntity
import com.android.kiyori.media.data.VideoCacheDao
import com.android.kiyori.media.data.VideoCacheEntity

/**
 * 视频数据库
 * 
 * Schema 导出配置：
 * - exportSchema = true：导出数据库结构到 app/schemas/ 目录
 * - 每次修改数据库结构时，Room 会自动生成对应版本的 JSON schema 文件
 * - 这些文件应该提交到版本控制，方便追踪数据库变更历史
 */
@Database(
    entities = [
        VideoCacheEntity::class,
        PlaybackHistoryEntity::class,
        InternalDownloadEntity::class,
        BilibiliDownloadEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class VideoDatabase : RoomDatabase() {
    
    abstract fun videoCacheDao(): VideoCacheDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun internalDownloadDao(): InternalDownloadDao
    abstract fun bilibiliDownloadDao(): BilibiliDownloadDao
    
    companion object {
        @Volatile
        private var INSTANCE: VideoDatabase? = null
        
        /**
         * 数据库版本1到2的迁移
         * 添加播放历史记录表
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建播放历史记录表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS playback_history (
                        uri TEXT PRIMARY KEY NOT NULL,
                        fileName TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        duration INTEGER NOT NULL,
                        lastPlayed INTEGER NOT NULL,
                        folderName TEXT NOT NULL,
                        danmuPath TEXT,
                        danmuVisible INTEGER NOT NULL DEFAULT 1,
                        danmuOffsetTime INTEGER NOT NULL DEFAULT 0,
                        thumbnailPath TEXT
                    )
                """.trimIndent())
                
                // 创建索引以优化查询性能
                database.execSQL("CREATE INDEX IF NOT EXISTS index_playback_history_lastPlayed ON playback_history(lastPlayed DESC)")
            }
        }
        
        /**
         * 数据库版本2到3的迁移
         * 为video_cache表添加索引以优化查询性能
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为 folderName 添加索引（优化 getAllVideos 排序）
                database.execSQL("CREATE INDEX IF NOT EXISTS index_video_cache_folderName ON video_cache(folderName)")
                
                // 为 folderPath 和 name 添加复合索引（优化 getVideosByFolder 查询和排序）
                database.execSQL("CREATE INDEX IF NOT EXISTS index_video_cache_folderPath_name ON video_cache(folderPath, name)")
                
                // 为 lastScanned 添加索引（优化 deleteOldEntries 条件过滤）
                database.execSQL("CREATE INDEX IF NOT EXISTS index_video_cache_lastScanned ON video_cache(lastScanned)")
            }
        }
        
        /**
         * 数据库版本3到4的迁移
         * 为video_cache表添加nameSortKey字段，实现自然排序
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. 添加nameSortKey字段
                database.execSQL("ALTER TABLE video_cache ADD COLUMN nameSortKey TEXT NOT NULL DEFAULT ''")
                
                // 2. 为现有数据生成nameSortKey值（使用SQLite的字符串函数进行简单处理）
                // 注意：SQLite的LOWER()函数只能处理ASCII字符，不能完全实现自然排序
                // 但由于新插入的数据会使用Kotlin的generateSortKey()，这里只需简单处理即可
                database.execSQL("UPDATE video_cache SET nameSortKey = LOWER(name)")
                
                // 3. 删除旧索引
                database.execSQL("DROP INDEX IF EXISTS index_video_cache_folderPath_name")
                
                // 4. 创建新索引（使用nameSortKey）
                database.execSQL("CREATE INDEX IF NOT EXISTS index_video_cache_folderPath_nameSortKey ON video_cache(folderPath, nameSortKey)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_video_cache_folderPath_dateAdded ON video_cache(folderPath, dateAdded)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS internal_downloads (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        systemDownloadId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        fileName TEXT NOT NULL,
                        url TEXT NOT NULL,
                        mimeType TEXT NOT NULL,
                        description TEXT NOT NULL,
                        sourcePageUrl TEXT NOT NULL,
                        sourcePageTitle TEXT NOT NULL,
                        headersJson TEXT NOT NULL,
                        status TEXT NOT NULL,
                        totalBytes INTEGER NOT NULL,
                        downloadedBytes INTEGER NOT NULL,
                        localUri TEXT NOT NULL,
                        localPath TEXT NOT NULL,
                        mediaType TEXT NOT NULL,
                        reason INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_internal_downloads_systemDownloadId ON internal_downloads(systemDownloadId)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_internal_downloads_status ON internal_downloads(status)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_internal_downloads_createdAt ON internal_downloads(createdAt)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bilibili_downloads (
                        id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        url TEXT NOT NULL,
                        status TEXT NOT NULL,
                        progress INTEGER NOT NULL,
                        filePath TEXT,
                        errorMessage TEXT,
                        mediaType TEXT NOT NULL,
                        totalSize INTEGER NOT NULL,
                        downloadedSize INTEGER NOT NULL,
                        fragmentsJson TEXT NOT NULL,
                        aid TEXT NOT NULL,
                        cid TEXT NOT NULL,
                        epId TEXT,
                        seasonId TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_bilibili_downloads_status ON bilibili_downloads(status)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_bilibili_downloads_createdAt ON bilibili_downloads(createdAt)"
                )
            }
        }
        
        fun getDatabase(context: Context): VideoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VideoDatabase::class.java,
                    "video_database"
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

