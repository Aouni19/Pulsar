package com.example.pulsar.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.example.pulsar.R
import com.example.pulsar.ui.theme.bouncyClick

import com.example.pulsar.ui.components.MorphingSegmentedButton

/**
 * Displays the application settings screen, allowing users to configure download preferences,
 * appearance, and advanced options.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()

    val maxConcurrent by viewModel.maxConcurrent.collectAsState()
    val useAria2c by viewModel.useAria2c.collectAsState()
    val customFlags by viewModel.customFlags.collectAsState()
    val videoQuality by viewModel.videoQuality.collectAsState()
    val audioQuality by viewModel.audioQuality.collectAsState()
    val theme by viewModel.theme.collectAsState()

    var showQualityMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showCustomFlagsPanel by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SettingsTopBar(sharedTransitionScope, animatedVisibilityScope) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = FontFamily(Font(R.font.noto_serif)),
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Manage your archiving preferences and application behavior.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- DOWNLOADS SECTION ---
            SettingsSectionHeader("Downloads")
            SettingsCard {
                SettingsItem(
                    icon = Icons.Outlined.HighQuality,
                    title = "Default Quality",
                    subtitle = "Video: $videoQuality • Audio: $audioQuality",
                    onClick = { showQualityMenu = !showQualityMenu },
                    bottomContent = {
                        AnimatedVisibility(visible = showQualityMenu) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Video Quality",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                val videoOptions = listOf("4K", "1080p", "720p", "480p")
                                MorphingSegmentedButton(
                                    options = videoOptions,
                                    selectedOption = videoQuality,
                                    onOptionSelect = { viewModel.updateVideoQuality(it) }
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Audio Quality",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                val audioOptions = listOf("High (320k)", "Medium (128k)", "Low (64k)")
                                MorphingSegmentedButton(
                                    options = audioOptions,
                                    selectedOption = audioQuality,
                                    onOptionSelect = { viewModel.updateAudioQuality(it) }
                                )
                            }
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                SettingsItem(
                    icon = Icons.Outlined.SwapVert,
                    title = "Max Concurrent",
                    subtitle = "Simultaneous active transfers",
                    trailingContent = {
                        StepperControl(
                            value = maxConcurrent,
                            onValueChange = { viewModel.updateMaxConcurrent(it) }
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- APPEARANCE SECTION ---
            SettingsSectionHeader("Appearance")
            SettingsCard {
                SettingsItem(
                    icon = Icons.Outlined.Palette,
                    title = "Theme",
                    subtitle = when (theme) {
                        "Auto" -> "Follow system settings"
                        "Light" -> "Always light"
                        "Dark" -> "Always dark"
                        else -> "System default"
                    },
                    onClick = { showThemeMenu = !showThemeMenu },
                    bottomContent = {
                        AnimatedVisibility(visible = showThemeMenu) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                val options = listOf("Auto", "Light", "Dark")
                                MorphingSegmentedButton(
                                    options = options,
                                    selectedOption = theme,
                                    onOptionSelect = { label ->
                                        viewModel.updateTheme(label)
                                        // Removed showThemeMenu = false to keep it open
                                    },
                                    labelTransformation = { label ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val icon = when (label) {
                                                "Light" -> Icons.Default.LightMode
                                                "Dark" -> Icons.Default.DarkMode
                                                else -> Icons.Default.AutoMode
                                            }
                                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(label, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                )
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- ADVANCED SECTION ---
            SettingsSectionHeader("Advanced")
            SettingsCard {
                SettingsItem(
                    icon = Icons.Outlined.Terminal,
                    title = "Use aria2c Engine",
                    subtitle = "Enable multi-connection processing",
                    onClick = { viewModel.updateUseAria2c(!useAria2c) }, // Click entire row to toggle
                    trailingContent = {
                        Switch(
                            checked = useAria2c,
                            onCheckedChange = { viewModel.updateUseAria2c(it) }
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                SettingsItem(
                    icon = Icons.Outlined.Code,
                    title = "Custom Flags",
                    subtitle = "Append arguments to downloader",
                    onClick = { showCustomFlagsPanel = !showCustomFlagsPanel },
                    bottomContent = {
                        AnimatedVisibility(visible = showCustomFlagsPanel) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                val hasFlag: (String) -> Boolean = { flag ->
                                    " $customFlags ".contains(" $flag ")
                                }

                                val toggleFlag: (String) -> Unit = { flag ->
                                    if (hasFlag(flag)) {
                                        // Remove flag and clean up duplicate spaces
                                        val newFlags = " $customFlags ".replace(" $flag ", " ")
                                            .trim()
                                            .replace(Regex("\\s+"), " ")
                                        viewModel.updateCustomFlags(newFlags)
                                    } else {
                                        // Append flag
                                        val newFlags = if (customFlags.isBlank()) flag else "$customFlags $flag"
                                        viewModel.updateCustomFlags(newFlags.trim())
                                    }
                                }

                                Text("Format & Extraction", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val flags = listOf("--extract-audio", "--audio-format mp3", "--audio-quality 0", "--merge-output-format mkv")
                                    flags.forEach { flag ->
                                        FilterChip(
                                            selected = hasFlag(flag),
                                            onClick = { toggleFlag(flag) },
                                            label = { Text(flag) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Network & Bypass", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val flags = listOf("--force-ipv4", "--no-check-certificate", "--geo-bypass")
                                    flags.forEach { flag ->
                                        FilterChip(
                                            selected = hasFlag(flag),
                                            onClick = { toggleFlag(flag) },
                                            label = { Text(flag) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Metadata", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val flags = listOf("--write-subs", "--write-auto-subs", "--embed-thumbnail", "--add-metadata")
                                    flags.forEach { flag ->
                                        FilterChip(
                                            selected = hasFlag(flag),
                                            onClick = { toggleFlag(flag) },
                                            label = { Text(flag) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = customFlags,
                                    onValueChange = { viewModel.updateCustomFlags(it) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    placeholder = { Text("e.g. --limit-rate 500K") }
                                )
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- DANGER ZONE ---
            SettingsCard(borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)) {
                SettingsItem(
                    icon = Icons.Outlined.DeleteOutline,
                    iconTint = MaterialTheme.colorScheme.error,
                    title = "Clear Archive Cache",
                    titleColor = MaterialTheme.colorScheme.error,
                    subtitle = "Free up temporary .part files",
                    onClick = { viewModel.clearCache() } // Wired up directly to the row
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// --- REUSABLE COMPONENTS ---

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SettingsTopBar(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
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
                            .size(28.dp)
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = "pulsar_logo"),
                                animatedVisibilityScope = animatedVisibilityScope,
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pulsar",
                        style = MaterialTheme.typography.headlineMedium.copy(
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

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = FontFamily(Font(R.font.noto_serif)),
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
    )
}

@Composable
fun SettingsCard(
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

// UPDATED: Now accepts an onClick parameter for entire-row tapping
@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {},
    trailingContent: (@Composable () -> Unit)? = null,
    bottomContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClick { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = titleColor)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(16.dp))
                trailingContent()
            }
        }

        if (bottomContent != null) {
            bottomContent()
        }
    }
}

@Composable
fun TextWithChevron(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun StepperControl(value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { if (value > 1) onValueChange(value - 1) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(onClick = { if (value < 10) onValueChange(value + 1) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
        }
    }
}