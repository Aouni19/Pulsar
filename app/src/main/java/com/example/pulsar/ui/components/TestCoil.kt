package com.example.pulsar.ui.components

import androidx.compose.runtime.Composable
import coil3.compose.AsyncImagePainter
import coil3.asDrawable
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalContext

@Composable
fun Test(state: AsyncImagePainter.State.Success) {
    val context = LocalContext.current
    val image = state.result.image
    val drawable = image.asDrawable(context.resources)
    val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: drawable.toBitmap()
}
