package com.mindvault.app.data.model

data class NoteLink(
    val id: Long = 0,
    val sourceNoteId: Long,
    val targetNoteId: Long,
    val createdAt: Long = System.currentTimeMillis(),
)
