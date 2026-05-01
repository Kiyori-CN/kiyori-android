package com.android.kiyori.download

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.kiyori.database.VideoDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BilibiliDownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadManager = BilibiliDownloadManager(application)
    private val downloadDao = VideoDatabase.getDatabase(application).bilibiliDownloadDao()
    private val _downloadItems = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloadItems: StateFlow<List<DownloadItem>> = _downloadItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // SharedPreferences用于持久化存储路径
    private val prefs = application.getSharedPreferences("bilibili_download", android.content.Context.MODE_PRIVATE)
    
    // 存储路径
    private val _downloadPath = MutableStateFlow(
        prefs.getString("download_path", File(application.getExternalFilesDir(null), "downloads").absolutePath) ?: File(application.getExternalFilesDir(null), "downloads").absolutePath
    )
    val downloadPath: StateFlow<String> = _downloadPath
    
    private val _downloadPathDisplay = MutableStateFlow(
        prefs.getString("download_path_display", "downloads") ?: "downloads"
    )
    val downloadPathDisplay: StateFlow<String> = _downloadPathDisplay

    // 存储每个下载任务的Job，用于暂停/取消
    private val downloadJobs = mutableMapOf<String, Job>()

    private val TAG = "BilibiliDownloadVM"
    private val RESTORE_AS_PAUSED_STATUSES = setOf("pending", "downloading", "merging")

    init {
        loadDownloads()
    }

    private fun loadDownloads() {
        viewModelScope.launch {
            _isLoading.value = true
            val restoredItems = withContext(Dispatchers.IO) {
                downloadDao.getAll().map { entity ->
                    val item = entity.toDownloadItem()
                    if (item.status in RESTORE_AS_PAUSED_STATUSES) {
                        item.copy(
                            status = "paused",
                            errorMessage = "上次下载未完成，可重新开始",
                            updatedAt = System.currentTimeMillis()
                        ).also { downloadDao.upsert(it.toBilibiliDownloadEntity()) }
                    } else {
                        item
                    }
                }
            }
            _downloadItems.value = restoredItems
            _isLoading.value = false
        }
    }

    // 获取下载目录
    private fun getDownloadDir(): File {
        val path = _downloadPath.value
        // 如果是content URI，使用默认路径
        val dir = if (path.startsWith("content://")) {
            File(getApplication<Application>().getExternalFilesDir(null), "downloads")
        } else {
            File(path)
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    // 设置存储路径（支持DocumentTree URI）
    fun setDownloadPath(uriString: String, displayName: String = "") {
        _downloadPath.value = uriString
        _downloadPathDisplay.value = displayName.ifEmpty { uriString.substringAfterLast('/') }
        
        // 持久化保存
        prefs.edit().apply {
            putString("download_path", uriString)
            putString("download_path_display", _downloadPathDisplay.value)
            apply()
        }
        
        Log.d(TAG, "存储路径已更改并保存: $uriString, 显示: ${_downloadPathDisplay.value}")
    }

    // 创建下载文件（支持普通路径和content URI）
    private fun createDownloadFile(fileName: String): File {
        val path = _downloadPath.value
        return if (path.startsWith("content://")) {
            // 如果是content URI，需要使用DocumentFile API
            // 这里我们将文件先下载到临时目录，完成后再移动到用户选择的位置
            val tempDir = File(getApplication<Application>().cacheDir, "download_temp")
            tempDir.mkdirs()
            File(tempDir, fileName)
        } else {
            // 普通文件路径
            val dir = File(path)
            dir.mkdirs()
            File(dir, fileName)
        }
    }
    
    // 将临时文件移动到最终目录（处理DocumentFile）
    private fun moveToFinalLocation(tempFile: File, fileName: String): Boolean {
        val path = _downloadPath.value
        return try {
            if (path.startsWith("content://")) {
                // 使用DocumentFile API
                val uri = android.net.Uri.parse(path)
                val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(
                    getApplication(), uri
                )
                
                if (docFile != null && docFile.exists()) {
                    val mimeType = when {
                        fileName.endsWith(".mp4") -> "video/mp4"
                        fileName.endsWith(".m4s") -> "video/mp4"
                        else -> "application/octet-stream"
                    }
                    
                    val newFile = docFile.createFile(mimeType, fileName)
                    if (newFile != null) {
                        getApplication<android.app.Application>().contentResolver.openOutputStream(newFile.uri)?.use { output ->
                            tempFile.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        tempFile.delete()
                        Log.d(TAG, "文件已移动到DocumentTree: ${newFile.uri}")
                        true
                    } else {
                        Log.e(TAG, "无法在DocumentTree中创建文件")
                        false
                    }
                } else {
                    Log.e(TAG, "DocumentFile不存在")
                    false
                }
            } else {
                // 普通路径，文件已经在正确位置
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "移动文件失败", e)
            false
        }
    }

    // 选择存储路径（已废弃，使用UI层的文件选择器）
    @Deprecated("使用UI层的rememberLauncherForActivityResult")
    fun selectDownloadPath() {
        // 该方法已由UI层的文件选择器替代
    }

    private fun addDownloadItem(item: DownloadItem) {
        _downloadItems.value = _downloadItems.value + item
        persistItem(item)
    }

    private fun replaceDownloadItem(item: DownloadItem, persist: Boolean = true) {
        _downloadItems.value = _downloadItems.value.map {
            if (it.id == item.id) item else it
        }
        if (persist) {
            persistItem(item)
        }
    }

    private fun persistItem(item: DownloadItem) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadDao.upsert(item.toBilibiliDownloadEntity())
        }
    }

    private fun deletePersistedItem(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadDao.deleteById(id)
        }
    }

    // 清除已完成的下载
    fun clearCompletedDownloads() {
        _downloadItems.value = _downloadItems.value.filter { 
            it.status != "completed" && it.status != "cancelled" 
        }
        viewModelScope.launch(Dispatchers.IO) {
            downloadDao.deleteFinished()
        }
        Log.d(TAG, "已清除完成的下载记录")
    }

    fun addDownload(aid: String, cid: String, title: String) {
        val newItem = DownloadItem(
            id = System.currentTimeMillis().toString(),
            title = title,
            url = "bilibili://$aid/$cid",
            status = "pending",
            aid = aid,
            cid = cid
        )
        addDownloadItem(newItem)
        startDownload(newItem, aid, cid)
    }

    private fun startDownload(item: DownloadItem, aid: String, cid: String) {
        val resumableItem = item.copy(aid = aid, cid = cid, mediaType = MediaType.Video)
        replaceDownloadItem(resumableItem)
        startDownloadByMediaParse(
            item = resumableItem,
            parse = MediaParseResult(
                aid = aid,
                cid = cid,
                title = item.title,
                type = MediaType.Video
            )
        )
    }

    private fun updateItemProgress(id: String, progress: Int) {
        var updatedItem: DownloadItem? = null
        val boundedProgress = progress.coerceIn(0, 100)
        _downloadItems.value = _downloadItems.value.map {
            if (it.id == id) {
                it.copy(
                    progress = boundedProgress,
                    updatedAt = System.currentTimeMillis()
                ).also { updatedItem = it }
            } else {
                it
            }
        }
        updatedItem?.takeIf { item ->
            item.progress == 100 || item.progress % 5 == 0
        }?.let(::persistItem)
    }

    private fun updateItemDownloadPlan(id: String, fragments: List<DownloadFragment>) {
        val fragmentStates = fragments.map { fragment ->
            DownloadFragmentState(
                type = fragment.type,
                url = fragment.url,
                size = fragment.size
            )
        }
        val totalSize = fragmentStates.sumOf { it.size }
        var updatedItem: DownloadItem? = null
        _downloadItems.value = _downloadItems.value.map {
            if (it.id == id) {
                it.copy(
                    fragments = fragmentStates,
                    totalSize = totalSize,
                    updatedAt = System.currentTimeMillis()
                ).also { updatedItem = it }
            } else {
                it
            }
        }
        updatedItem?.let(::persistItem)
    }

    private fun updateItemStatus(id: String, status: String, errorMessage: String? = null) {
        var updatedItem: DownloadItem? = null
        _downloadItems.value = _downloadItems.value.map {
            if (it.id == id) {
                it.copy(
                    status = status,
                    errorMessage = errorMessage,
                    updatedAt = System.currentTimeMillis()
                ).also { updatedItem = it }
            } else {
                it
            }
        }
        updatedItem?.let(::persistItem)
    }

    private fun updateItemFilePath(id: String, filePath: String) {
        var updatedItem: DownloadItem? = null
        _downloadItems.value = _downloadItems.value.map {
            if (it.id == id) {
                it.copy(
                    filePath = filePath,
                    updatedAt = System.currentTimeMillis()
                ).also { updatedItem = it }
            } else {
                it
            }
        }
        updatedItem?.let(::persistItem)
    }

    // 新增：解析链接，返回可选集数（番剧）或视频信息
    suspend fun parseMediaUrlSync(url: String): MediaParseResult {
        return downloadManager.parseMediaUrl(url)
    }

    // 新增：获取番剧所有集数列表
    suspend fun getBangumiEpisodesSync(id: String): Result<List<EpisodeInfo>> {
        return downloadManager.getBangumiEpisodes(id)
    }

    // 新增：下载选定集数（支持视频/番剧）
    fun addDownloadByMediaParse(parse: MediaParseResult) {
        val newItem = DownloadItem(
            id = System.currentTimeMillis().toString(),
            title = parse.title,
            url = "bilibili://${parse.aid}/${parse.cid}",
            status = "pending",
            mediaType = parse.type,
            aid = parse.aid,
            cid = parse.cid,
            epId = parse.epId,
            seasonId = parse.seasonId
        )
        addDownloadItem(newItem)
        startDownloadByMediaParse(newItem, parse)
    }

    private fun startDownloadByMediaParse(item: DownloadItem, parse: MediaParseResult) {
        val job = viewModelScope.launch {
            updateItemStatus(item.id, "downloading")
            try {
                val result = downloadManager.getMediaInfo(
                    id = parse.aid,
                    cid = parse.cid,
                    isBangumi = (parse.type == MediaType.Bangumi),
                    epId = parse.epId,
                    seasonId = parse.seasonId
                )
                if (result.isSuccess) {
                    val videoInfo = result.getOrThrow()
                    updateItemDownloadPlan(item.id, videoInfo.fragments)
                    if (videoInfo.fragments.isEmpty()) {
                        updateItemStatus(item.id, "failed", "未获取到可下载片段")
                        return@launch
                    }
                    val downloadedFiles = mutableListOf<File>()
                    val fragmentCount = videoInfo.fragments.size
                    
                    for ((index, fragment) in videoInfo.fragments.withIndex()) {
                        val fileName = "${item.title}_${fragment.type}.m4s"
                        val file = createDownloadFile(fileName)
                        
                        Log.d(TAG, "开始下载片段 ${index + 1}/$fragmentCount: ${fragment.type}")
                        
                        val downloadResult = downloadManager.downloadFile(fragment.url, file) { bytesRead, totalBytes ->
                            val fragmentProgress = if (totalBytes > 0) {
                                (bytesRead.toFloat() / totalBytes * 100).toInt()
                            } else 0
                            
                            val completedWeight = index * 100
                            val overallProgress = (completedWeight + fragmentProgress) / fragmentCount
                            
                            updateItemProgress(item.id, overallProgress)
                        }
                        
                        if (downloadResult.isFailure) {
                            updateItemStatus(item.id, "failed", downloadResult.exceptionOrNull()?.message)
                            downloadedFiles.forEach { it.delete() }
                            return@launch
                        }
                        
                        downloadedFiles.add(file)
                        Log.d(TAG, "片段下载完成: ${fragment.type}, 文件大小: ${file.length() / 1024 / 1024}MB")
                    }
                    
                    // 合并音视频
                    if (downloadedFiles.size == 2) {
                        updateItemStatus(item.id, "merging")
                        val videoFile = downloadedFiles.find { it.name.contains("video") }
                        val audioFile = downloadedFiles.find { it.name.contains("audio") }
                        
                        if (videoFile != null && audioFile != null) {
                            val outputFileName = "${item.title}.mp4"
                            val outputFile = createDownloadFile(outputFileName)
                            
                            // 在IO线程执行合并，避免UI卡顿
                            val mergeResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                mergeVideoAudio(videoFile, audioFile, outputFile)
                            }
                            
                            if (mergeResult) {
                                videoFile.delete()
                                audioFile.delete()
                                updateItemFilePath(item.id, outputFile.absolutePath)
                                
                                // 如果是DocumentTree，移动到最终位置
                                if (_downloadPath.value.startsWith("content://")) {
                                    if (moveToFinalLocation(outputFile, outputFileName)) {
                                        updateItemStatus(item.id, "completed")
                                    } else {
                                        updateItemStatus(item.id, "failed", "无法保存到选择的文件夹")
                                    }
                                } else {
                                    updateItemStatus(item.id, "completed")
                                }
                            } else {
                                updateItemStatus(item.id, "failed", "音视频合并失败")
                            }
                        }
                    } else {
                        downloadedFiles.firstOrNull()?.let { downloadedFile ->
                            updateItemFilePath(item.id, downloadedFile.absolutePath)
                        }
                        updateItemStatus(item.id, "completed")
                    }
                } else {
                    updateItemStatus(item.id, "failed", result.exceptionOrNull()?.message)
                }
            } catch (e: CancellationException) {
                updateItemStatus(item.id, "paused")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "下载异常", e)
                updateItemStatus(item.id, "failed", e.message)
            } finally {
                downloadJobs.remove(item.id)
            }
        }
        
        downloadJobs[item.id] = job
    }

    // 新增：根据EpisodeInfo下载番剧集数
    fun addDownloadByEpisode(episode: EpisodeInfo, seasonId: String) {
        val newItem = DownloadItem(
            id = System.currentTimeMillis().toString(),
            title = episode.longTitle.ifEmpty { episode.title },
            url = "bilibili://ep${episode.episodeId}",
            status = "pending",
            mediaType = MediaType.Bangumi,
            aid = episode.aid,
            cid = episode.cid,
            epId = episode.episodeId,
            seasonId = seasonId
        )
        addDownloadItem(newItem)
        startDownloadByMediaParse(
            item = newItem,
            parse = MediaParseResult(
                aid = episode.aid,
                cid = episode.cid,
                title = newItem.title,
                type = MediaType.Bangumi,
                epId = episode.episodeId,
                seasonId = seasonId
            )
        )
    }
    
    // 合并音视频（使用MediaMuxer）
    private fun mergeVideoAudio(videoFile: File, audioFile: File, outputFile: File): Boolean {
        return try {
            Log.d(TAG, "开始合并: video=${videoFile.length()}bytes, audio=${audioFile.length()}bytes")
            
            // 使用Android MediaMuxer合并
            val muxer = android.media.MediaMuxer(
                outputFile.absolutePath,
                android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            
            // 提取video track
            val videoExtractor = android.media.MediaExtractor()
            videoExtractor.setDataSource(videoFile.absolutePath)
            var videoFormat: android.media.MediaFormat? = null
            
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoFormat = format
                    videoExtractor.selectTrack(i)
                    Log.d(TAG, "找到视频轨道: $mime")
                    break
                }
            }
            
            // 提取audio track
            val audioExtractor = android.media.MediaExtractor()
            audioExtractor.setDataSource(audioFile.absolutePath)
            var audioFormat: android.media.MediaFormat? = null
            
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioFormat = format
                    audioExtractor.selectTrack(i)
                    Log.d(TAG, "找到音频轨道: $mime")
                    break
                }
            }
            
            if (videoFormat == null || audioFormat == null) {
                Log.e(TAG, "无法找到音视频轨道")
                videoExtractor.release()
                audioExtractor.release()
                return false
            }
            
            // 添加轨道到muxer
            val muxerVideoTrack = muxer.addTrack(videoFormat)
            val muxerAudioTrack = muxer.addTrack(audioFormat)
            muxer.start()
            
            // 复制视频数据
            val videoBuffer = java.nio.ByteBuffer.allocate(1024 * 1024)
            val videoBufferInfo = android.media.MediaCodec.BufferInfo()
            
            while (true) {
                videoBufferInfo.size = videoExtractor.readSampleData(videoBuffer, 0)
                if (videoBufferInfo.size < 0) break
                
                videoBufferInfo.presentationTimeUs = videoExtractor.sampleTime
                videoBufferInfo.flags = mapExtractorFlags(videoExtractor.sampleFlags)
                
                muxer.writeSampleData(muxerVideoTrack, videoBuffer, videoBufferInfo)
                videoExtractor.advance()
            }
            Log.d(TAG, "视频轨道复制完成")
            
            // 复制音频数据
            val audioBuffer = java.nio.ByteBuffer.allocate(1024 * 1024)
            val audioBufferInfo = android.media.MediaCodec.BufferInfo()
            
            while (true) {
                audioBufferInfo.size = audioExtractor.readSampleData(audioBuffer, 0)
                if (audioBufferInfo.size < 0) break
                
                audioBufferInfo.presentationTimeUs = audioExtractor.sampleTime
                audioBufferInfo.flags = mapExtractorFlags(audioExtractor.sampleFlags)
                
                muxer.writeSampleData(muxerAudioTrack, audioBuffer, audioBufferInfo)
                audioExtractor.advance()
            }
            Log.d(TAG, "音频轨道复制完成")
            
            // 释放资源
            videoExtractor.release()
            audioExtractor.release()
            muxer.stop()
            muxer.release()
            
            Log.d(TAG, "合并完成，输出文件大小: ${outputFile.length() / 1024 / 1024}MB")
            true
        } catch (e: Exception) {
            Log.e(TAG, "合并失败", e)
            false
        }
    }

    fun pauseDownload(item: DownloadItem) {
        downloadJobs[item.id]?.cancel()
        downloadJobs.remove(item.id)
        updateItemStatus(item.id, "paused")
        Log.d(TAG, "暂停下载: ${item.title}")
    }

    fun resumeDownload(item: DownloadItem) {
        Log.d(TAG, "恢复下载: ${item.title}")
        if (downloadJobs.containsKey(item.id)) {
            return
        }
        if (item.aid.isBlank() || item.cid.isBlank()) {
            updateItemStatus(item.id, "failed", "缺少可恢复下载信息")
            return
        }
        val resetItem = item.copy(
            status = "pending",
            progress = 0,
            errorMessage = null,
            updatedAt = System.currentTimeMillis()
        )
        replaceDownloadItem(resetItem)
        startDownloadByMediaParse(
            item = resetItem,
            parse = MediaParseResult(
                aid = resetItem.aid,
                cid = resetItem.cid,
                title = resetItem.title,
                type = resetItem.mediaType,
                epId = resetItem.epId,
                seasonId = resetItem.seasonId
            )
        )
    }

    fun cancelDownload(item: DownloadItem) {
        downloadJobs[item.id]?.cancel()
        downloadJobs.remove(item.id)
        _downloadItems.value = _downloadItems.value.filter { it.id != item.id }
        deletePersistedItem(item.id)
        Log.d(TAG, "取消下载: ${item.title}")
    }

    private fun mapExtractorFlags(sampleFlags: Int): Int {
        var bufferFlags = 0
        if ((sampleFlags and android.media.MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
            bufferFlags = bufferFlags or android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME
        }
        if ((sampleFlags and android.media.MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
            bufferFlags = bufferFlags or android.media.MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
        }
        return bufferFlags
    }
}
