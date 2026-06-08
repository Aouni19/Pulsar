package com.example.pulsar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Connected Button Group based on Material 3 Expressive design.
 */
@Composable
fun ConnectedButtonGroup(
    options: List<String>,
    selectedOption: String,
    onOptionSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selectedOption
            
            val targetTopStart = if (options.size == 1 || isSelected || index == 0) 24.dp else 8.dp
            val targetBottomStart = if (options.size == 1 || isSelected || index == 0) 24.dp else 8.dp
            val targetTopEnd = if (options.size == 1 || isSelected || index == options.lastIndex) 24.dp else 8.dp
            val targetBottomEnd = if (options.size == 1 || isSelected || index == options.lastIndex) 24.dp else 8.dp

            val topStart by animateDpAsState(targetValue = targetTopStart, label = "topStart")
            val bottomStart by animateDpAsState(targetValue = targetBottomStart, label = "bottomStart")
            val topEnd by animateDpAsState(targetValue = targetTopEnd, label = "topEnd")
            val bottomEnd by animateDpAsState(targetValue = targetBottomEnd, label = "bottomEnd")

            val shape = RoundedCornerShape(
                topStart = topStart,
                bottomStart = bottomStart,
                topEnd = topEnd,
                bottomEnd = bottomEnd
            )

            val containerColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                label = "containerColor"
            )
            
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "contentColor"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(shape)
                    .background(containerColor)
                    .clickable { onOptionSelect(option) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}
