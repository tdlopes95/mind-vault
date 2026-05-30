package com.mindvault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mindvault.app.data.local.entity.NoteEntity
import com.mindvault.app.data.local.entity.NoteLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteLinkDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLink(link: NoteLinkEntity): Long

    @Query("DELETE FROM NoteLinkEntity WHERE (sourceNoteId = :noteId1 AND targetNoteId = :noteId2) OR (sourceNoteId = :noteId2 AND targetNoteId = :noteId1)")
    suspend fun deleteLink(noteId1: Long, noteId2: Long)

    @Query("""
        SELECT notes.* FROM notes
        WHERE notes.id IN (
            SELECT targetNoteId FROM NoteLinkEntity WHERE sourceNoteId = :noteId
            UNION
            SELECT sourceNoteId FROM NoteLinkEntity WHERE targetNoteId = :noteId
        )
        AND notes.isDeleted = 0
    """)
    fun getLinkedNotes(noteId: Long): Flow<List<NoteEntity>>

    @Query("""
        SELECT notes.* FROM notes
        INNER JOIN NoteLinkEntity ON notes.id = NoteLinkEntity.sourceNoteId
        WHERE NoteLinkEntity.targetNoteId = :noteId
        AND notes.isDeleted = 0
    """)
    fun getBacklinks(noteId: Long): Flow<List<NoteEntity>>

    @Query("""
        SELECT COUNT(*) FROM (
            SELECT targetNoteId AS linkedId FROM NoteLinkEntity WHERE sourceNoteId = :noteId
            UNION
            SELECT sourceNoteId AS linkedId FROM NoteLinkEntity WHERE targetNoteId = :noteId
        )
    """)
    fun getLinkCount(noteId: Long): Flow<Int>
}
