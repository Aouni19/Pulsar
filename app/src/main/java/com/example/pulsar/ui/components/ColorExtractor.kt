package com.example.pulsar.ui.components

import android.graphics.Bitmap
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DynamicColors(
    val lightContainer: Color?,
    val lightProgress: Color?,
    val darkContainer: Color?,
    val darkProgress: Color?
)

suspend fun extractColorsFromBitmap(bitmap: Bitmap): DynamicColors = withContext(Dispatchers.Default) {
    // Palette cannot process HARDWARE bitmaps. We must copy it to a software configuration first.
    val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
        bitmap.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        bitmap
    }

    val palette = Palette.from(softwareBitmap).generate()

    // --- LIGHT MODE COLORS ---
    // Use extremely low alpha to keep cards nearly white but subtly tinted by the thumbnail
    val lightContainer = (palette.lightVibrantSwatch ?: palette.vibrantSwatch ?: palette.lightMutedSwatch)?.rgb?.let {
        Color(it).copy(alpha = 0.05f)
    } ?: palette.dominantSwatch?.rgb?.let { Color(it).copy(alpha = 0.03f) }

    val lightProgress = palette.vibrantSwatch?.rgb?.let { Color(it) }
        ?: palette.dominantSwatch?.rgb?.let { Color(it) }

    // --- DARK MODE COLORS ---
    val darkContainer = palette.darkMutedSwatch?.rgb?.let { Color(it) }
        ?: palette.dominantSwatch?.rgb?.let { Color(it).copy(alpha = 0.15f) }

    val darkProgress = palette.lightVibrantSwatch?.rgb?.let { Color(it) }
        ?: palette.vibrantSwatch?.rgb?.let { Color(it) }

    DynamicColors(lightContainer, lightProgress, darkContainer, darkProgress)
}
