package com.mindvault.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mindvault.app.util.NoteColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteOptionsBottomSheet(
    isFavorite: Boolean,
    isPinned: Boolean = false,
    selectedColor: Int,
    onFavoriteToggle: () -> Unit,
    onPinToggle: () -> Unit = {},
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Note color",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                NoteColors.presets.forEach { color ->
                    ColorCircle(
                        color = color,
                        isSelected = color == selectedColor,
                        onClick = { onColorSelected(color) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            OptionRow(
                icon = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                label = if (isPinned) "Unpin" else "Pin",
                onClick = { onPinToggle(); onDismiss() },
            )
            OptionRow(
                icon = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                label = if (isFavorite) "Remove from favorites" else "Add to favorites",
                onClick = { onFavoriteToggle(); onDismiss() },
            )
            OptionRow(
                icon = Icons.Outlined.Archive,
                label = "Archive",
                onClick = { onArchive(); onDismiss() },
            )
            OptionRow(
                icon = Icons.Outlined.Delete,
                label = "Delete",
                onClick = { onDelete(); onDismiss() },
            )
        }
    }
}

@Composable
private fun ColorCircle(
    color: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (color == 0) Color.Transparent else Color(color)
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
