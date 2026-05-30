package com.mindvault.app.data.local.dao

import androidx.room.*
import com.mindvault.app.data.local.entity.NoteEntity
import com.mindvault.app.data.model.CategoryNoteCount
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :now WHERE id = :id")
    suspend fun softDeleteNote(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun permanentlyDeleteNote(id: Long)

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreNote(id: Long)

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Long): Flow<NoteEntity?>

    @Query("UPDATE notes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE notes SET isArchived = :isArchived WHERE id = :id")
    suspend fun toggleArchive(id: Long, isArchived: Boolean)

    @Query("""
        SELECT notes.* FROM notes
        INNER JOIN notes_fts ON notes.id = notes_fts.rowid
        WHERE notes.isDeleted = 0
          AND notes_fts MATCH :query
        ORDER BY notes.updatedAt DESC
    """)
    fun searchNotesFts(query: String): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes
        WHERE isDeleted = 0
          AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedAt < :cutoffTimestamp")
    suspend fun purgeOldDeletedNotes(cutoffTimestamp: Long)

    @Query("UPDATE notes SET categoryId = :categoryId WHERE id = :noteId")
    suspend fun assignCategory(noteId: Long, categoryId: Long?)

    @Query("SELECT * FROM notes WHERE categoryId = :categoryId AND isDeleted = 0 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getNotesByCategory(categoryId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE categoryId IS NULL AND isDeleted = 0 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getUncategorizedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isPinned = 1 AND isDeleted = 0 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getPinnedNotes(): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :id")
    suspend fun togglePin(id: Long, isPinned: Boolean)

    @Query("SELECT categoryId, COUNT(*) as count FROM notes WHERE isDeleted = 0 AND isArchived = 0 AND categoryId IS NOT NULL GROUP BY categoryId")
    fun getNoteCounts(): Flow<List<CategoryNoteCount>>
}
