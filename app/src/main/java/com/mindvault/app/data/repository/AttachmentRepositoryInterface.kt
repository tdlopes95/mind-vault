package com.mindvault.app.data.repository

import android.net.Uri
import com.mindvault.app.data.model.Attachment
import kotlinx.coroutines.flow.Flow
import java.io.File

interface AttachmentRepositoryInterface {
    suspend fun addAttachment(noteId: Long, uri: Uri): Attachment
    suspend fun deleteAttachment(attachment: Attachment)
    fun getAttachmentsForNote(noteId: Long): Flow<List<Attachment>>
    fun getAttachmentCount(noteId: Long): Flow<Int>
    suspend fun deleteAllAttachmentsForNote(noteId: Long)
    fun getAttachmentFile(attachment: Attachment): File?
}
