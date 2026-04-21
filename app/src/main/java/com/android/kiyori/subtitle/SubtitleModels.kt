package com.android.kiyori.subtitle

/**
 * 字幕信息数据类
 */
data class SubtitleInfo(
    val id: String? = null,
    val url: String,
    val fileName: String? = null,
    val language: String? = null,
    val languageDisplay: String? = null,
    val format: String? = null,
    val source: String? = null,
    val release: String? = null,
    val isHearingImpaired: Boolean = false,
    val downloadCount: Int? = null
) {
    val displayName: String get() = fileName ?: release ?: "未知字幕"
    val displayLanguage: String get() = languageDisplay ?: language ?: "未知语言"
}

/**
 * 字幕源枚举
 */
object SubtitleSources {
    val ALL = mapOf(
        "all" to "全部来源",
        "subdl" to "SubDL",
        "subf2m" to "Subf2m",
        "opensubtitles" to "OpenSubtitles",
        "podnapisi" to "Podnapisi",
        "gestdown" to "Gestdown",
        "animetosho" to "AnimeTosho"
    )
}

/**
 * 字幕格式枚举
 */
object SubtitleFormats {
    val ALL = mapOf(
        "srt" to "SRT",
        "ass" to "ASS",
        "ssa" to "SSA",
        "vtt" to "VTT"
    )
}

/**
 * 字幕语言枚举（精简版）
 */
object SubtitleLanguages {
    val ALL = mapOf(
        "all" to "全部语言",
        "zh" to "简体中文",
        "zh-TW" to "繁體中文",
        "en" to "English",
        "ja" to "日本語",
        "ko" to "한국어",
        "fr" to "Français",
        "de" to "Deutsch",
        "es" to "Español",
        "ru" to "Русский",
        "ar" to "العربية",
        "pt" to "Português",
        "it" to "Italiano",
        "th" to "ไทย",
        "vi" to "Tiếng Việt"
    )
}

/**
 * 搜索选项
 */
data class SearchOptions(
    val languages: Set<String> = setOf("zh", "en"),  // 默认中英文
    val sources: Set<String> = setOf("all"),
    val formats: Set<String> = setOf("srt", "ass")
)

/**
 * TMDB搜索结果
 */
data class TmdbMediaResult(
    val id: Int,
    val mediaType: String,  // "movie" or "tv"
    val title: String,
    val releaseYear: String? = null,
    val poster: String? = null,
    val overview: String? = null
) {
    val displayTitle: String get() = if (releaseYear != null) "$title ($releaseYear)" else title
}

