package com.android.kiyori.download

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InternalDownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val downloadManager = InternalDownloadManager.getInstance(application)

    val records: StateFlow<List<InternalDownloadEntity>> = downloadManager.records.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList()
    )

    private var refreshJob: Job? = null

    init {
        downloadManager.refreshAll()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                downloadManager.refreshIncomplete()
                delay(1500L)
            }
        }
    }

    fun addDownload(request: InternalDownloadRequest): Result<Long> {
        return downloadManager.enqueue(request)
    }

    fun retry(record: InternalDownloadEntity): Result<Long> {
        return downloadManager.retry(record)
    }

    fun cancel(record: InternalDownloadEntity) {
        downloadManager.cancel(record)
    }

    fun remove(record: InternalDownloadEntity, deleteFile: Boolean) {
        downloadManager.removeRecord(record, deleteFile)
    }

    fun clearCompleted(deleteFile: Boolean) {
        downloadManager.clearCompleted(deleteFile)
    }

    fun open(record: InternalDownloadEntity): Boolean {
        return downloadManager.openDownloadedFile(record)
    }

    fun rename(record: InternalDownloadEntity, targetFileName: String): Result<InternalDownloadEntity> {
        return downloadManager.rename(record, targetFileName)
    }

    fun moveToDirectory(
        record: InternalDownloadEntity,
        treeUriString: String,
        onResult: (Result<InternalDownloadEntity>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(
                runCatching {
                    withContext(Dispatchers.IO) {
                        downloadManager.moveRecordToDirectory(record, treeUriString)
                    }
                }
            )
        }
    }

    fun transferToPublicDirectory(
        record: InternalDownloadEntity,
        onResult: (Result<InternalDownloadEntity>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(
                runCatching {
                    withContext(Dispatchers.IO) {
                        downloadManager.transferRecordToPublicDirectory(record)
                    }
                }
            )
        }
    }

    override fun onCleared() {
        refreshJob?.cancel()
        refreshJob = null
        super.onCleared()
    }
}
