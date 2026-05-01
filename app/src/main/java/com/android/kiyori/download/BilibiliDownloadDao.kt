package com.android.kiyori.download

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BilibiliDownloadDao {
    @Query("SELECT * FROM bilibili_downloads ORDER BY createdAt DESC")
    fun getAll(): List<BilibiliDownloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: BilibiliDownloadEntity)

    @Query("DELETE FROM bilibili_downloads WHERE id = :id")
    fun deleteById(id: String)

    @Query("DELETE FROM bilibili_downloads WHERE status IN ('completed', 'cancelled')")
    fun deleteFinished()
}
