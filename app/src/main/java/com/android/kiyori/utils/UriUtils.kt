package com.android.kiyori.utils

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log

/**
 * URI 工具类。
 * content:// 播放必须保留文件描述符交给 MPV，避免 scoped storage 下真实路径无法被 native 层打开。
 */
object UriUtils {
    private const val TAG = "UriUtils"
    private val detachedContentFds = mutableSetOf<Int>()

    fun Uri.openContentFd(context: Context): String? {
        var pfd: ParcelFileDescriptor? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(this, "r")
            val fd = pfd?.detachFd() ?: return null
            pfd.close()
            pfd = null
            synchronized(detachedContentFds) {
                detachedContentFds.add(fd)
            }
            "fd://$fd"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open content URI: $this", e)
            null
        } finally {
            try {
                pfd?.close()
            } catch (closeError: Exception) {
                Log.w(TAG, "Failed to close content descriptor shell: ${closeError.message}")
            }
        }
    }

    fun closeDetachedContentFds() {
        val fds = synchronized(detachedContentFds) {
            detachedContentFds.toList().also {
                detachedContentFds.clear()
            }
        }
        fds.forEach(::closeDetachedFd)
    }

    fun closeDetachedContentFdsExcept(retainedFd: Int?) {
        if (retainedFd == null) {
            closeDetachedContentFds()
            return
        }
        val fds = synchronized(detachedContentFds) {
            detachedContentFds
                .filter { it != retainedFd }
                .also { closing -> detachedContentFds.removeAll(closing.toSet()) }
        }
        fds.forEach(::closeDetachedFd)
    }

    fun retainedFdFromResolvedPath(path: String?): Int? {
        if (path == null || !path.startsWith("fd://")) {
            return null
        }
        return path.removePrefix("fd://").toIntOrNull()
    }

    fun Uri.resolveUri(context: Context): String? {
        return when (scheme) {
            "file" -> path
            "content" -> openContentFd(context)
            "http", "https", "rtsp", "rtmp", "rtmps" -> toString()
            else -> {
                Log.e(TAG, "Unknown URI scheme: $scheme")
                null
            }
        }
    }

    fun Uri.getFolderName(): String {
        return try {
            val path = this.path ?: return "未知文件夹"
            val segments = path.split("/")
            if (segments.size > 1) {
                segments[segments.size - 2]
            } else {
                "未知文件夹"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get folder name", e)
            "未知文件夹"
        }
    }

    private fun closeDetachedFd(fd: Int) {
        try {
            ParcelFileDescriptor.adoptFd(fd).close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to close detached content fd: $fd", e)
        }
    }
}
