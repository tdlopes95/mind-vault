package com.mindvault.app.data.repository

import com.mindvault.app.data.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepositoryInterface {
    fun getActiveNotes(): Flow<List<Note>>
    fun getArchivedNotes(): Flow<List<Note>>
    fun getDeletedNotes(): Flow<List<Note>>
    fun getFavoriteNotes(): Flow<List<Note>>
    fun getNoteById(id: Long): Flow<Note?>
    fun searchNotes(query: String): Flow<List<Note>>
    fun searchNotesFts(query: String): Flow<List<Note>>
    suspend fun insertNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun softDeleteNote(id: Long)
    suspend fun permanentlyDeleteNote(id: Long)
    suspend fun restoreNote(id: Long)
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
    suspend fun toggleArchive(id: Long, isArchived: Boolean)
    suspend fun purgeOldDeletedNotes(cutoffTimestamp: Long)
    suspend fun assignCategory(noteId: Long, categoryId: Long?)
}
