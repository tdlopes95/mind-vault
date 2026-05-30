package com.mindvault.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mindvault.app.data.model.Note

@Composable
fun NoteLinkChip(
    note: Note,
    onNavigate: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onNavigate,
        label = {
            Text(
                text = note.title.ifBlank { "Untitled" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        trailingIcon = {
            IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove link", modifier = Modifier.size(14.dp))
            }
        },
        modifier = modifier,
    )
}
