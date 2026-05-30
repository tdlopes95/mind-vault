package com.mindvault.app.data.local.dao

import androidx.room.*
import com.mindvault.app.data.local.entity.NoteEntity
import com.mindvault.app.data.local.entity.NoteTagCrossRef
import com.mindvault.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("SELECT * FROM TagEntity ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("""
        SELECT TagEntity.* FROM TagEntity
        INNER JOIN NoteTagCrossRef ON TagEntity.id = NoteTagCrossRef.tagId
        WHERE NoteTagCrossRef.noteId = :noteId
    """)
    fun getTagsForNote(noteId: Long): Flow<List<TagEntity>>

    @Query("""
        SELECT notes.* FROM notes
        INNER JOIN NoteTagCrossRef ON notes.id = NoteTagCrossRef.noteId
        WHERE NoteTagCrossRef.tagId = :tagId AND notes.isDeleted = 0
    """)
    fun getNotesForTag(tagId: Long): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToNote(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM NoteTagCrossRef WHERE noteId = :noteId AND tagId = :tagId")
    suspend fun removeTagFromNote(noteId: Long, tagId: Long)

    @Query("DELETE FROM NoteTagCrossRef WHERE noteId = :noteId")
    suspend fun removeAllTagsFromNote(noteId: Long)

    @Query("SELECT * FROM TagEntity WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchTags(query: String): Flow<List<TagEntity>>

    @Query("""
        SELECT DISTINCT notes.* FROM notes
        INNER JOIN NoteTagCrossRef ON notes.id = NoteTagCrossRef.noteId
        WHERE NoteTagCrossRef.tagId = :tagId AND notes.isDeleted = 0 AND notes.isArchived = 0
        ORDER BY notes.updatedAt DESC
    """)
    fun getActiveNotesForTag(tagId: Long): Flow<List<NoteEntity>>
}
