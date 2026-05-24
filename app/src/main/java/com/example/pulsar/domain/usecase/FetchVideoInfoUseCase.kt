package com.example.pulsar.domain.usecase

import android.content.Context
import com.example.pulsar.data.model.Format
import com.example.pulsar.data.model.VideoInfo
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@Serializable
private data class OEmbedResponse(
    val title: String? = null,
    val author_name: String? = null,
    val thumbnail_url: String? = null
)

/**
 * Use case for retrieving video metadata and available formats from a given URL.
 */
class FetchVideoInfoUseCase @Inject constructor(
    // Inject the Application Context so we can initialize the library
    @ApplicationContext private val context: Context
) {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend operator fun invoke(url: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        try {
            // 1. Fast Path for YouTube - Bypasses slow yt-dlp initialization and extraction
            if (url.contains("youtube.com") || url.contains("youtu.be")) {
                try {
                    val videoId = extractYoutubeId(url) ?: "youtube_video"
                    val canonicalUrl = "https://www.youtube.com/watch?v=$videoId"
                    
                    // Try HTML Scraping first (more data: duration, views, date)
                    var fastVideoInfo: VideoInfo? = null
                    try {
                        val connection = URL(canonicalUrl).openConnection() as HttpURLConnection
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        
                        if (connection.responseCode == 200) {
                            val html = connection.inputStream.bufferedReader().use { reader ->
                                val sb = StringBuilder()
                                val charBuffer = CharArray(8192)
                                var totalCharsRead = 0
                                // Read up to 2MB of text to ensure we hit the metadata JSON (at 600-800KB)
                                while (totalCharsRead < 2000000) {
                                    val read = reader.read(charBuffer)
                                    if (read == -1) break
                                    sb.append(charBuffer, 0, read)
                                    totalCharsRead += read
                                }
                                sb.toString()
                            }

                            val title = "\"title\":\\{\"simpleText\":\"([^\"]+)\"".toRegex().find(html)?.groupValues?.get(1)
                                ?: "\"title\":\"([^\"]+)\"".toRegex().find(html)?.groupValues?.get(1)
                                ?: "<title>(.*) - YouTube</title>".toRegex().find(html)?.groupValues?.get(1)
                            
                            val uploader = "\"ownerChannelName\":\"([^\"]+)\"".toRegex().find(html)?.groupValues?.get(1)
                                ?: "\"author\":\"([^\"]+)\"".toRegex().find(html)?.groupValues?.get(1)
                                
                            val duration = "\"lengthSeconds\":\"(\\d+)\"".toRegex().find(html)?.groupValues?.get(1)?.toIntOrNull() 
                                ?: "\"length_seconds\":(\\d+)".toRegex().find(html)?.groupValues?.get(1)?.toIntOrNull()
                                ?: 0
                                
                            val viewCount = "\"viewCount\":\"(\\d+)\"".toRegex().find(html)?.groupValues?.get(1)?.toLongOrNull()
                                ?: "\"view_count\":(\\d+)".toRegex().find(html)?.groupValues?.get(1)?.toLongOrNull()
                                ?: 0L
                            
                            // More robust date extraction: 2005-04-23
                            var dateMatch = "\"uploadDate\":\"(\\d{4})-(\\d{2})-(\\d{2})".toRegex().find(html)
                            if (dateMatch == null) {
                                dateMatch = "\"publishDate\":\"(\\d{4})-(\\d{2})-(\\d{2})".toRegex().find(html)
                            }
                            
                            val uploadDate = if (dateMatch != null) {
                                "${dateMatch.groupValues[1]}${dateMatch.groupValues[2]}${dateMatch.groupValues[3]}"
                            } else ""

                            val thumbnail = "\"thumbnail\":\\{\"thumbnails\":\\[\\{\"url\":\"([^\"]+)\"".toRegex().find(html)?.groupValues?.get(1)
                                ?: "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"

                            if (title != null) {
                                fastVideoInfo = VideoInfo(
                                    id = videoId,
                                    title = title.replace("\\u0026", "&").replace("\\\"", "\""),
                                    uploader = uploader?.replace("\\u0026", "&") ?: "Unknown Channel",
                                    thumbnail = thumbnail.replace("\\u0026", "&"),
                                    duration = duration,
                                    viewCount = viewCount,
                                    uploadDate = uploadDate,
                                    formats = emptyList()
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Fallback to OEmbed if HTML scraping failed or is missing critical info
                    if (fastVideoInfo == null || fastVideoInfo.title == "Unknown Title") {
                        val oembedUrl = "https://www.youtube.com/oembed?url=$url&format=json"
                        val connection = URL(oembedUrl).openConnection() as HttpURLConnection
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        
                        if (connection.responseCode == 200) {
                            val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                            val oEmbedData = jsonParser.decodeFromString<OEmbedResponse>(responseString)
                            
                            val oEmbedInfo = VideoInfo(
                                id = videoId,
                                title = oEmbedData.title ?: "Unknown Title",
                                uploader = oEmbedData.author_name ?: "Unknown Channel",
                                thumbnail = oEmbedData.thumbnail_url,
                                duration = fastVideoInfo?.duration ?: 0,
                                viewCount = fastVideoInfo?.viewCount ?: 0,
                                uploadDate = fastVideoInfo?.uploadDate ?: "",
                                formats = emptyList()
                            )
                            fastVideoInfo = oEmbedInfo
                        }
                    }
                    
                    if (fastVideoInfo != null) {
                        val duration = fastVideoInfo.duration ?: 0
                        // Standard YouTube Formats to bypass slow format extraction
                        val standardFormats = listOf(
                            Format(formatId = "bestvideo[height<=2160]", resolution = "4K", ext = "mp4", vcodec = "avc1", acodec = "mp4a", filesizeApprox = estimateSize("4K", duration)),
                            Format(formatId = "bestvideo[height<=1080]", resolution = "1080p", ext = "mp4", vcodec = "avc1", acodec = "mp4a", filesizeApprox = estimateSize("1080p", duration)),
                            Format(formatId = "bestvideo[height<=720]", resolution = "720p", ext = "mp4", vcodec = "avc1", acodec = "mp4a", filesizeApprox = estimateSize("720p", duration)),
                            Format(formatId = "bestvideo[height<=480]", resolution = "480p", ext = "mp4", vcodec = "avc1", acodec = "mp4a", filesizeApprox = estimateSize("480p", duration)),
                            Format(formatId = "bestvideo[height<=360]", resolution = "360p", ext = "mp4", vcodec = "avc1", acodec = "mp4a", filesizeApprox = estimateSize("360p", duration)),
                            Format(formatId = "bestaudio[abr<=320]/bestaudio", resolution = "High (320k)", ext = "mp3", vcodec = "none", acodec = "mp3", abr = 320.0, filesizeApprox = estimateSize("High", duration)),
                            Format(formatId = "bestaudio[abr<=128]/bestaudio", resolution = "Medium (128k)", ext = "mp3", vcodec = "none", acodec = "mp3", abr = 128.0, filesizeApprox = estimateSize("Medium", duration)),
                            Format(formatId = "bestaudio[abr<=64]/bestaudio", resolution = "Low (64k)", ext = "mp3", vcodec = "none", acodec = "mp3", abr = 64.0, filesizeApprox = estimateSize("Low", duration))
                        )
                        
                        return@withContext Result.success(fastVideoInfo.copy(formats = standardFormats))
                    }
                } catch (e: Exception) {
                    // Fallback to yt-dlp if fast path fails completely
                    e.printStackTrace()
                }
            }

            // 2. Slow Path (yt-dlp) for non-YouTube URLs or if fast path failed
            val request = YoutubeDLRequest(url).apply {
                addOption("-J")
                addOption("--no-playlist") // Prevent downloading entire playlist metadata
                addOption("--force-ipv4")  // Bypass slow IPv6 resolution timeouts on older devices
                addOption("--no-warnings") // Reduce console output processing overhead
            }
            val response = YoutubeDL.getInstance().execute(request, null, null)
            val output = response.out

            val videoInfo = jsonParser.decodeFromString<VideoInfo>(output)

            // 1. Intercept and mold the raw formats into our perfect 5-item list
                val cleanFormats = videoInfo.formats
                    .filter { !it.formatId.contains("sb") } // Exclude storyboards
                    .map { format ->
                        // Extract "1080" from "1920x1080"
                        val height = format.resolution.substringAfterLast("x").toIntOrNull() ?: 0
                        val cleanRes = when {
                            format.isAudioOnly -> {
                                when {
                                    (format.abr ?: 0.0) >= 256 -> "High (320k)"
                                    (format.abr ?: 0.0) >= 128 -> "Medium (128k)"
                                    else -> "Low (64k)"
                                }
                            }
                            height >= 2160 -> "4K"
                            height >= 1080 -> "1080p"
                            height >= 720 -> "720p"
                            height >= 480 -> "480p"
                            height >= 360 -> "360p"
                            else -> "Ignore"
                        }
                        format.copy(resolution = cleanRes)
                    }
                    .filter { it.resolution != "Ignore" } // Filter unsupported resolutions
                    .distinctBy { it.resolution } // Deduplicate formats by resolution
                    .sortedByDescending {
                        // Sort resolutions sequentially
                        when {
                            it.resolution == "4K" -> 2160
                            it.resolution == "1080p" -> 1080
                            it.resolution == "720p" -> 720
                            it.resolution == "480p" -> 480
                            it.resolution == "360p" -> 360
                            it.resolution.contains("High") -> 320
                            it.resolution.contains("Medium") -> 128
                            it.resolution.contains("Low") -> 64
                            else -> 0 
                        }
                    }

                Result.success(videoInfo.copy(formats = cleanFormats))

        } catch (e: Exception) {
            e.printStackTrace()
            val trueError = e.cause?.message ?: e.message ?: "Unknown error occurred"
            Result.failure(Exception(trueError))
        }
    }

    private fun estimateSize(resolution: String, duration: Int): Long {
        if (duration <= 0) return 0L
        val bitrate = when {
            resolution == "4K" -> 15_000_000L
            resolution == "1080p" -> 4_000_000L
            resolution == "720p" -> 2_000_000L
            resolution == "480p" -> 800_000L
            resolution == "360p" -> 400_000L
            resolution.contains("High") -> 320_000L
            resolution.contains("Medium") -> 128_000L
            resolution.contains("Low") -> 64_000L
            else -> 0L
        }
        return (bitrate * duration) / 8
    }
    
    private fun extractYoutubeId(url: String): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\\u200C\\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
        val compiledPattern = java.util.regex.Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(url)
        return if (matcher.find()) {
            matcher.group()
        } else null
    }
}