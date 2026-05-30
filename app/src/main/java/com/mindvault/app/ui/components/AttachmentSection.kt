package com.mindvault.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mindvault.app.data.model.Attachment

@Composable
fun AttachmentSection(
    attachments: List<Attachment>,
    onDelete: (Attachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(attachments, key = { it.id }) { attachment ->
            AttachmentRow(
                attachment = attachment,
                onDelete = { onDelete(attachment) },
            )
        }
    }
}
