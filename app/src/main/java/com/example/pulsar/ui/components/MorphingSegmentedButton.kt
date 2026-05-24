package com.example.pulsar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset

/**
 * A segmented button component with morphing animations between selections.
 */
@Composable
fun MorphingSegmentedButton(
    options: List<String>,
    selectedOption: String,
    onOptionSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    labelTransformation: @Composable (String) -> Unit = { label ->
        Text(
            text = if (label == "Audio Only") "Audio" else label,
            style = MaterialTheme.typography.labelMedium
        )
    }
) {
    val selectedIndex = options.indexOf(selectedOption).coerceAtLeast(0)
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
    ) {
        val maxWidth = maxWidth
        val itemWidth = maxWidth / options.size
        
        // Animated Offset for the selection pill
        val animatedOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy),
            label = "pill_offset"
        )

        // Animated Shape for the selection pill (the "morph" part)
        // We use the same animation spec as the offset to keep them in sync
        val springSpec = spring<androidx.compose.ui.unit.Dp>(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        )

        val startRadius by animateDpAsState(
            targetValue = if (selectedIndex == 0) 12.dp else 4.dp,
            animationSpec = springSpec,
            label = "start_radius"
        )
        val endRadius by animateDpAsState(
            targetValue = if (selectedIndex == options.size - 1) 12.dp else 4.dp,
            animationSpec = springSpec,
            label = "end_radius"
        )

        // Selection Pill
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToPx(), 0) }
                .width(itemWidth)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(RoundedCornerShape(
                    topStart = startRadius, 
                    bottomStart = startRadius, 
                    topEnd = endRadius, 
                    bottomEnd = endRadius
                ))
                .background(MaterialTheme.colorScheme.secondaryContainer)
        )

        // Labels Row
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onOptionSelect(option) },
                    contentAlignment = Alignment.Center
                ) {
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) 
                            MaterialTheme.colorScheme.onSecondaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "content_color"
                    )

                    CompositionLocalProvider(
                        LocalContentColor provides contentColor
                    ) {
                        labelTransformation(option)
                    }
                }
            }
        }
    }
}
