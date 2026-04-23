package com.android.kiyori.download

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InternalDownloadDao {
    @Query("SELECT * FROM internal_downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InternalDownloadEntity>>

    @Query("SELECT * FROM internal_downloads ORDER BY createdAt DESC")
    fun getAll(): List<InternalDownloadEntity>

    @Query("SELECT * FROM internal_downloads WHERE status IN (:statuses) ORDER BY createdAt DESC")
    fun getByStatuses(statuses: List<String>): List<InternalDownloadEntity>

    @Query("SELECT * FROM internal_downloads WHERE systemDownloadId = :systemDownloadId LIMIT 1")
    fun getBySystemDownloadId(systemDownloadId: Long): InternalDownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: InternalDownloadEntity): Long

    @Update
    fun update(entity: InternalDownloadEntity)

    @Query("DELETE FROM internal_downloads WHERE id = :id")
    fun deleteById(id: Long)
}
