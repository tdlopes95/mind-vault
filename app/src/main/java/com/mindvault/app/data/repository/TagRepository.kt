package com.mindvault.app.data.repository

import com.mindvault.app.data.local.dao.TagDao
import com.mindvault.app.data.local.entity.NoteTagCrossRef
import com.mindvault.app.data.local.entity.TagEntity
import com.mindvault.app.data.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(private val dao: TagDao) : TagRepositoryInterface {

    override fun getAllTags(): Flow<List<Tag>> = dao.getAllTags().map { it.map(TagEntity::toDomain) }

    override fun getTagsForNote(noteId: Long): Flow<List<Tag>> =
        dao.getTagsForNote(noteId).map { it.map(TagEntity::toDomain) }

    override fun searchTags(query: String): Flow<List<Tag>> =
        dao.searchTags(query).map { it.map(TagEntity::toDomain) }

    override suspend fun insertTag(name: String, color: Int): Long =
        dao.insertTag(TagEntity(name = name, color = color))

    override suspend fun deleteTag(tag: Tag) = dao.deleteTag(tag.toEntity())

    override suspend fun addTagToNote(noteId: Long, tagId: Long) =
        dao.addTagToNote(NoteTagCrossRef(noteId = noteId, tagId = tagId))

    override suspend fun removeTagFromNote(noteId: Long, tagId: Long) =
        dao.removeTagFromNote(noteId, tagId)

    override suspend fun removeAllTagsFromNote(noteId: Long) =
        dao.removeAllTagsFromNote(noteId)

    override suspend fun setTagsForNote(noteId: Long, tagIds: List<Long>) {
        dao.removeAllTagsFromNote(noteId)
        tagIds.forEach { tagId -> dao.addTagToNote(NoteTagCrossRef(noteId = noteId, tagId = tagId)) }
    }

    override fun getActiveNoteIdsForTag(tagId: Long): Flow<Set<Long>> =
        dao.getActiveNotesForTag(tagId).map { notes -> notes.map { it.id }.toSet() }
}

private fun TagEntity.toDomain() = Tag(id = id, name = name, color = color, createdAt = createdAt)
private fun Tag.toEntity() = TagEntity(id = id, name = name, color = color, createdAt = createdAt)
