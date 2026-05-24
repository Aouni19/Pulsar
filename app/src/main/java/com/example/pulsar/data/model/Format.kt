package com.example.pulsar.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Format(
    @SerialName("format_id") val formatId: String,
    val resolution: String = "audio",
    val ext: String,
    val vcodec: String = "none",
    val acodec: String = "none",
    val abr: Double? = null,
    val filesize: Long? = null,
    @SerialName("filesize_approx") val filesizeApprox: Long? = null
) {
    // Helper properties to easily filter formats later in the UI
    val isAudioOnly: Boolean get() = vcodec == "none" && acodec != "none"
    val isVideoOnly: Boolean get() = vcodec != "none" && acodec == "none"
    val actualFileSize: Long get() = filesize ?: filesizeApprox ?: 0L
    val isApproximate: Boolean get() = filesize == null && filesizeApprox != null
}