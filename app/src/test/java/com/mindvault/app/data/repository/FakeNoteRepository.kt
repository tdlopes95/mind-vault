package com.mindvault.app.data.repository

import com.mindvault.app.data.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeNoteRepository : NoteRepositoryInterface {

    private val notes = mutableListOf<Note>()
    private var nextId = 1L

    fun seed(note: Note): Note {
        val stored = note.copy(id = nextId++)
        notes.add(stored)
        return stored
    }

    fun allNotes(): List<Note> = notes.toList()

    override fun getActiveNotes(): Flow<List<Note>> = flow { emit(notes.filter { !it.isDeleted && !it.isArchived }) }
    override fun getArchivedNotes(): Flow<List<Note>> = flow { emit(notes.filter { it.isArchived && !it.isDeleted }) }
    override fun getDeletedNotes(): Flow<List<Note>> = flow { emit(notes.filter { it.isDeleted }) }
    override fun getFavoriteNotes(): Flow<List<Note>> = flow { emit(notes.filter { it.isFavorite && !it.isDeleted && !it.isArchived }) }
    override fun getPinnedNotes(): Flow<List<Note>> = flow { emit(notes.filter { it.isPinned && !it.isDeleted && !it.isArchived }) }
    override fun getNoteById(id: Long): Flow<Note?> = flow { emit(notes.find { it.id == id }) }
    override fun searchNotes(query: String): Flow<List<Note>> = flow {
        emit(notes.filter { !it.isDeleted && (it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)) })
    }
    override fun searchNotesFts(query: String): Flow<List<Note>> = searchNotes(query.trimEnd('*'))

    override suspend fun insertNote(note: Note): Long {
        val id = nextId++
        notes.add(note.copy(id = id))
        return id
    }

    override suspend fun updateNote(note: Note) {
        val idx = notes.indexOfFirst { it.id == note.id }
        if (idx >= 0) notes[idx] = note
    }

    override suspend fun softDeleteNote(id: Long) {
        val idx = notes.indexOfFirst { it.id == id }
        if (idx >= 0) notes[idx] = notes[idx].copy(isDeleted = true, deletedAt = System.currentTimeMillis())
    }

    override suspend fun permanentlyDeleteNote(id: Long) {
        notes.removeAll { it.id == id }
    }

    override suspend fun restoreNote(id: Long) {
        val idx = notes.indexOfFirst { it.id == id }
        if (idx >= 0) notes[idx] = notes[idx].copy(isDeleted = false, isArchived = false, deletedAt = null)
    }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        val idx = notes.indexOfFirst { it.id == id }
        if (idx >= 0) notes[idx] = notes[idx].copy(isFavorite = isFavorite)
    }

    override suspend fun toggleArchive(id: Long, isArchived: Boolean) {
        val idx = notes.indexOfFirst { it.id == id }
        if (idx >= 0) notes[idx] = notes[idx].copy(isArchived = isArchived)
    }

    override suspend fun togglePin(id: Long, isPinned: Boolean) {
        val idx = notes.indexOfFirst { it.id == id }
        if (idx >= 0) notes[idx] = notes[idx].copy(isPinned = isPinned)
    }

    override suspend fun assignCategory(noteId: Long, categoryId: Long?) {
        val idx = notes.indexOfFirst { it.id == noteId }
        if (idx >= 0) notes[idx] = notes[idx].copy(categoryId = categoryId)
    }

    override suspend fun purgeOldDeletedNotes(cutoffTimestamp: Long) {
        notes.removeAll { it.isDeleted && (it.deletedAt ?: 0L) < cutoffTimestamp }
    }
}
