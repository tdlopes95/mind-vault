package com.mindvault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.mindvault.app.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Insert
    suspend fun insertAttachment(attachment: AttachmentEntity): Long

    @Delete
    suspend fun deleteAttachment(attachment: AttachmentEntity)

    @Query("SELECT * FROM AttachmentEntity WHERE noteId = :noteId ORDER BY createdAt ASC")
    fun getAttachmentsForNote(noteId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT COUNT(*) FROM AttachmentEntity WHERE noteId = :noteId")
    fun getAttachmentCount(noteId: Long): Flow<Int>

    @Query("DELETE FROM AttachmentEntity WHERE noteId = :noteId")
    suspend fun deleteAllAttachmentsForNote(noteId: Long)
}
