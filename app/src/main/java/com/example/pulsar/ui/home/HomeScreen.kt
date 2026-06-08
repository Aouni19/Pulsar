package com.example.pulsar.ui.home

import coil3.asDrawable
import androidx.core.graphics.drawable.toBitmap
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import com.example.pulsar.ui.theme.bouncyClick
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.pulsar.R
import java.io.File

/**
 * The main screen of the application, allowing users to input URLs for downloading
 * and displaying a list of recent downloads.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,

    viewModel: HomeViewModel = hiltViewModel()
) {
    var urlInput by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val recentDownloads by viewModel.recentDownloadsFlow.collectAsState(initial = emptyList())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PulsarTopBar(sharedTransitionScope, animatedVisibilityScope) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Standard Material 3 OutlinedTextField with theme-compliant colors
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("Paste video link here") },
                placeholder = {
                    Text(text = "https://youtu.be/...")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                trailingIcon = {
                    IconButton(onClick = { /* Paste logic could go here */ }) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                singleLine = true,
                isError = uiState is HomeUiState.Error
            )

            if (uiState is HomeUiState.Error) {
                Text(
                    text = (uiState as HomeUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, start = 16.dp).align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.fetchVideo(urlInput.trim()) },
                modifier = Modifier.height(60.dp).fillMaxWidth(.5f),
                enabled = uiState !is HomeUiState.Loading,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (uiState is HomeUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetch", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Downloads",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                if (recentDownloads.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .clickable { viewModel.clearRecentDownloads() }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (recentDownloads.isEmpty()) {
                    item(key = "empty_state") {
                        Text(
                            text = "No recent downloads yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp).animateItem()
                        )
                    }
                } else {
                    items(recentDownloads, key = { it.id }) { item ->
                        Box(modifier = Modifier.animateItem()) {
                            RecentDownloadCard(item, onClick = {
                                item.filePath?.let { path ->
                                    openFile(context, path)
                                }
                            })
                        }
                    }
                }
            }
        }
    }

    if (uiState is HomeUiState.Success) {
        val videoInfo = (uiState as HomeUiState.Success).videoInfo
        val preferredVideoQuality by viewModel.videoQuality.collectAsState()
        val preferredAudioQuality by viewModel.audioQuality.collectAsState()
        
        DetailsBottomSheet(
            videoInfo = videoInfo,
            preferredVideoQuality = preferredVideoQuality,
            preferredAudioQuality = preferredAudioQuality,
            onDismiss = { viewModel.resetState() },
            onDownload = { selectedFormat, isVideoType ->
                viewModel.startDownload(
                    url = urlInput,
                    title = videoInfo.title,
                    formatId = selectedFormat.formatId,
                    isAudio = !isVideoType,
                    isVideoOnly = selectedFormat.isVideoOnly,
                    quality = selectedFormat.resolution,
                    thumbnailUrl = videoInfo.thumbnail ?: ""
                )
            }
        )
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PulsarTopBar(
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
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.Center,
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
                            .size(35.dp)
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = "pulsar_logo"),
                                animatedVisibilityScope = animatedVisibilityScope,
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Pulsar",
                        style = MaterialTheme.typography.displayLarge.copy(
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
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun RecentDownloadCard(item: RecentDownloadItem, onClick: () -> Unit) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f
    val scope = rememberCoroutineScope()
    var dynamicColors by remember { mutableStateOf<com.example.pulsar.ui.components.DynamicColors?>(null) }
    
    val containerColor = if (isDark) {
        dynamicColors?.darkContainer ?: colorScheme.surface
    } else {
        dynamicColors?.lightContainer ?: colorScheme.surface
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .bouncyClick { onClick() },
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (item.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = item.thumbnailUrl,
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
                    }                )
            } else {
                Box(
                    modifier = Modifier
                        .size(120.dp, 72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val statusText = when (item.status) {
                    com.example.pulsar.data.model.DownloadStatus.COMPLETED -> "Completed"
                    com.example.pulsar.data.model.DownloadStatus.DOWNLOADING -> "Downloading"
                    com.example.pulsar.data.model.DownloadStatus.FAILED -> "Failed"
                    com.example.pulsar.data.model.DownloadStatus.CANCELLED -> "Cancelled"
                    com.example.pulsar.data.model.DownloadStatus.QUEUED -> "Queued"
                }

                val statusColor = when (item.status) {
                    com.example.pulsar.data.model.DownloadStatus.COMPLETED -> Color(0xFF4CAF50) // Prominent Green
                    com.example.pulsar.data.model.DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary // Keep Primary for Downloading
                    com.example.pulsar.data.model.DownloadStatus.FAILED -> MaterialTheme.colorScheme.error // Prominent Red
                    com.example.pulsar.data.model.DownloadStatus.CANCELLED -> Color(0xFFFF9800) // Prominent Orange
                    com.example.pulsar.data.model.DownloadStatus.QUEUED -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = statusColor,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.quality,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.size,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.runtime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}