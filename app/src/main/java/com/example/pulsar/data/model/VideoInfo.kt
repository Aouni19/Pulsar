package com.example.pulsar.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class VideoInfo(
    val id: String,
    val title: String,
    val uploader: String? = "Unknown Channel",
    val thumbnail: String? = null,
    val duration: Int? = 0, // In seconds
    @SerialName("view_count") val viewCount: Long? = 0,
    @SerialName("upload_date") val uploadDate: String? = "", // Format: YYYYMMDD
    val formats: List<Format>
)