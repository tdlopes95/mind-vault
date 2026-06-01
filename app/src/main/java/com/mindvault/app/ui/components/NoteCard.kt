package com.mindvault.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindvault.app.data.model.Note
import com.mindvault.app.util.DateUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
    linkCount: Int = 0,
) {
    val hasColor = note.color != 0
    val cardColor = if (hasColor) {
        Color(note.color).copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val borderColor = if (hasColor) {
        Color(note.color).copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }
    val haptic = LocalHapticFeedback.current
    val relativeTime = remember(note.updatedAt) { DateUtils.formatRelativeTime(note.updatedAt) }

    val starScale = remember { Animatable(1f) }
    LaunchedEffect(note.isFavorite) {
        if (note.isFavorite) {
            starScale.animateTo(1.4f, spring(dampingRatio = 0.3f))
            starScale.animateTo(1f, spring())
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    if (note.title.isNotBlank()) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    if (note.content.isNotBlank()) {
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
                if (note.isPinned) {
                    Icon(
                        Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(14.dp).padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                    if (linkCount > 0) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "$linkCount linked notes",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            text = "$linkCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFavoriteToggle()
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (note.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (note.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp).scale(starScale.value),
                    )
                }
            }
        }
    }
}
