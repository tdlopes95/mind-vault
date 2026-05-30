package com.mindvault.app.data.repository

import com.mindvault.app.data.local.dao.NoteDao
import com.mindvault.app.data.local.entity.NoteEntity
import com.mindvault.app.data.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(private val dao: NoteDao) : NoteRepositoryInterface {

    override fun getActiveNotes(): Flow<List<Note>> = dao.getActiveNotes().map { it.map(NoteEntity::toDomain) }

    override fun getArchivedNotes(): Flow<List<Note>> = dao.getArchivedNotes().map { it.map(NoteEntity::toDomain) }

    override fun getDeletedNotes(): Flow<List<Note>> = dao.getDeletedNotes().map { it.map(NoteEntity::toDomain) }

    override fun getFavoriteNotes(): Flow<List<Note>> = dao.getFavoriteNotes().map { it.map(NoteEntity::toDomain) }

    override fun getNoteById(id: Long): Flow<Note?> = dao.getNoteById(id).map { it?.toDomain() }

    override fun searchNotes(query: String): Flow<List<Note>> = dao.searchNotes(query).map { it.map(NoteEntity::toDomain) }

    override suspend fun insertNote(note: Note): Long = dao.insertNote(note.toEntity())

    override suspend fun updateNote(note: Note) = dao.updateNote(note.toEntity())

    override suspend fun softDeleteNote(id: Long) = dao.softDeleteNote(id)

    override suspend fun permanentlyDeleteNote(id: Long) = dao.permanentlyDeleteNote(id)

    override suspend fun restoreNote(id: Long) = dao.restoreNote(id)

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = dao.toggleFavorite(id, isFavorite)

    override suspend fun toggleArchive(id: Long, isArchived: Boolean) = dao.toggleArchive(id, isArchived)

    override suspend fun purgeOldDeletedNotes(cutoffTimestamp: Long) = dao.purgeOldDeletedNotes(cutoffTimestamp)
}

private fun NoteEntity.toDomain() = Note(
    id = id,
    title = title,
    content = content,
    color = color,
    isFavorite = isFavorite,
    isArchived = isArchived,
    isDeleted = isDeleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun Note.toEntity() = NoteEntity(
    id = id,
    title = title,
    content = content,
    color = color,
    isFavorite = isFavorite,
    isArchived = isArchived,
    isDeleted = isDeleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)
