package com.mindvault.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.mindvault.app.data.model.Tag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagChip(
    tag: Tag,
    onRemove: (() -> Unit)? = null,
) {
    AssistChip(
        onClick = {},
        label = { Text("#${tag.name}") },
        trailingIcon = if (onRemove != null) {
            {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove tag",
                )
            }
        } else null,
    )
    if (onRemove != null) {
        // Wrap in a Box to handle click on trailing icon area via AssistChip onClick
    }
}

@Composable
fun TagChipRemovable(
    tag: Tag,
    onRemove: () -> Unit,
) {
    AssistChip(
        onClick = onRemove,
        label = { Text("#${tag.name}") },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove tag ${tag.name}",
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagFilterChip(
    tag: Tag,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("#${tag.name}") },
    )
}
