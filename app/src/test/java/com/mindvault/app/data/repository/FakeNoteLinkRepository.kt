package com.mindvault.app.data.repository

import com.mindvault.app.data.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeNoteLinkRepository : NoteLinkRepositoryInterface {

    private val links = mutableListOf<Pair<Long, Long>>() // sourceId → targetId

    override suspend fun linkNotes(noteId1: Long, noteId2: Long): Long {
        if (links.none { it == Pair(noteId1, noteId2) || it == Pair(noteId2, noteId1) }) {
            links.add(Pair(noteId1, noteId2))
        }
        return links.size.toLong()
    }

    override suspend fun unlinkNotes(noteId1: Long, noteId2: Long) {
        links.removeAll { (a, b) -> (a == noteId1 && b == noteId2) || (a == noteId2 && b == noteId1) }
    }

    override fun getLinkedNotes(noteId: Long): Flow<List<Note>> = flow { emit(emptyList()) }

    override fun getBacklinks(noteId: Long): Flow<List<Note>> = flow { emit(emptyList()) }

    override fun getLinkCount(noteId: Long): Flow<Int> = flow {
        emit(links.count { (a, b) -> a == noteId || b == noteId })
    }
}
