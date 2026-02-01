package com.fam4k007.videoplayer.dandanplay

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * DanDanPlay API 服务
 * API 文档: https://api.dandanplay.net/swagger/index.html
 */
class DanDanPlayApi {
    companion object {
        private const val TAG = "DanDanPlayApi"
        private const val BASE_URL = "https://api.dandanplay.net"
        
        // API 密钥 - 从BuildConfig读取
        private val APP_ID = com.fam4k007.videoplayer.BuildConfig.DANDANPLAY_APP_ID
        private val APP_SECRET = com.fam4k007.videoplayer.BuildConfig.DANDANPLAY_APP_SECRET
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * 生成签名（签名验证模式）
     * 算法: base64(sha256(AppId + Timestamp + Path + AppSecret))
     */
    private fun generateSignature(timestamp: Long, path: String): String {
        val data = "$APP_ID$timestamp$path$APP_SECRET"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }

    /**
     * 搜索动漫
     * GET /api/v2/search/episodes
     * @param keyword 搜索关键词
     * @return 搜索结果
     */
    suspend fun searchAnime(keyword: String): Result<SearchAnimeResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
                // API路径（用于签名计算）
                val path = "/api/v2/search/episodes"
                val url = "$BASE_URL$path?anime=$encodedKeyword"
                
                // 生成时间戳和签名
                val timestamp = System.currentTimeMillis() / 1000
                val signature = generateSignature(timestamp, path)
                
                Log.d(TAG, "========== DanDanPlay API 搜索 ==========")
                Log.d(TAG, "原始关键词: $keyword")
                Log.d(TAG, "编码后关键词: $encodedKeyword")
                Log.d(TAG, "API路径: $path")
                Log.d(TAG, "完整URL: $url")
                Log.d(TAG, "时间戳: $timestamp")
                Log.d(TAG, "签名: $signature")

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Accept", "application/json")
                    .addHeader("Accept-Encoding", "gzip, deflate")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    // 添加DanDanPlay API必需的身份验证头（签名验证模式）
                    .addHeader("X-AppId", APP_ID)
                    .addHeader("X-Timestamp", timestamp.toString())
                    .addHeader("X-Signature", signature)
                    .build()

                Log.d(TAG, "请求头:")
                request.headers.forEach { (name, value) ->
                    Log.d(TAG, "  $name: $value")
                }

                val response = client.newCall(request).execute()
                
                Log.d(TAG, "响应状态码: ${response.code}")
                Log.d(TAG, "响应消息: ${response.message}")
                Log.d(TAG, "Content-Encoding: ${response.header("Content-Encoding")}")
                Log.d(TAG, "响应头:")
                response.headers.forEach { (name, value) ->
                    Log.d(TAG, "  $name: $value")
                }
                
                // 检查错误信息头（403时会有详细错误信息）
                response.header("X-Error-Message")?.let { errorMsg ->
                    Log.e(TAG, "服务器错误信息: $errorMsg")
                }
                
                // 处理 GZIP 压缩（与getDanmaku一致）
                val responseBody = if (response.header("Content-Encoding") == "gzip") {
                    Log.d(TAG, "解压GZIP响应")
                    val gzipStream = GZIPInputStream(response.body?.byteStream())
                    val outputStream = ByteArrayOutputStream()
                    gzipStream.copyTo(outputStream)
                    outputStream.toString("UTF-8")
                } else {
                    response.body?.string()
                }
                
                Log.d(TAG, "响应体长度: ${responseBody?.length ?: 0}")
                Log.d(TAG, "响应体内容: ${responseBody?.take(500)}")  // 只打印前500字符

                if (response.isSuccessful && responseBody != null) {
                    val searchResponse = gson.fromJson(responseBody, SearchAnimeResponse::class.java)
                    Log.d(TAG, "解析成功，找到 ${searchResponse.animes.size} 个结果")
                    Result.success(searchResponse)
                } else {
                    val errorMsg = "搜索失败: ${response.code} - ${response.message}"
                    Log.e(TAG, errorMsg)
                    Log.e(TAG, "错误响应体: $responseBody")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "搜索异常", e)
                Log.e(TAG, "异常类型: ${e.javaClass.simpleName}")
                Log.e(TAG, "异常消息: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    /**
     * 获取弹幕
     * GET /api/v2/comment/{episodeId}
     * @param episodeId 剧集ID
     * @return 弹幕列表
     */
    suspend fun getDanmaku(episodeId: Int): Result<DanmakuResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // API路径（用于签名计算）
                val path = "/api/v2/comment/$episodeId"
                val url = "$BASE_URL$path?withRelated=true"
                
                // 生成时间戳和签名
                val timestamp = System.currentTimeMillis() / 1000
                val signature = generateSignature(timestamp, path)
                
                Log.d(TAG, "========== DanDanPlay API 获取弹幕 ==========")
                Log.d(TAG, "剧集ID: $episodeId")
                Log.d(TAG, "API路径: $path")
                Log.d(TAG, "完整URL: $url")
                Log.d(TAG, "时间戳: $timestamp")
                Log.d(TAG, "签名: $signature")

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Accept", "application/json")
                    .addHeader("Accept-Encoding", "gzip, deflate")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    // 添加DanDanPlay API必需的身份验证头（签名验证模式）
                    .addHeader("X-AppId", APP_ID)
                    .addHeader("X-Timestamp", timestamp.toString())
                    .addHeader("X-Signature", signature)
                    .build()

                Log.d(TAG, "请求头:")
                request.headers.forEach { (name, value) ->
                    Log.d(TAG, "  $name: $value")
                }

                val response = client.newCall(request).execute()
                
                Log.d(TAG, "响应状态码: ${response.code}")
                Log.d(TAG, "响应消息: ${response.message}")
                Log.d(TAG, "Content-Encoding: ${response.header("Content-Encoding")}")
                
                // 检查错误信息头
                response.header("X-Error-Message")?.let { errorMsg ->
                    Log.e(TAG, "服务器错误信息: $errorMsg")
                }
                
                // 处理 GZIP 压缩
                val responseBody = if (response.header("Content-Encoding") == "gzip") {
                    Log.d(TAG, "解压GZIP响应")
                    val gzipStream = GZIPInputStream(response.body?.byteStream())
                    val outputStream = ByteArrayOutputStream()
                    gzipStream.copyTo(outputStream)
                    outputStream.toString("UTF-8")
                } else {
                    response.body?.string()
                }

                Log.d(TAG, "响应体长度: ${responseBody?.length ?: 0}")
                Log.d(TAG, "响应体内容: ${responseBody?.take(500)}")

                if (response.isSuccessful && responseBody != null) {
                    val danmakuResponse = gson.fromJson(responseBody, DanmakuResponse::class.java)
                    Log.d(TAG, "解析成功，获得 ${danmakuResponse.count} 条弹幕")
                    Result.success(danmakuResponse)
                } else {
                    val errorMsg = "获取弹幕失败: ${response.code} - ${response.message}"
                    Log.e(TAG, errorMsg)
                    Log.e(TAG, "错误响应体: $responseBody")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取弹幕异常", e)
                Log.e(TAG, "异常类型: ${e.javaClass.simpleName}")
                Log.e(TAG, "异常消息: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    /**
     * 将弹幕转换为 Bilibili XML 格式
     */
    fun convertToXml(danmakuResponse: DanmakuResponse): String {
        val xmlBuilder = StringBuilder()
        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        xmlBuilder.append("<i>\n")
        xmlBuilder.append("  <chatserver>chat.bilibili.com</chatserver>\n")
        xmlBuilder.append("  <chatid>0</chatid>\n")
        xmlBuilder.append("  <mission>0</mission>\n")
        xmlBuilder.append("  <maxlimit>8000</maxlimit>\n")
        xmlBuilder.append("  <state>0</state>\n")
        xmlBuilder.append("  <real_name>0</real_name>\n")
        xmlBuilder.append("  <source>DanDanPlay</source>\n")

        for (comment in danmakuResponse.comments) {
            try {
                // p 格式: "时间,模式,字号,颜色,时间戳,弹幕池,用户hash,弹幕ID"
                // DanDanPlay p 格式: "时间,模式,颜色,用户ID"
                val pParts = comment.p.split(",")
                if (pParts.size >= 3) {
                    val time = pParts[0]
                    val mode = pParts[1]
                    val color = if (pParts.size > 2) pParts[2] else "16777215"
                    
                    // 转换为 Bilibili 格式
                    // 格式: 时间,模式,字号,颜色,时间戳,弹幕池,用户hash,弹幕ID
                    val p = "$time,$mode,25,$color,${System.currentTimeMillis() / 1000},0,0,${comment.cid}"
                    val content = comment.m
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&apos;")
                    
                    xmlBuilder.append("  <d p=\"$p\">$content</d>\n")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error converting danmaku: ${comment.m}", e)
            }
        }

        xmlBuilder.append("</i>")
        return xmlBuilder.toString()
    }
}
