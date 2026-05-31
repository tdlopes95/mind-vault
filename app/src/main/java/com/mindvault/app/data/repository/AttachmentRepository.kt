package com.mindvault.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.mindvault.app.data.local.dao.AttachmentDao
import com.mindvault.app.data.local.entity.AttachmentEntity
import com.mindvault.app.data.model.Attachment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentRepository @Inject constructor(
    private val dao: AttachmentDao,
    @ApplicationContext private val context: Context,
) : AttachmentRepositoryInterface {

    override suspend fun addAttachment(noteId: Long, uri: Uri): Attachment {
        val originalFileName = resolveFileName(uri) ?: "attachment_${System.currentTimeMillis()}"
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val safeFileName = "${UUID.randomUUID()}_$originalFileName"
        val dir = File(context.filesDir, "attachments/$noteId").also { it.mkdirs() }
        val destFile = File(dir, safeFileName)

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            destFile.delete()
            throw IllegalStateException("Failed to save attachment: ${e.message}", e)
        }

        val relativePath = "attachments/$noteId/$safeFileName"
        val entity = AttachmentEntity(
            noteId = noteId,
            fileName = originalFileName,
            filePath = relativePath,
            mimeType = mimeType,
            fileSize = destFile.length(),
        )
        val id = dao.insertAttachment(entity)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun deleteAttachment(attachment: Attachment) {
        getAttachmentFile(attachment)?.delete()
        dao.deleteAttachment(attachment.toEntity())
    }

    override fun getAttachmentsForNote(noteId: Long): Flow<List<Attachment>> =
        dao.getAttachmentsForNote(noteId).map { it.map(AttachmentEntity::toDomain) }

    override fun getAttachmentCount(noteId: Long): Flow<Int> = dao.getAttachmentCount(noteId)

    override suspend fun deleteAllAttachmentsForNote(noteId: Long) {
        File(context.filesDir, "attachments/$noteId").deleteRecursively()
        dao.deleteAllAttachmentsForNote(noteId)
    }

    override fun getAttachmentFile(attachment: Attachment): File? {
        val file = File(context.filesDir, attachment.filePath)
        return if (file.exists()) file else null
    }

    private fun resolveFileName(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            if (nameIndex >= 0) cursor.getString(nameIndex) else null
        }
    }
}

private fun AttachmentEntity.toDomain() = Attachment(
    id = id,
    noteId = noteId,
    fileName = fileName,
    filePath = filePath,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt,
)

private fun Attachment.toEntity() = AttachmentEntity(
    id = id,
    noteId = noteId,
    fileName = fileName,
    filePath = filePath,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt,
)
