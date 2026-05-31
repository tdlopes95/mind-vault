package com.mindvault.app.data.repository

import com.mindvault.app.data.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeTagRepository : TagRepositoryInterface {

    private val tags = mutableListOf<Tag>()
    private val noteTagMap = mutableMapOf<Long, MutableSet<Long>>() // noteId → tagIds
    private var nextId = 1L

    fun seed(tag: Tag): Tag {
        val stored = tag.copy(id = nextId++)
        tags.add(stored)
        return stored
    }

    override fun getAllTags(): Flow<List<Tag>> = flow { emit(tags.toList()) }

    override fun getTagsForNote(noteId: Long): Flow<List<Tag>> = flow {
        val tagIds = noteTagMap[noteId] ?: emptySet()
        emit(tags.filter { it.id in tagIds })
    }

    override fun searchTags(query: String): Flow<List<Tag>> = flow {
        emit(tags.filter { it.name.contains(query, ignoreCase = true) })
    }

    override suspend fun insertTag(name: String, color: Int): Long {
        val existing = tags.find { it.name.equals(name, ignoreCase = true) }
        if (existing != null) return existing.id
        val id = nextId++
        tags.add(Tag(id = id, name = name, color = color))
        return id
    }

    override suspend fun deleteTag(tag: Tag) {
        tags.removeAll { it.id == tag.id }
        noteTagMap.values.forEach { it.remove(tag.id) }
    }

    override suspend fun addTagToNote(noteId: Long, tagId: Long) {
        noteTagMap.getOrPut(noteId) { mutableSetOf() }.add(tagId)
    }

    override suspend fun removeTagFromNote(noteId: Long, tagId: Long) {
        noteTagMap[noteId]?.remove(tagId)
    }

    override suspend fun removeAllTagsFromNote(noteId: Long) {
        noteTagMap.remove(noteId)
    }

    override suspend fun setTagsForNote(noteId: Long, tagIds: List<Long>) {
        noteTagMap[noteId] = tagIds.toMutableSet()
    }

    override fun getActiveNoteIdsForTag(tagId: Long): Flow<Set<Long>> = flow {
        emit(noteTagMap.entries.filter { tagId in it.value }.map { it.key }.toSet())
    }
}
