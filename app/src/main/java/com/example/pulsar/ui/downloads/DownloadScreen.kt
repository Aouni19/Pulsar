package com.example.pulsar.ui.downloads

import coil3.asDrawable
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import coil3.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.Delete
import kotlinx.coroutines.launch
import com.example.pulsar.ui.theme.bouncyCombinedClick
import com.example.pulsar.ui.theme.bouncyClick
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pulsar.R

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * Displays the active downloads, showing progress and allowing users to manage or cancel them.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DownloadsScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: DownloadsViewModel = hiltViewModel() // Inject the ViewModel
) {
    val downloads by viewModel.downloadsFlow.collectAsState(initial = emptyList())
    val context = LocalContext.current
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { DownloadsTopBar(sharedTransitionScope, animatedVisibilityScope) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            if (selectedIds.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Active Downloads",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = FontFamily(Font(R.font.noto_serif)),
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${downloads.size} items", // Dynamic count
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedIds = setOf() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = {
                            selectedIds.forEach { id ->
                                val item = downloads.find { it.id == id }
                                viewModel.deleteDownload(id, item?.filePath)
                            }
                            selectedIds = setOf()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Display downloaded items
                items(downloads) { item ->
                    val isSelected = selectedIds.contains(item.id)
                    ActiveDownloadCard(
                        title = item.title,
                        quality = item.quality,
                        progress = item.progress,
                        progressText = item.progressText,
                        statusText = item.statusText,
                        isPlaying = item.isPlaying,
                        thumbnailUrl = item.thumbnailUrl,
                        isQueued = item.isQueued,
                        isSelected = isSelected,
                        onCancel = { viewModel.cancelDownload(item.id) }, // Wire up the cancel button
                        onLongClick = {
                            if (selectedIds.isEmpty()) {
                                selectedIds = setOf(item.id)
                            }
                        },
                        onClick = {
                            if (selectedIds.isNotEmpty()) {
                                selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                            } else {
                                item.filePath?.let { path ->
                                    openFile(context, path)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun openFile(context: android.content.Context, filePath: String) {
    val file = File(filePath)
    if (!file.exists()) return

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val extension = MimeTypeMap.getFileExtensionFromUrl(filePath)
    val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, type)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error
    }
}

@Composable
fun ActiveDownloadCard(
    title: String,
    quality: String,
    progress: Float,
    progressText: String,
    statusText: String,
    isPlaying: Boolean,
    thumbnailUrl: String,
    isQueued: Boolean = false,
    isSelected: Boolean = false,
    onCancel: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f
    val scope = rememberCoroutineScope()
    var dynamicColors by remember { mutableStateOf<com.example.pulsar.ui.components.DynamicColors?>(null) }
    
    val baseContainerColor = if (isDark) {
        dynamicColors?.darkContainer ?: colorScheme.surface
    } else {
        dynamicColors?.lightContainer ?: colorScheme.surface
    }
    
    val containerColor = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.5f) else baseContainerColor
    
    val progressColor = if (isDark) {
        dynamicColors?.darkProgress ?: colorScheme.primary
    } else {
        dynamicColors?.lightProgress ?: colorScheme.primary
    }
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyCombinedClick(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(borderWidth, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Video Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp, 72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant),
                    onSuccess = { state ->
                        val drawable = state.result.image.asDrawable(context.resources)
                        val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: drawable.toBitmap()
                        
                        scope.launch {
                            dynamicColors = com.example.pulsar.ui.components.extractColorsFromBitmap(bitmap)
                        }
                    }
                )
            } else {
                // Thumbnail Mock
                Box(
                    modifier = Modifier
                        .size(120.dp, 72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Title and Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                        CircularOutlinedButton(
//                            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
//                            onClick = { /* Toggle Play/Pause */ }
//                        )
                        CircularOutlinedButton(
                            icon = Icons.Default.Close,
                            onClick = onCancel
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Quality Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = quality,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Text Row (Percentage and Speed/Status)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isQueued) {
                        // Queued Badge
                        Box(
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Queued",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Speed and Time left
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Custom Outlined IconButton to match your draft
@Composable
fun CircularOutlinedButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
            .bouncyClick { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// THE UPDATED TOP BAR (Matching SettingsScreen)
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DownloadsTopBar(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            with(sharedTransitionScope) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "pulsar_header_container"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.waves_icon),
                        contentDescription = "Pulsar Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(28.dp) // Smaller logo
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = "pulsar_logo"),
                                animatedVisibilityScope = animatedVisibilityScope,
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pulsar",
                        style = MaterialTheme.typography.headlineMedium.copy( // Using headlineMedium instead of displayLarge
                            fontFamily = FontFamily(Font(R.font.noto_serif)),
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "pulsar_text"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    )
                }
            }
        }
    }
}