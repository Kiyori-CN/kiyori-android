package com.android.kiyori.media

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoFileParcelable(
    val uri: String,
    val name: String,
    val path: String,
    val size: Long,
    val duration: Long,
    val dateAdded: Long
) : Parcelable

