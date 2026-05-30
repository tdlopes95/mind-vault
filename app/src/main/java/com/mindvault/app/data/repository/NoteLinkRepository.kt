package com.mindvault.app.data.repository

import com.mindvault.app.data.local.dao.NoteLinkDao
import com.mindvault.app.data.local.entity.NoteEntity
import com.mindvault.app.data.local.entity.NoteLinkEntity
import com.mindvault.app.data.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteLinkRepository @Inject constructor(private val dao: NoteLinkDao) : NoteLinkRepositoryInterface {

    override suspend fun linkNotes(noteId1: Long, noteId2: Long): Long =
        dao.insertLink(NoteLinkEntity(sourceNoteId = noteId1, targetNoteId = noteId2))

    override suspend fun unlinkNotes(noteId1: Long, noteId2: Long) = dao.deleteLink(noteId1, noteId2)

    override fun getLinkedNotes(noteId: Long): Flow<List<Note>> =
        dao.getLinkedNotes(noteId).map { it.map(NoteEntity::toDomain) }

    override fun getBacklinks(noteId: Long): Flow<List<Note>> =
        dao.getBacklinks(noteId).map { it.map(NoteEntity::toDomain) }

    override fun getLinkCount(noteId: Long): Flow<Int> = dao.getLinkCount(noteId)
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
    categoryId = categoryId,
    isPinned = isPinned,
)
