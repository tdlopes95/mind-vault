package com.mindvault.app.data.repository

import com.mindvault.app.data.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteLinkRepositoryInterface {
    suspend fun linkNotes(noteId1: Long, noteId2: Long): Long
    suspend fun unlinkNotes(noteId1: Long, noteId2: Long)
    fun getLinkedNotes(noteId: Long): Flow<List<Note>>
    fun getBacklinks(noteId: Long): Flow<List<Note>>
    fun getLinkCount(noteId: Long): Flow<Int>
}
