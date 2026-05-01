package com.android.kiyori.download

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.android.kiyori.BuildConfig
import com.android.kiyori.database.VideoDatabase
import com.android.kiyori.player.ui.VideoPlayerActivity
import com.android.kiyori.remote.RemotePlaybackHeaders
import com.tencent.smtt.sdk.CookieManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class InternalDownloadManager private constructor(
    private val appContext: Context
) {
    private val dao = VideoDatabase.getDatabase(appContext).internalDownloadDao()
    private val preferencesRepository = DownloadPreferencesRepository(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runningJobs = ConcurrentHashMap<Long, Job>()
    private val schedulerMutex = Mutex()
    private val nextDownloadId = AtomicLong(System.currentTimeMillis())

    val records: Flow<List<InternalDownloadEntity>> = dao.observeAll()

    fun enqueue(request: InternalDownloadRequest): Result<Long> {
        return runCatching {
            val safeUrl = request.url.trim()
            require(safeUrl.isNotBlank()) { "下载地址不能为空" }

            val preparedHeaders = prepareHeaders(
                url = safeUrl,
                sourcePageUrl = request.sourcePageUrl,
                inputHeaders = request.headers
            )
            val resolvedFileName = sanitizeFileName(
                request.fileName.trim().ifBlank {
                    URLUtil.guessFileName(
                        safeUrl,
                        RemotePlaybackHeaders.get(preparedHeaders, "Content-Disposition"),
                        request.mimeType.ifBlank { null }
                    )
                }
            )
            val resolvedMimeType = resolveMimeType(
                mimeType = request.mimeType,
                fileName = resolvedFileName,
                url = safeUrl
            )
            val resolvedTitle = request.title.trim().ifBlank { resolvedFileName }
            val resolvedDescription = request.description.trim().ifBlank { "准备下载" }
            val downloadId = nextDownloadId.incrementAndGet()
            val now = System.currentTimeMillis()

            scope.launch {
                dao.insert(
                    InternalDownloadEntity(
                        systemDownloadId = downloadId,
                        title = resolvedTitle,
                        fileName = resolvedFileName,
                        url = safeUrl,
                        mimeType = resolvedMimeType,
                        description = resolvedDescription,
                        sourcePageUrl = request.sourcePageUrl.trim(),
                        sourcePageTitle = request.sourcePageTitle.trim(),
                        headersJson = headersToJson(preparedHeaders),
                        status = InternalDownloadStatus.PENDING,
                        mediaType = request.mediaType.trim(),
                        createdAt = now,
                        updatedAt = now
                    )
                )
                scheduleDownloads()
            }

            downloadId
        }
    }

    fun refreshAll() {
        scope.launch {
            scheduleDownloads()
        }
    }

    fun recoverInterruptedDownloads() {
        scope.launch {
            markInterruptedDownloads()
            scheduleDownloads()
        }
    }

    fun refreshIncomplete() {
        scope.launch {
            scheduleDownloads()
        }
    }

    fun cancel(record: InternalDownloadEntity) {
        scope.launch {
            runningJobs.remove(record.systemDownloadId)?.cancel()
            deleteTempFile(record.fileName)
            dao.update(
                record.copy(
                    status = InternalDownloadStatus.CANCELLED,
                    updatedAt = System.currentTimeMillis()
                )
            )
            scheduleDownloads()
        }
    }

    fun clearCompleted(deleteFile: Boolean) {
        scope.launch {
            dao.getByStatuses(
                listOf(
                    InternalDownloadStatus.SUCCESS,
                    InternalDownloadStatus.FAILED,
                    InternalDownloadStatus.CANCELLED
                )
            ).forEach { removeRecordInternal(it, deleteFile) }
        }
    }

    fun removeRecord(record: InternalDownloadEntity, deleteFile: Boolean) {
        scope.launch {
            removeRecordInternal(record, deleteFile)
        }
    }

    fun retry(record: InternalDownloadEntity): Result<Long> {
        return enqueue(
            InternalDownloadRequest(
                url = record.url,
                title = record.title,
                fileName = record.fileName,
                mimeType = record.mimeType,
                description = record.description,
                sourcePageUrl = record.sourcePageUrl,
                sourcePageTitle = record.sourcePageTitle,
                mediaType = record.mediaType,
                headers = jsonToHeaders(record.headersJson)
            )
        ).onSuccess {
            removeRecord(record, deleteFile = false)
        }
    }

    fun openDownloadedFile(record: InternalDownloadEntity): Boolean {
        val uri = resolveOpenUri(record) ?: return false
        val mimeType = record.mimeType.ifBlank {
            resolveMimeType(record.mimeType, record.fileName, record.url)
        }
        if (isApkPackage(mimeType, record.fileName)) {
            return runCatching {
                appContext.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                )
                scheduleAutoCleanApk(record)
                true
            }.getOrDefault(false)
        }
        if (isPlayableMedia(mimeType, record.fileName)) {
            return runCatching {
                appContext.startActivity(
                    Intent(appContext, VideoPlayerActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        setDataAndType(uri, mimeType.ifBlank { "*/*" })
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra(VideoPlayerActivity.EXTRA_FORCE_LOCAL_PLAYBACK, true)
                        putExtra("video_title", record.title.ifBlank { record.fileName })
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                true
            }.getOrDefault(false)
        }
        return runCatching {
            appContext.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType.ifBlank { "*/*" })
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
            true
        }.getOrDefault(false)
    }

    fun rename(record: InternalDownloadEntity, targetFileName: String): Result<InternalDownloadEntity> {
        return runCatching {
            val safeFileName = sanitizeFileName(targetFileName)
            require(safeFileName.isNotBlank()) { "文件名不能为空" }

            var localUri = record.localUri
            var localPath = record.localPath

            if (record.status == InternalDownloadStatus.SUCCESS) {
                val localFile = resolveLocalFile(record)
                if (localFile != null && localFile.exists()) {
                    val renamedFile = renameLocalFile(localFile, safeFileName)
                    localPath = renamedFile.absolutePath
                    localUri = buildLocalContentUri(renamedFile).toString()
                } else {
                    val openUri = record.localUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
                    if (openUri?.scheme?.equals("content", ignoreCase = true) == true) {
                        val renamedUri = DocumentsContract.renameDocument(
                            appContext.contentResolver,
                            openUri,
                            safeFileName
                        ) ?: throw IllegalStateException("当前文件暂不支持重命名")
                        localUri = renamedUri.toString()
                    }
                }
            }

            val updatedRecord = record.copy(
                title = safeFileName,
                fileName = safeFileName,
                localUri = localUri,
                localPath = localPath,
                updatedAt = System.currentTimeMillis()
            )
            scope.launch {
                dao.update(updatedRecord)
            }
            updatedRecord
        }
    }

    suspend fun moveRecordToDirectory(
        record: InternalDownloadEntity,
        treeUriString: String
    ): InternalDownloadEntity {
        require(treeUriString.isNotBlank()) { "目标目录不能为空" }
        require(!hasLocalM3u8Package(record)) {
            "M3U8离线包需要保留在应用下载目录"
        }
        val mimeType = record.mimeType.ifBlank {
            resolveMimeType(record.mimeType, record.fileName, record.url)
        }
        val movedLocation = resolveLocalFile(record)
            ?.takeIf { it.exists() }
            ?.let {
                copyToCustomDirectory(
                    sourceFile = it,
                    displayName = record.fileName,
                    mimeType = mimeType,
                    treeUriString = treeUriString
                )
            }
            ?: copyUriToCustomDirectory(
                sourceUri = resolveOpenUri(record) ?: throw IOException("当前文件不存在"),
                displayName = record.fileName,
                mimeType = mimeType,
                treeUriString = treeUriString
            )

        deleteDownloadedFile(record)

        val updatedRecord = record.copy(
            title = movedLocation.displayName,
            fileName = movedLocation.displayName,
            localUri = movedLocation.uri.toString(),
            localPath = movedLocation.absolutePath,
            updatedAt = System.currentTimeMillis()
        )
        dao.update(updatedRecord)
        return updatedRecord
    }

    suspend fun transferRecordToPublicDirectory(record: InternalDownloadEntity): InternalDownloadEntity {
        require(!hasLocalM3u8Package(record)) {
            "M3U8离线包需要保留在应用下载目录"
        }
        val mimeType = record.mimeType.ifBlank {
            resolveMimeType(record.mimeType, record.fileName, record.url)
        }
        val publicLocation = resolveLocalFile(record)
            ?.takeIf { it.exists() }
            ?.let {
                copyToPublicDownloads(
                    sourceFile = it,
                    displayName = record.fileName,
                    mimeType = mimeType
                )
            }
            ?: copyUriToPublicDownloads(
                sourceUri = resolveOpenUri(record) ?: throw IOException("当前文件不存在"),
                displayName = record.fileName,
                mimeType = mimeType
            )

        deleteDownloadedFile(record)

        val updatedRecord = record.copy(
            title = publicLocation.displayName,
            fileName = publicLocation.displayName,
            localUri = publicLocation.uri.toString(),
            localPath = publicLocation.absolutePath,
            updatedAt = System.currentTimeMillis()
        )
        dao.update(updatedRecord)
        return updatedRecord
    }

    private suspend fun removeRecordInternal(record: InternalDownloadEntity, deleteFile: Boolean) {
        runningJobs.remove(record.systemDownloadId)?.cancel()
        deleteTempFile(record.fileName)
        if (deleteFile) {
            deleteDownloadedFile(record)
        }
        dao.deleteById(record.id)
        scheduleDownloads()
    }

    private suspend fun scheduleDownloads() {
        schedulerMutex.withLock {
            val settings = preferencesRepository.getSettings()
            val allRecords = dao.getAll()
            val validIds = allRecords.mapTo(hashSetOf()) { it.systemDownloadId }

            runningJobs.entries.toList().forEach { (downloadId, job) ->
                if (!job.isActive || downloadId !in validIds) {
                    runningJobs.remove(downloadId)
                }
            }

            val candidates = allRecords
                .filter { shouldSchedule(it.status) }
                .sortedBy { it.createdAt }

            candidates.forEach { record ->
                if (runningJobs.size >= settings.maxConcurrentTasks) {
                    return
                }
                if (runningJobs.containsKey(record.systemDownloadId)) {
                    return@forEach
                }

                val job = scope.launch {
                    performDownload(record.systemDownloadId)
                }
                runningJobs[record.systemDownloadId] = job
                job.invokeOnCompletion {
                    runningJobs.remove(record.systemDownloadId)
                    scope.launch {
                        scheduleDownloads()
                    }
                }
            }
        }
    }

    private suspend fun markInterruptedDownloads() {
        schedulerMutex.withLock {
            dao.getByStatuses(
                listOf(
                    InternalDownloadStatus.RUNNING,
                    InternalDownloadStatus.UNKNOWN
                )
            ).forEach { record ->
                if (runningJobs.containsKey(record.systemDownloadId)) {
                    return@forEach
                }
                deleteTempFile(record.fileName)
                dao.update(
                    record.copy(
                        description = "上次下载中断，可重试",
                        status = InternalDownloadStatus.FAILED,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private suspend fun performDownload(downloadId: Long) {
        val existing = dao.getBySystemDownloadId(downloadId) ?: return
        val settings = preferencesRepository.getSettings()
        val headers = jsonToHeaders(existing.headersJson)
        val targetFile = resolveAvailableTargetFile(existing.fileName)
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.part")
        var isLocalM3u8Package = false
        tempFile.parentFile?.mkdirs()
        if (tempFile.exists()) {
            tempFile.delete()
        }

        val runningRecord = existing.copy(
            title = targetFile.name,
            fileName = targetFile.name,
            status = InternalDownloadStatus.RUNNING,
            downloadedBytes = 0L,
            totalBytes = 0L,
            localUri = "",
            localPath = targetFile.absolutePath,
            updatedAt = System.currentTimeMillis()
        )
        dao.update(runningRecord)

        try {
            val requestBuilder = Request.Builder().url(existing.url)
            headers.forEach { (key, value) ->
                if (key.isNotBlank() && value.isNotBlank()) {
                    requestBuilder.header(key, value)
                }
            }

            val client = buildHttpClient(settings)
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }

                val body = response.body ?: throw IOException("响应体为空")
                val responseMimeType = response.body?.contentType()?.toString().orEmpty()
                val resolvedMimeType = resolveMimeType(
                    mimeType = runningRecord.mimeType.ifBlank { responseMimeType },
                    fileName = targetFile.name,
                    url = existing.url
                )
                val totalBytes = body.contentLength().coerceAtLeast(0L)
                val canUseSegmentedDownload = !isM3u8Resource(existing.url, targetFile.name, resolvedMimeType) &&
                    totalBytes > settings.chunkSizeKb.coerceIn(128, 12288) * 1024L &&
                    settings.normalThreadCount > 1 &&
                    (
                        supportsRangeDownload(response) ||
                            probeRangeDownload(
                                client = client,
                                url = response.request.url.toString(),
                                headers = headers
                            )
                    )
                val downloadedBytes = if (isM3u8Resource(existing.url, targetFile.name, resolvedMimeType)) {
                    val playlistContent = body.string()
                    val normalizedContent = if (settings.autoMergeM3u8) {
                        isLocalM3u8Package = true
                        downloadM3u8Package(
                            content = playlistContent,
                            baseUrl = response.request.url.toString(),
                            headers = headers,
                            client = client,
                            outputPlaylistFile = tempFile,
                            settings = settings,
                            runningRecord = runningRecord.copy(mimeType = resolvedMimeType),
                            targetFile = targetFile
                        )
                    } else {
                        playlistContent
                    }
                    if (!settings.autoMergeM3u8) {
                        tempFile.writeText(normalizedContent)
                    }
                    tempFile.length()
                } else if (canUseSegmentedDownload) {
                    body.close()
                    downloadSegmentedFile(
                        client = client,
                        url = response.request.url.toString(),
                        headers = headers,
                        tempFile = tempFile,
                        totalBytes = totalBytes,
                        settings = settings,
                        runningRecord = runningRecord.copy(mimeType = resolvedMimeType, totalBytes = totalBytes),
                        targetFile = targetFile
                    )
                } else {
                    val buffer = ByteArray(settings.chunkSizeKb.coerceIn(128, 12288) * 1024)
                    var downloaded = 0L
                    var lastStoredAt = 0L

                    body.byteStream().use { input ->
                        tempFile.outputStream().buffered().use { output ->
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val read = input.read(buffer)
                                if (read < 0) {
                                    break
                                }
                                output.write(buffer, 0, read)
                                downloaded += read

                                val now = System.currentTimeMillis()
                                if (now - lastStoredAt >= 350L) {
                                    dao.update(
                                        runningRecord.copy(
                                            mimeType = resolvedMimeType,
                                            totalBytes = totalBytes,
                                            downloadedBytes = downloaded,
                                            localPath = targetFile.absolutePath,
                                            updatedAt = now
                                        )
                                    )
                                    lastStoredAt = now
                                }
                            }
                            output.flush()
                        }
                    }
                    downloaded
                }

                currentCoroutineContext().ensureActive()
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                require(tempFile.renameTo(targetFile)) { "保存文件失败" }

                currentCoroutineContext().ensureActive()
                val finalLocation = if (settings.customDirectoryUri.isNotBlank() && !isLocalM3u8Package) {
                    copyToCustomDirectory(
                        sourceFile = targetFile,
                        displayName = targetFile.name,
                        mimeType = resolvedMimeType,
                        treeUriString = settings.customDirectoryUri
                    )
                } else {
                    SavedDownloadLocation(
                        uri = buildLocalContentUri(targetFile),
                        absolutePath = targetFile.absolutePath,
                        displayName = targetFile.name,
                        fileSizeBytes = targetFile.length()
                    )
                }

                val storedLocation = if (
                    settings.autoTransferToPublicDir &&
                    settings.customDirectoryUri.isBlank() &&
                    !isLocalM3u8Package
                ) {
                    copyToPublicDownloads(
                        sourceFile = targetFile,
                        displayName = finalLocation.displayName,
                        mimeType = resolvedMimeType
                    )
                } else {
                    finalLocation
                }

                if (
                    !isLocalM3u8Package &&
                    (settings.customDirectoryUri.isNotBlank() || settings.autoTransferToPublicDir) &&
                    targetFile.exists()
                ) {
                    targetFile.delete()
                }

                currentCoroutineContext().ensureActive()
                val storedBytes = if (isLocalM3u8Package) {
                    targetFile.length() + directorySizeBytes(m3u8PackageDirectoryFor(targetFile))
                } else {
                    storedLocation.fileSizeBytes
                }
                val successTotalBytes = if (isLocalM3u8Package) {
                    storedBytes
                } else {
                    maxOf(totalBytes, downloadedBytes, storedBytes)
                }

                val successRecord = runningRecord.copy(
                    mimeType = resolvedMimeType,
                    status = InternalDownloadStatus.SUCCESS,
                    title = storedLocation.displayName,
                    fileName = storedLocation.displayName,
                    totalBytes = successTotalBytes,
                    downloadedBytes = storedBytes,
                    localUri = storedLocation.uri.toString(),
                    localPath = storedLocation.absolutePath,
                    updatedAt = System.currentTimeMillis(),
                    completedAt = System.currentTimeMillis()
                )
                currentCoroutineContext().ensureActive()
                dao.update(successRecord)

                if (settings.showCompletionTip) {
                    postToast("下载完成：${storedLocation.displayName}")
                }
            }
        } catch (_: CancellationException) {
            tempFile.delete()
            if (isLocalM3u8Package) {
                m3u8PackageDirectoryFor(targetFile).deleteRecursively()
            }
            dao.update(
                runningRecord.copy(
                    status = InternalDownloadStatus.CANCELLED,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } catch (error: Exception) {
            tempFile.delete()
            if (isLocalM3u8Package) {
                m3u8PackageDirectoryFor(targetFile).deleteRecursively()
            }
            dao.update(
                runningRecord.copy(
                    description = error.message?.take(160).orEmpty().ifBlank { "下载失败" },
                    status = InternalDownloadStatus.FAILED,
                    updatedAt = System.currentTimeMillis()
                )
            )
            if (settings.showCompletionTip) {
                postToast("下载失败：${runningRecord.fileName}")
            }
        }
    }

    private suspend fun downloadM3u8Package(
        content: String,
        baseUrl: String,
        headers: Map<String, String>,
        client: OkHttpClient,
        outputPlaylistFile: File,
        settings: DownloadSettingsState,
        runningRecord: InternalDownloadEntity,
        targetFile: File,
        depth: Int = 0
    ): String = coroutineScope {
        if (depth >= 4) {
            outputPlaylistFile.writeText(content)
            return@coroutineScope content
        }

        val mediaPlaylist = selectM3u8MediaPlaylist(content, baseUrl, headers, client)
        if (mediaPlaylist != null) {
            return@coroutineScope downloadM3u8Package(
                content = mediaPlaylist.content,
                baseUrl = mediaPlaylist.url,
                headers = headers,
                client = client,
                outputPlaylistFile = outputPlaylistFile,
                settings = settings,
                runningRecord = runningRecord,
                targetFile = targetFile,
                depth = depth + 1
            )
        }

        val assetDirectory = m3u8PackageDirectoryFor(targetFile)
        if (assetDirectory.exists()) {
            assetDirectory.deleteRecursively()
        }
        assetDirectory.mkdirs()

        val resources = mutableListOf<M3u8PackageResource>()
        val rewrittenLines = mutableListOf<String>()
        var mediaIndex = 0
        var sidecarIndex = 0

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.isBlank() -> rewrittenLines += rawLine
                line.startsWith("#EXT-X-KEY:", ignoreCase = true) ||
                    line.startsWith("#EXT-X-MAP:", ignoreCase = true) -> {
                    val uri = extractM3u8UriAttribute(line)
                    if (uri.isNullOrBlank()) {
                        rewrittenLines += rawLine
                    } else {
                        val file = File(assetDirectory, "resource_${sidecarIndex++}${guessM3u8ResourceExtension(uri)}")
                        resources += M3u8PackageResource(
                            url = resolveUrl(baseUrl, uri),
                            file = file
                        )
                        rewrittenLines += replaceM3u8UriAttributeWithValue(line, file.toURI().toString())
                    }
                }

                line.startsWith("#") -> rewrittenLines += rawLine
                else -> {
                    val file = File(assetDirectory, "segment_${mediaIndex++}${guessM3u8ResourceExtension(line)}")
                    resources += M3u8PackageResource(
                        url = resolveUrl(baseUrl, line),
                        file = file
                    )
                    rewrittenLines += file.toURI().toString()
                }
            }
        }

        val downloadedBytes = AtomicLong(0L)
        val lastStoredAt = AtomicLong(0L)
        val limiter = Semaphore(settings.m3u8ThreadCount.coerceIn(1, 64))

        var completed = false
        try {
            resources.map { resource ->
                async(Dispatchers.IO) {
                    limiter.withPermit {
                        downloadM3u8ResourceWithRetry(
                            client = client,
                            headers = headers,
                            resource = resource,
                            onBytes = { bytes ->
                                val current = downloadedBytes.addAndGet(bytes.toLong())
                                val now = System.currentTimeMillis()
                                val last = lastStoredAt.get()
                                if (now - last >= 350L && lastStoredAt.compareAndSet(last, now)) {
                                    dao.update(
                                        runningRecord.copy(
                                            totalBytes = 0L,
                                            downloadedBytes = current,
                                            localPath = targetFile.absolutePath,
                                            updatedAt = now
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }.awaitAll()

            currentCoroutineContext().ensureActive()
            val rewrittenContent = rewrittenLines.joinToString("\n")
            outputPlaylistFile.writeText(rewrittenContent)
            completed = true
            rewrittenContent
        } finally {
            if (!completed) {
                assetDirectory.deleteRecursively()
            }
        }
    }

    private fun selectM3u8MediaPlaylist(
        content: String,
        baseUrl: String,
        headers: Map<String, String>,
        client: OkHttpClient
    ): M3u8MediaPlaylist? {
        var pendingStreamInf = false
        var pendingBandwidth = -1L
        var selectedUrl: String? = null
        var selectedBandwidth = -1L
        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("#EXT-X-STREAM-INF", ignoreCase = true) -> {
                    pendingStreamInf = true
                    pendingBandwidth = extractM3u8LongAttribute(line, "BANDWIDTH") ?: -1L
                }
                pendingStreamInf && line.isNotBlank() && !line.startsWith("#") -> {
                    if (selectedUrl == null || pendingBandwidth > selectedBandwidth) {
                        selectedUrl = resolveUrl(baseUrl, line)
                        selectedBandwidth = pendingBandwidth
                    }
                    pendingStreamInf = false
                    pendingBandwidth = -1L
                }
                line.isNotBlank() && !line.startsWith("#") -> pendingStreamInf = false
            }
        }
        val url = selectedUrl ?: return null
        return M3u8MediaPlaylist(
            url = url,
            content = fetchTextContent(client, url, headers)
        )
    }

    private suspend fun downloadM3u8ResourceWithRetry(
        client: OkHttpClient,
        headers: Map<String, String>,
        resource: M3u8PackageResource,
        onBytes: (Int) -> Unit
    ) {
        var lastError: Exception? = null
        repeat(SEGMENT_DOWNLOAD_RETRY_COUNT) { attempt ->
            var reportedBytes = 0L
            try {
                downloadM3u8Resource(
                    client = client,
                    headers = headers,
                    resource = resource,
                    onBytes = { bytes ->
                        reportedBytes += bytes.toLong()
                        onBytes(bytes)
                    }
                )
                return
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                lastError = error
                if (reportedBytes > 0L) {
                    onBytes(-reportedBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                }
                if (attempt < SEGMENT_DOWNLOAD_RETRY_COUNT - 1) {
                    resource.file.delete()
                    delay(250L * (attempt + 1))
                }
            }
        }
        throw lastError ?: IOException("M3U8分片下载失败")
    }

    private suspend fun downloadM3u8Resource(
        client: OkHttpClient,
        headers: Map<String, String>,
        resource: M3u8PackageResource,
        onBytes: (Int) -> Unit
    ) {
        currentCoroutineContext().ensureActive()
        resource.file.parentFile?.mkdirs()
        val requestBuilder = Request.Builder().url(resource.url)
        headers.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank()) {
                requestBuilder.header(key, value)
            }
        }

        currentCoroutineContext().ensureActive()
        client.newCall(requestBuilder.build()).execute().use { response ->
            currentCoroutineContext().ensureActive()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("M3U8分片响应为空")
            val buffer = ByteArray(DEFAULT_SEGMENT_BUFFER_BYTES)
            body.byteStream().use { input ->
                resource.file.outputStream().buffered().use { output ->
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) {
                            break
                        }
                        currentCoroutineContext().ensureActive()
                        output.write(buffer, 0, read)
                        onBytes(read)
                    }
                    currentCoroutineContext().ensureActive()
                    output.flush()
                }
            }
        }
    }

    private suspend fun downloadSegmentedFile(
        client: OkHttpClient,
        url: String,
        headers: Map<String, String>,
        tempFile: File,
        totalBytes: Long,
        settings: DownloadSettingsState,
        runningRecord: InternalDownloadEntity,
        targetFile: File
    ): Long = coroutineScope {
        val partDirectory = File(tempFile.parentFile, "${tempFile.name}.segments")
        if (partDirectory.exists()) {
            partDirectory.deleteRecursively()
        }
        partDirectory.mkdirs()

        val splitSize = settings.chunkSizeKb.coerceIn(128, 12288) * 1024L
        val segments = buildList {
            var start = 0L
            var index = 0
            while (start < totalBytes) {
                val end = minOf(start + splitSize - 1L, totalBytes - 1L)
                add(
                    DownloadSegment(
                        index = index,
                        start = start,
                        end = end,
                        file = File(partDirectory, "part_$index")
                    )
                )
                start = end + 1L
                index += 1
            }
        }

        val downloadedBytes = AtomicLong(0L)
        val lastStoredAt = AtomicLong(0L)
        val limiter = Semaphore(settings.normalThreadCount.coerceIn(1, 64))

        try {
            segments.map { segment ->
                async(Dispatchers.IO) {
                    limiter.withPermit {
                        downloadSegmentWithRetry(
                            client = client,
                            url = url,
                            headers = headers,
                            segment = segment,
                            onBytes = { bytes ->
                                val current = downloadedBytes.addAndGet(bytes.toLong())
                                val now = System.currentTimeMillis()
                                val last = lastStoredAt.get()
                                if (now - last >= 350L && lastStoredAt.compareAndSet(last, now)) {
                                    dao.update(
                                        runningRecord.copy(
                                            downloadedBytes = current,
                                            localPath = targetFile.absolutePath,
                                            updatedAt = now
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }.awaitAll()

            currentCoroutineContext().ensureActive()
            tempFile.outputStream().buffered().use { output ->
                segments.sortedBy { it.index }.forEach { segment ->
                    currentCoroutineContext().ensureActive()
                    segment.file.inputStream().buffered().use { input ->
                        input.copyTo(output)
                    }
                }
                output.flush()
            }
            require(tempFile.length() == totalBytes) {
                "分段合并大小异常"
            }
            totalBytes
        } finally {
            partDirectory.deleteRecursively()
        }
    }

    private suspend fun downloadSegmentWithRetry(
        client: OkHttpClient,
        url: String,
        headers: Map<String, String>,
        segment: DownloadSegment,
        onBytes: (Int) -> Unit
    ) {
        var lastError: Exception? = null
        repeat(SEGMENT_DOWNLOAD_RETRY_COUNT) { attempt ->
            var reportedBytes = 0L
            try {
                downloadSegment(
                    client = client,
                    url = url,
                    headers = headers,
                    segment = segment,
                    onBytes = { bytes ->
                        reportedBytes += bytes.toLong()
                        onBytes(bytes)
                    }
                )
                return
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                lastError = error
                if (reportedBytes > 0L) {
                    onBytes(-reportedBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                }
                if (attempt < SEGMENT_DOWNLOAD_RETRY_COUNT - 1) {
                    segment.file.delete()
                    delay(250L * (attempt + 1))
                }
            }
        }
        throw lastError ?: IOException("分段下载失败")
    }

    private suspend fun downloadSegment(
        client: OkHttpClient,
        url: String,
        headers: Map<String, String>,
        segment: DownloadSegment,
        onBytes: (Int) -> Unit
    ) {
        currentCoroutineContext().ensureActive()
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Range", "bytes=${segment.start}-${segment.end}")
        headers.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank() && !key.equals("Range", ignoreCase = true)) {
                requestBuilder.header(key, value)
            }
        }

        currentCoroutineContext().ensureActive()
        client.newCall(requestBuilder.build()).execute().use { response ->
            currentCoroutineContext().ensureActive()
            if (response.code != 206) {
                throw IOException("HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("分段响应体为空")
            val expectedLength = segment.end - segment.start + 1L
            var downloaded = 0L
            val buffer = ByteArray(DEFAULT_SEGMENT_BUFFER_BYTES)

            body.byteStream().use { input ->
                segment.file.outputStream().buffered().use { output ->
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) {
                            break
                        }
                        currentCoroutineContext().ensureActive()
                        output.write(buffer, 0, read)
                        downloaded += read
                        onBytes(read)
                    }
                    currentCoroutineContext().ensureActive()
                    output.flush()
                }
            }

            currentCoroutineContext().ensureActive()
            if (downloaded != expectedLength) {
                throw IOException("分段大小异常")
            }
        }
    }

    private fun supportsRangeDownload(response: Response): Boolean {
        return response.header("Accept-Ranges")?.equals("bytes", ignoreCase = true) == true
    }

    private fun probeRangeDownload(
        client: OkHttpClient,
        url: String,
        headers: Map<String, String>
    ): Boolean {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Range", "bytes=0-0")
        headers.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank() && !key.equals("Range", ignoreCase = true)) {
                requestBuilder.header(key, value)
            }
        }
        return runCatching {
            client.newCall(requestBuilder.build()).execute().use { response ->
                response.code == 206 &&
                    response.header("Content-Range").orEmpty().startsWith("bytes ", ignoreCase = true)
            }
        }.getOrDefault(false)
    }

    private fun prepareHeaders(
        url: String,
        sourcePageUrl: String,
        inputHeaders: Map<String, String>
    ): Map<String, String> {
        val headers = LinkedHashMap<String, String>()
        inputHeaders.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank()) {
                headers[key] = value
            }
        }

        val referer = RemotePlaybackHeaders.get(headers, "Referer").orEmpty()
            .ifBlank { sourcePageUrl.trim() }
        if (referer.isNotBlank()) {
            headers.putIfAbsent("Referer", referer)
        }

        RemotePlaybackHeaders.deriveOrigin(referer)?.let {
            headers.putIfAbsent("Origin", it)
        }

        headers.putIfAbsent("User-Agent", RemotePlaybackHeaders.DEFAULT_USER_AGENT)
        headers.putIfAbsent("Accept", "*/*")
        headers.putIfAbsent("Accept-Encoding", "identity")

        if (RemotePlaybackHeaders.get(headers, "Cookie").isNullOrBlank()) {
            val cookie = runCatching {
                CookieManager.getInstance().getCookie(url).orEmpty()
                    .ifBlank { CookieManager.getInstance().getCookie(referer).orEmpty() }
            }.getOrDefault("")
            if (cookie.isNotBlank()) {
                headers["Cookie"] = cookie
            }
        }

        return headers
    }

    private fun buildHttpClient(settings: DownloadSettingsState): OkHttpClient {
        val maxDownloadThreads = settings.maxConcurrentTasks *
            maxOf(settings.normalThreadCount, settings.m3u8ThreadCount)
        val dispatcher = Dispatcher().apply {
            maxRequests = maxDownloadThreads.coerceIn(4, 128)
            maxRequestsPerHost = maxOf(settings.normalThreadCount, settings.m3u8ThreadCount).coerceIn(2, 64)
        }
        return OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .dispatcher(dispatcher)
            .connectionPool(ConnectionPool(dispatcher.maxRequests, 30, TimeUnit.SECONDS))
            .protocols(
                if (settings.enableHttp2) {
                    listOf(Protocol.HTTP_2, Protocol.HTTP_1_1)
                } else {
                    listOf(Protocol.HTTP_1_1)
                }
            )
            .build()
    }

    private fun shouldSchedule(status: String): Boolean {
        return status == InternalDownloadStatus.PENDING ||
            status == InternalDownloadStatus.RUNNING ||
            status == InternalDownloadStatus.UNKNOWN
    }

    private fun getDownloadDirectory(): File {
        val baseDirectory = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(appContext.filesDir, "downloads")
        return File(baseDirectory, "kiyori").apply {
            mkdirs()
        }
    }

    private fun resolveAvailableTargetFile(fileName: String): File {
        val directory = getDownloadDirectory()
        val baseName = fileName.substringBeforeLast('.', fileName)
        val extension = fileName.substringAfterLast('.', "")
        var index = 0
        while (true) {
            val candidateName = if (index == 0) {
                fileName
            } else if (extension.isBlank()) {
                "$baseName($index)"
            } else {
                "$baseName($index).$extension"
            }
            val candidate = File(directory, candidateName)
            val tempCandidate = File(directory, "$candidateName.part")
            if (!candidate.exists() && !tempCandidate.exists()) {
                return candidate
            }
            index += 1
        }
    }

    private fun resolveOpenUri(record: InternalDownloadEntity): Uri? {
        val localFile = resolveLocalFile(record)
        if (localFile != null && localFile.exists()) {
            return buildLocalContentUri(localFile)
        }
        if (record.localUri.isNotBlank()) {
            return Uri.parse(record.localUri)
        }
        return null
    }

    private fun resolveLocalFile(record: InternalDownloadEntity): File? {
        if (record.localPath.isNotBlank()) {
            return File(record.localPath)
        }
        val uri = record.localUri.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: return null
        return if (uri.scheme.equals("file", ignoreCase = true)) {
            File(uri.path.orEmpty())
        } else {
            null
        }
    }

    private fun m3u8PackageDirectoryFor(playlistFile: File): File {
        return File(playlistFile.parentFile, "${playlistFile.name}.files")
    }

    private fun directorySizeBytes(directory: File): Long {
        if (!directory.exists()) {
            return 0L
        }
        return directory.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    private fun hasLocalM3u8Package(record: InternalDownloadEntity): Boolean {
        val localFile = resolveLocalFile(record) ?: return false
        return m3u8PackageDirectoryFor(localFile).exists()
    }

    private fun buildLocalContentUri(file: File): Uri {
        return FileProvider.getUriForFile(
            appContext,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file
        )
    }

    private fun deleteTempFile(fileName: String) {
        val tempFile = File(getDownloadDirectory(), "$fileName.part")
        if (tempFile.exists()) {
            tempFile.delete()
        }
        m3u8PackageDirectoryFor(File(getDownloadDirectory(), fileName)).deleteRecursively()
    }

    private fun deleteDownloadedFile(record: InternalDownloadEntity): Boolean {
        resolveLocalFile(record)?.let { file ->
            m3u8PackageDirectoryFor(file).deleteRecursively()
            if (file.exists()) {
                return file.delete()
            }
        }
        val uri = record.localUri.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: return false
        return runCatching {
            when (uri.scheme?.lowercase()) {
                "content" -> appContext.contentResolver.delete(uri, null, null) >= 0
                "file" -> File(uri.path.orEmpty()).delete()
                else -> false
            }
        }.getOrDefault(false)
    }

    private fun renameLocalFile(file: File, targetFileName: String): File {
        require(file.exists()) { "当前文件不存在" }
        val renamedFile = File(file.parentFile, targetFileName)
        require(renamedFile.absolutePath != file.absolutePath) { "文件名未发生变化" }
        val oldPackageDirectory = m3u8PackageDirectoryFor(file)
        val newPackageDirectory = m3u8PackageDirectoryFor(renamedFile)
        require(!oldPackageDirectory.exists() || !newPackageDirectory.exists()) {
            "目标文件名的M3U8离线包已存在"
        }
        require(file.renameTo(renamedFile)) { "重命名失败" }
        if (oldPackageDirectory.exists()) {
            if (!oldPackageDirectory.renameTo(newPackageDirectory)) {
                renamedFile.renameTo(file)
                throw IOException("M3U8离线包重命名失败")
            }
        }
        return renamedFile
    }

    private fun copyToPublicDownloads(
        sourceFile: File,
        displayName: String,
        mimeType: String
    ): SavedDownloadLocation {
        require(sourceFile.exists()) { "源文件不存在" }
        return writeToPublicDownloads(displayName, mimeType) { output ->
            sourceFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }
    }

    private fun copyUriToPublicDownloads(
        sourceUri: Uri,
        displayName: String,
        mimeType: String
    ): SavedDownloadLocation {
        return writeToPublicDownloads(displayName, mimeType) { output ->
            openInputStream(sourceUri)?.use { input ->
                input.copyTo(output)
            } ?: throw IOException("无法读取当前文件")
        }
    }

    private fun writeToPublicDownloads(
        displayName: String,
        mimeType: String,
        writer: (java.io.OutputStream) -> Unit
    ): SavedDownloadLocation {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType.ifBlank { "application/octet-stream" })
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Kiyori")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val itemUri = appContext.contentResolver.insert(collection, values)
            ?: throw IOException("无法创建公开目录文件")

        try {
            appContext.contentResolver.openOutputStream(itemUri, "w")?.use(writer)
                ?: throw IOException("无法写入公开目录")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            appContext.contentResolver.update(itemUri, values, null, null)

            val document = DocumentFile.fromSingleUri(appContext, itemUri)
            return SavedDownloadLocation(
                uri = itemUri,
                absolutePath = "",
                displayName = document?.name ?: displayName,
                fileSizeBytes = document?.length()?.takeIf { it > 0L } ?: 0L
            )
        } catch (error: Exception) {
            appContext.contentResolver.delete(itemUri, null, null)
            throw error
        }
    }

    private fun postToast(message: String) {
        scope.launch(Dispatchers.Main) {
            android.widget.Toast.makeText(appContext, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun headersToJson(headers: Map<String, String>): String {
        val json = JSONObject()
        headers.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank()) {
                json.put(key, value)
            }
        }
        return json.toString()
    }

    private fun jsonToHeaders(headersJson: String): Map<String, String> {
        if (headersJson.isBlank()) {
            return emptyMap()
        }
        return runCatching {
            val json = JSONObject(headersJson)
            buildMap {
                val iterator = json.keys()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    put(key, json.optString(key, ""))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun isM3u8Resource(url: String, fileName: String, mimeType: String): Boolean {
        val normalizedMimeType = mimeType.substringBefore(';').trim().lowercase()
        if (normalizedMimeType.contains("mpegurl") || normalizedMimeType.contains("vnd.apple.mpegurl")) {
            return true
        }
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension == "m3u8" || url.substringBefore('?').lowercase().endsWith(".m3u8")) {
            return true
        }
        return false
    }

    private fun normalizeM3u8Content(
        content: String,
        baseUrl: String,
        headers: Map<String, String>,
        client: OkHttpClient,
        depth: Int = 0
    ): String {
        if (depth >= 4) {
            return content
        }

        val normalizedLines = mutableListOf<String>()
        var hasMediaSegments = false

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("#EXT-X-KEY:", ignoreCase = true) ||
                    line.startsWith("#EXT-X-MAP:", ignoreCase = true) -> {
                    normalizedLines += replaceM3u8UriAttribute(line, baseUrl)
                }

                line.isBlank() || line.startsWith("#") -> {
                    normalizedLines += rawLine
                }

                else -> {
                    val absoluteUrl = resolveUrl(baseUrl, line)
                    if (!hasMediaSegments && absoluteUrl.lowercase().contains(".m3u8")) {
                        return normalizeM3u8Content(
                            content = fetchTextContent(client, absoluteUrl, headers),
                            baseUrl = absoluteUrl,
                            headers = headers,
                            client = client,
                            depth = depth + 1
                        )
                    }
                    normalizedLines += absoluteUrl
                    hasMediaSegments = true
                }
            }
        }

        return normalizedLines.joinToString("\n")
    }

    private fun replaceM3u8UriAttribute(line: String, baseUrl: String): String {
        val pattern = Regex("URI=\"([^\"]+)\"")
        val match = pattern.find(line) ?: return line
        val source = match.groupValues[1]
        return line.replace(source, resolveUrl(baseUrl, source))
    }

    private fun extractM3u8UriAttribute(line: String): String? {
        return Regex("URI=\"([^\"]+)\"").find(line)?.groupValues?.getOrNull(1)
    }

    private fun extractM3u8LongAttribute(line: String, name: String): Long? {
        val pattern = Regex("(?:^|,)${Regex.escape(name)}=(\\d+)", RegexOption.IGNORE_CASE)
        return pattern.find(line)?.groupValues?.getOrNull(1)?.toLongOrNull()
    }

    private fun replaceM3u8UriAttributeWithValue(line: String, replacementUri: String): String {
        val pattern = Regex("URI=\"([^\"]+)\"")
        val match = pattern.find(line) ?: return line
        return line.replace(match.groupValues[1], replacementUri)
    }

    private fun guessM3u8ResourceExtension(value: String): String {
        val path = value.substringBefore('?').substringBefore('#')
        val extension = path.substringAfterLast('.', "")
            .takeIf { it.length in 1..8 && it.all { char -> char.isLetterOrDigit() } }
            ?.lowercase()
        return when {
            extension.isNullOrBlank() -> ".bin"
            extension == "jpeg" -> ".jpg"
            else -> ".$extension"
        }
    }

    private fun resolveUrl(baseUrl: String, value: String): String {
        return runCatching { URL(URL(baseUrl), value).toString() }
            .getOrDefault(value)
    }

    private fun fetchTextContent(
        client: OkHttpClient,
        url: String,
        headers: Map<String, String>
    ): String {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank()) {
                requestBuilder.header(key, value)
            }
        }
        return client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            response.body?.string() ?: throw IOException("响应体为空")
        }
    }

    private fun resolveMimeType(
        mimeType: String,
        fileName: String,
        url: String
    ): String {
        if (mimeType.isNotBlank()) {
            return mimeType.substringBefore(';').trim()
        }
        val fromName = URLConnection.guessContentTypeFromName(fileName)
        if (!fromName.isNullOrBlank()) {
            return fromName
        }
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
            ?.lowercase()
            .orEmpty()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension).orEmpty()
    }

    private fun sanitizeFileName(fileName: String): String {
        val sanitized = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        return sanitized.ifBlank { "download_${System.currentTimeMillis()}" }
    }

    private fun copyToCustomDirectory(
        sourceFile: File,
        displayName: String,
        mimeType: String,
        treeUriString: String
    ): SavedDownloadLocation {
        require(sourceFile.exists()) { "源文件不存在" }
        val originalSize = sourceFile.length()
        val treeUri = Uri.parse(treeUriString)
        val directory = DocumentFile.fromTreeUri(appContext, treeUri)
            ?: throw IOException("自定义下载目录不可用")
        require(directory.exists() && directory.isDirectory) { "自定义下载目录不可用" }

        val availableName = resolveAvailableDocumentName(directory, displayName)
        val targetDocument = directory.createFile(
            mimeType.ifBlank { "application/octet-stream" },
            availableName
        ) ?: throw IOException("无法在自定义目录创建文件")

        appContext.contentResolver.openOutputStream(targetDocument.uri, "w")?.use { output ->
            sourceFile.inputStream().use { input ->
                input.copyTo(output)
            }
        } ?: throw IOException("无法写入自定义目录")

        return SavedDownloadLocation(
            uri = targetDocument.uri,
            absolutePath = "",
            displayName = targetDocument.name ?: availableName,
            fileSizeBytes = targetDocument.length().takeIf { it > 0L } ?: originalSize
        )
    }

    private fun copyUriToCustomDirectory(
        sourceUri: Uri,
        displayName: String,
        mimeType: String,
        treeUriString: String
    ): SavedDownloadLocation {
        val treeUri = Uri.parse(treeUriString)
        val directory = DocumentFile.fromTreeUri(appContext, treeUri)
            ?: throw IOException("自定义下载目录不可用")
        require(directory.exists() && directory.isDirectory) { "自定义下载目录不可用" }

        val availableName = resolveAvailableDocumentName(directory, displayName)
        val targetDocument = directory.createFile(
            mimeType.ifBlank { "application/octet-stream" },
            availableName
        ) ?: throw IOException("无法在自定义目录创建文件")

        try {
            appContext.contentResolver.openOutputStream(targetDocument.uri, "w")?.use { output ->
                openInputStream(sourceUri)?.use { input ->
                    input.copyTo(output)
                } ?: throw IOException("无法读取当前文件")
            } ?: throw IOException("无法写入自定义目录")
        } catch (error: Exception) {
            runCatching { appContext.contentResolver.delete(targetDocument.uri, null, null) }
            throw error
        }

        return SavedDownloadLocation(
            uri = targetDocument.uri,
            absolutePath = "",
            displayName = targetDocument.name ?: availableName,
            fileSizeBytes = targetDocument.length().takeIf { it > 0L } ?: 0L
        )
    }

    private fun openInputStream(uri: Uri): java.io.InputStream? {
        return when (uri.scheme?.lowercase()) {
            "content" -> appContext.contentResolver.openInputStream(uri)
            "file" -> File(uri.path.orEmpty()).takeIf { it.exists() }?.inputStream()
            else -> null
        }
    }

    private fun resolveAvailableDocumentName(directory: DocumentFile, displayName: String): String {
        val baseName = displayName.substringBeforeLast('.', displayName)
        val extension = displayName.substringAfterLast('.', "")
        var index = 0
        while (true) {
            val candidate = when {
                index == 0 -> displayName
                extension.isBlank() -> "$baseName($index)"
                else -> "$baseName($index).$extension"
            }
            if (directory.findFile(candidate) == null) {
                return candidate
            }
            index += 1
        }
    }

    private fun isPlayableMedia(mimeType: String, fileName: String): Boolean {
        val normalizedMimeType = mimeType.lowercase()
        if (normalizedMimeType.startsWith("video/") || normalizedMimeType.startsWith("audio/")) {
            return true
        }
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in setOf(
            "mp4", "mkv", "webm", "m3u8", "ts", "m4v", "mov", "avi", "flv", "wmv",
            "mp3", "m4a", "aac", "ogg", "wav", "flac"
        )
    }

    private fun isApkPackage(mimeType: String, fileName: String): Boolean {
        if (mimeType.equals("application/vnd.android.package-archive", ignoreCase = true)) {
            return true
        }
        return fileName.substringAfterLast('.', "").equals("apk", ignoreCase = true)
    }

    private fun scheduleAutoCleanApk(record: InternalDownloadEntity) {
        if (!preferencesRepository.getSettings().autoCleanApk) {
            return
        }
        scope.launch {
            delay(90_000L)
            removeRecordInternal(record, deleteFile = true)
        }
    }

    private data class SavedDownloadLocation(
        val uri: Uri,
        val absolutePath: String,
        val displayName: String,
        val fileSizeBytes: Long
    )

    private data class DownloadSegment(
        val index: Int,
        val start: Long,
        val end: Long,
        val file: File
    )

    private data class M3u8PackageResource(
        val url: String,
        val file: File
    )

    private data class M3u8MediaPlaylist(
        val url: String,
        val content: String
    )

    companion object {
        private const val SEGMENT_DOWNLOAD_RETRY_COUNT = 5
        private const val DEFAULT_SEGMENT_BUFFER_BYTES = 64 * 1024

        @Volatile
        private var INSTANCE: InternalDownloadManager? = null

        fun initialize(context: Context) {
            getInstance(context).recoverInterruptedDownloads()
        }

        fun getInstance(context: Context): InternalDownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InternalDownloadManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
