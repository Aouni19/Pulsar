package com.example.pulsar.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import coil3.compose.AsyncImage
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pulsar.R
import com.example.pulsar.data.model.Format
import com.example.pulsar.data.model.VideoInfo
import com.example.pulsar.ui.theme.bouncyClick
import android.util.Log
import com.example.pulsar.ui.components.MorphingSegmentedButton
import java.text.SimpleDateFormat
import java.util.Locale

enum class SheetStep { DETAILS, QUALITY }
enum class FormatType { VIDEO, AUDIO }

/**
 * Bottom sheet displaying video details and options for format and quality selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsBottomSheet(
    videoInfo: VideoInfo,
    preferredVideoQuality: String = "1080p",
    preferredAudioQuality: String = "High (320k)",
    onDismiss: () -> Unit,
    onDownload: (Format, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentStep by remember { mutableStateOf(SheetStep.DETAILS) }
    var selectedFormatType by remember { mutableStateOf(FormatType.VIDEO) }
    
    // Logic to find the best matching index based on preference
    val filteredFormats = remember(selectedFormatType, videoInfo.formats) {
        val formats = if (selectedFormatType == FormatType.VIDEO) {
            videoInfo.formats.filter { !it.isAudioOnly }
        } else {
            videoInfo.formats.filter { it.isAudioOnly }
        }
        Log.d("PulsarDebug", "Filtered Formats: ${formats.map { it.resolution }}")
        formats
    }

    val preferredQuality = if (selectedFormatType == FormatType.VIDEO) preferredVideoQuality else preferredAudioQuality

    val initialIndex = remember(filteredFormats, preferredQuality) {
        val index = filteredFormats.indexOfFirst { it.resolution == preferredQuality }
        Log.d("PulsarDebug", "Initial Index Match for $preferredQuality: $index")
        if (index != -1) index else 0
    }

    var selectedQualityIndex by remember(initialIndex) { mutableIntStateOf(initialIndex) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Crossfade(targetState = currentStep, label = "sheet_transition") { step ->
            when (step) {
                SheetStep.DETAILS -> {
                    DetailsStep(
                        videoInfo = videoInfo,
                        selectedFormatType = selectedFormatType,
                        onFormatChange = { selectedFormatType = it },
                        onNext = {
                            currentStep = SheetStep.QUALITY
                        }
                    )
                }
                SheetStep.QUALITY -> {
                    QualityStep(
                        filteredFormats = filteredFormats,
                        formatType = selectedFormatType,
                        selectedIndex = selectedQualityIndex,
                        onIndexChange = { 
                            Log.d("PulsarDebug", "Manual index change to: $it")
                            selectedQualityIndex = it 
                        },
                        onBack = { currentStep = SheetStep.DETAILS },
                        onDownload = { format ->
                            onDownload(format, selectedFormatType == FormatType.VIDEO)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsStep(
    videoInfo: VideoInfo,
    selectedFormatType: FormatType,
    onFormatChange: (FormatType) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)
    ) {
        // Thumbnail
        AsyncImage(
            model = videoInfo.thumbnail,
            contentDescription = "Thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Title and Channel (Following Draft exactly)
        Text(
            text = videoInfo.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily(Font(R.font.noto_serif)),
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = videoInfo.uploader ?: "Unknown Channel",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stat Badges Row - Added horizontal scroll and arrangement
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatBadge(icon = Icons.Default.AccessTime, text = formatDuration(videoInfo.duration ?: 0))
            StatBadge(icon = Icons.Default.CalendarMonth, text = formatDate(videoInfo.uploadDate ?: ""))
            StatBadge(icon = Icons.Default.BarChart, text = formatViews(videoInfo.viewCount ?: 0))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Format Selection
        Text(
            text = "Choose Format",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        val options = listOf("Video", "Audio Only")
        MorphingSegmentedButton(
            options = options,
            selectedOption = if (selectedFormatType == FormatType.VIDEO) "Video" else "Audio Only",
            onOptionSelect = { label ->
                onFormatChange(if (label == "Video") FormatType.VIDEO else FormatType.AUDIO)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Select Quality Button
        val buttonColor = MaterialTheme.colorScheme.primary
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Select Quality", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun QualityStep(
    filteredFormats: List<Format>,
    formatType: FormatType,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    onBack: () -> Unit,
    onDownload: (Format) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (formatType == FormatType.VIDEO) "Select Video Quality" else "Select Audio Quality",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Back")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredFormats.isEmpty()) {
            Text("No formats available for this selection.", color = MaterialTheme.colorScheme.error)
        } else {
            filteredFormats.take(5).forEachIndexed { index, format ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onIndexChange(index) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedIndex == index,
                        onClick = { onIndexChange(index) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6B553B))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        // Display clean resolution for Video, or "Audio" for Audio
                        val titleText = if (formatType == FormatType.VIDEO) {
                            "${format.resolution} • MP4"
                        } else {
                            "${format.resolution} • MP3"
                        }

                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val sizeText = when {
                            format.actualFileSize > 0 -> {
                                val prefix = if (format.isApproximate) "~" else ""
                                val mb = format.actualFileSize / (1024 * 1024)
                                val displayMb = if (mb == 0L) "1" else mb.toString()
                                " • $prefix$displayMb MB"
                            }
                            else -> ""
                        }
                        Text(
                            text = "${if (formatType == FormatType.AUDIO) format.acodec else format.vcodec}$sizeText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { if (filteredFormats.isNotEmpty()) onDownload(filteredFormats[selectedIndex]) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp),
            enabled = filteredFormats.isNotEmpty()
        ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Download Now", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// --- UI Helper Components & Formatters ---

@Composable
fun StatBadge(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer) // Draft Light Green
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

// Utility formatting functions
fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "--:--"
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

fun formatDate(rawDate: String): String {
    if (rawDate.isBlank()) return "Unknown Date"
    if (rawDate.length != 8) return rawDate
    return try {
        val input = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(rawDate)
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(input!!)
    } catch (e: Exception) { rawDate }
}

fun formatViews(views: Long): String {
    if (views <= 0) return "No views"
    return when {
        views >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM views", views / 1_000_000.0)
        views >= 1_000 -> String.format(Locale.getDefault(), "%.1fK views", views / 1_000.0)
        else -> "$views views"
    }
}



