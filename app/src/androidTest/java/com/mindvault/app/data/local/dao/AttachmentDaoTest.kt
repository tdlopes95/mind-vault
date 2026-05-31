package com.mindvault.app.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mindvault.app.data.local.MindVaultDatabase
import com.mindvault.app.data.local.entity.AttachmentEntity
import com.mindvault.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttachmentDaoTest {

    private lateinit var db: MindVaultDatabase
    private lateinit var attachmentDao: AttachmentDao
    private lateinit var noteDao: NoteDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, MindVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        attachmentDao = db.attachmentDao()
        noteDao = db.noteDao()
    }

    @After
    fun teardown() { db.close() }

    private suspend fun insertNote(title: String = "Note"): Long =
        noteDao.insertNote(NoteEntity(title = title, content = ""))

    private fun attachment(noteId: Long, name: String = "file.png") = AttachmentEntity(
        noteId = noteId,
        fileName = name,
        filePath = "attachments/$noteId/$name",
        mimeType = "image/png",
        fileSize = 1024L,
        createdAt = System.currentTimeMillis(),
    )

    @Test
    fun insertAndGetAttachmentsForNote() = runTest {
        val noteId = insertNote()
        attachmentDao.insertAttachment(attachment(noteId, "photo.png"))
        attachmentDao.insertAttachment(attachment(noteId, "doc.pdf"))

        val attachments = attachmentDao.getAttachmentsForNote(noteId).first()
        assertEquals(2, attachments.size)
    }

    @Test
    fun getAttachmentsForNote_doesNotReturnOtherNoteAttachments() = runTest {
        val noteId1 = insertNote("Note 1")
        val noteId2 = insertNote("Note 2")
        attachmentDao.insertAttachment(attachment(noteId1))
        attachmentDao.insertAttachment(attachment(noteId2))

        val attachments = attachmentDao.getAttachmentsForNote(noteId1).first()
        assertEquals(1, attachments.size)
        assertEquals(noteId1, attachments[0].noteId)
    }

    @Test
    fun deleteAttachment_removesFromList() = runTest {
        val noteId = insertNote()
        val id = attachmentDao.insertAttachment(attachment(noteId))
        val entity = attachmentDao.getAttachmentsForNote(noteId).first().first()

        attachmentDao.deleteAttachment(entity)

        assertTrue(attachmentDao.getAttachmentsForNote(noteId).first().isEmpty())
    }

    @Test
    fun getAttachmentCount_returnsCorrectCount() = runTest {
        val noteId = insertNote()
        attachmentDao.insertAttachment(attachment(noteId, "a.png"))
        attachmentDao.insertAttachment(attachment(noteId, "b.png"))
        attachmentDao.insertAttachment(attachment(noteId, "c.png"))

        assertEquals(3, attachmentDao.getAttachmentCount(noteId).first())
    }

    @Test
    fun getAttachmentCount_noAttachments_returnsZero() = runTest {
        val noteId = insertNote()
        assertEquals(0, attachmentDao.getAttachmentCount(noteId).first())
    }

    @Test
    fun deleteAllAttachmentsForNote_clearsAll() = runTest {
        val noteId = insertNote()
        attachmentDao.insertAttachment(attachment(noteId, "a.png"))
        attachmentDao.insertAttachment(attachment(noteId, "b.png"))

        attachmentDao.deleteAllAttachmentsForNote(noteId)

        assertTrue(attachmentDao.getAttachmentsForNote(noteId).first().isEmpty())
    }

    @Test
    fun deleteAllAttachmentsForNote_doesNotAffectOtherNotes() = runTest {
        val noteId1 = insertNote("Note 1")
        val noteId2 = insertNote("Note 2")
        attachmentDao.insertAttachment(attachment(noteId1))
        attachmentDao.insertAttachment(attachment(noteId2))

        attachmentDao.deleteAllAttachmentsForNote(noteId1)

        assertEquals(0, attachmentDao.getAttachmentCount(noteId1).first())
        assertEquals(1, attachmentDao.getAttachmentCount(noteId2).first())
    }

    @Test
    fun cascadeDelete_attachmentsRemovedWhenNoteDeleted() = runTest {
        val noteId = insertNote()
        attachmentDao.insertAttachment(attachment(noteId))

        noteDao.permanentlyDeleteNote(noteId)

        assertTrue(attachmentDao.getAttachmentsForNote(noteId).first().isEmpty())
    }

    @Test
    fun attachmentsOrderedByCreatedAtAsc() = runTest {
        val noteId = insertNote()
        attachmentDao.insertAttachment(attachment(noteId, "first.png").copy(createdAt = 1000L))
        attachmentDao.insertAttachment(attachment(noteId, "second.png").copy(createdAt = 2000L))
        attachmentDao.insertAttachment(attachment(noteId, "third.png").copy(createdAt = 3000L))

        val attachments = attachmentDao.getAttachmentsForNote(noteId).first()
        assertEquals("first.png", attachments[0].fileName)
        assertEquals("third.png", attachments[2].fileName)
    }
}
