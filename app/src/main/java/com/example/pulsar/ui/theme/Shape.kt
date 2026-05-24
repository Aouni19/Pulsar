package com.example.pulsar.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val PulsarShapes = Shapes(
    small = RoundedCornerShape(12.dp),   // chips, small cards
    medium = RoundedCornerShape(20.dp),  // cards, bottom sheet content
    large = RoundedCornerShape(28.dp)    // bottom sheets, dialogs
)