package com.fam4k007.videoplayer.dandanplay

import com.google.gson.annotations.SerializedName

/**
 * DanDanPlay API 数据模型
 */

// 搜索动漫请求
data class SearchAnimeRequest(
    val anime: String,
    val episode: String? = null
)

// 搜索动漫响应
data class SearchAnimeResponse(
    @SerializedName("hasMore")
    val hasMore: Boolean,
    @SerializedName("animes")
    val animes: List<AnimeSearchInfo>
)

// 动漫信息
data class AnimeSearchInfo(
    @SerializedName("animeId")
    val animeId: Int,
    @SerializedName("animeTitle")
    val animeTitle: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("typeDescription")
    val typeDescription: String,
    @SerializedName("episodes")
    val episodes: List<EpisodeInfo>
)

// 剧集信息
data class EpisodeInfo(
    @SerializedName("episodeId")
    val episodeId: Int,
    @SerializedName("episodeTitle")
    val episodeTitle: String
)

// 弹幕响应
data class DanmakuResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("comments")
    val comments: List<DanmakuComment>
)

// 弹幕评论
data class DanmakuComment(
    @SerializedName("cid")
    val cid: Long,
    @SerializedName("p")
    val p: String,  // 格式: "时间,模式,颜色,用户ID"
    @SerializedName("m")
    val m: String   // 弹幕内容
)
