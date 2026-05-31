package com.mindvault.app.data.repository

import android.net.Uri
import com.mindvault.app.data.model.Attachment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class FakeAttachmentRepository : AttachmentRepositoryInterface {

    private val attachments = mutableListOf<Attachment>()
    private var nextId = 1L

    override suspend fun addAttachment(noteId: Long, uri: Uri): Attachment {
        val attachment = Attachment(
            id = nextId++,
            noteId = noteId,
            fileName = "file_$nextId.bin",
            filePath = "attachments/$noteId/file_$nextId.bin",
            mimeType = "application/octet-stream",
            fileSize = 0L,
            createdAt = System.currentTimeMillis(),
        )
        attachments.add(attachment)
        return attachment
    }

    override suspend fun deleteAttachment(attachment: Attachment) {
        attachments.removeAll { it.id == attachment.id }
    }

    override fun getAttachmentsForNote(noteId: Long): Flow<List<Attachment>> = flow {
        emit(attachments.filter { it.noteId == noteId })
    }

    override fun getAttachmentCount(noteId: Long): Flow<Int> = flow {
        emit(attachments.count { it.noteId == noteId })
    }

    override suspend fun deleteAllAttachmentsForNote(noteId: Long) {
        attachments.removeAll { it.noteId == noteId }
    }

    override fun getAttachmentFile(attachment: Attachment): File? = null
}
