package com.mindvault.app.data.repository

import com.mindvault.app.data.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepositoryInterface {
    fun getAllTags(): Flow<List<Tag>>
    fun getTagsForNote(noteId: Long): Flow<List<Tag>>
    fun searchTags(query: String): Flow<List<Tag>>
    suspend fun insertTag(name: String, color: Int = 0): Long
    suspend fun deleteTag(tag: Tag)
    suspend fun addTagToNote(noteId: Long, tagId: Long)
    suspend fun removeTagFromNote(noteId: Long, tagId: Long)
    suspend fun removeAllTagsFromNote(noteId: Long)
    suspend fun setTagsForNote(noteId: Long, tagIds: List<Long>)
    fun getActiveNoteIdsForTag(tagId: Long): Flow<Set<Long>>
}
